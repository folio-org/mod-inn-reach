package org.folio.innreach.batch.contribution.service;

import static java.lang.Math.max;
import static org.folio.innreach.batch.contribution.ContributionJobContextManager.*;
import static org.folio.innreach.batch.contribution.ContributionJobContextManager.getContributionJobContext;

import java.net.SocketTimeoutException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import com.google.common.collect.Iterables;

import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.innreach.domain.service.impl.FolioExecutionContextBuilder;
import org.folio.spring.scope.FolioExecutionContextSetter;
import org.folio.innreach.external.exception.InnReachConnectionException;
import org.folio.innreach.external.exception.ServiceSuspendedException;
import org.folio.innreach.batch.contribution.InitialContributionJobConsumerContainer;
import org.folio.innreach.external.exception.SocketTimeOutExceptionWrapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Service;

import org.folio.innreach.batch.contribution.ContributionJobContext;
import org.folio.innreach.batch.contribution.ContributionJobContext.Statistics;
import org.folio.innreach.batch.contribution.IterationEventReaderFactory;
import org.folio.innreach.batch.contribution.listener.ContributionExceptionListener;
import org.folio.innreach.batch.contribution.listener.ContributionJobStatsListener;
import org.folio.innreach.config.props.ContributionJobProperties;
import org.folio.innreach.domain.dto.folio.inventorystorage.InstanceIterationEvent;
import org.folio.innreach.domain.service.ContributionService;
import org.folio.innreach.domain.service.ContributionValidationService;
import org.folio.innreach.domain.service.InventoryViewService;
import org.folio.innreach.domain.service.RecordContributionService;
import org.folio.innreach.dto.Instance;
import org.folio.innreach.dto.Item;
import org.folio.spring.FolioExecutionContext;

@Service
@Log4j2
@RequiredArgsConstructor
public class ContributionJobRunner {

  private static final String DE_CONTRIBUTE_INSTANCE_MSG = "De-contributing ineligible instance";

  @Qualifier("instanceExceptionListener")
  private final ContributionExceptionListener instanceExceptionListener;
  @Qualifier("itemExceptionListener")
  private final ContributionExceptionListener itemExceptionListener;
  private final ContributionJobStatsListener statsListener;
  private final RecordContributionService recordContributionService;
  private final ContributionValidationService validationService;
  private final InventoryViewService inventoryViewService;
  private final ContributionJobProperties jobProperties;
  private final ContributionService contributionService;
  private final FolioExecutionContext folioContext;
  @Qualifier("contributionRetryTemplate")
  private final RetryTemplate retryTemplate;
  private final IterationEventReaderFactory itemReaderFactory;

  private final Statistics stats = new Statistics();

  private static final List<UUID> runningInitialContributions = Collections.synchronizedList(new ArrayList<>());
  private final FolioExecutionContextBuilder folioExecutionContextBuilder;

  private static Map<String,Integer> totalRecords = new HashMap<>();
  private static ConcurrentHashMap<String, Integer> recordsProcessed = new ConcurrentHashMap<>();

  public void startInitialContribution(UUID centralServerId, String tenantId, UUID contributionId, UUID iterationJobId, Integer numberOfRecords) {
    log.info("startInitialContribution:: parameters centralServerId: {}, tenantId: {}, contributionId: {}, iterationJobId: {}, numberOfRecords: {}", centralServerId, tenantId, contributionId, iterationJobId, numberOfRecords);
    var context = ContributionJobContext.builder()
      .contributionId(contributionId)
      .iterationJobId(iterationJobId)
      .centralServerId(centralServerId)
      .tenantId(tenantId)
      .isInitialContribution(true)
      .build();

    log.info("IterationJobId set at startInitialContribution: {}",context.getIterationJobId());

    //clear maps key & value of this tenant if present before start
    totalRecords.remove(tenantId);
    recordsProcessed.remove(tenantId);
    stats.clearStats();

    beginContributionJobContext(context);

    totalRecords.put(context.getTenantId(), numberOfRecords);

    log.info("Starting initial contribution: totalRecords: {}", totalRecords);

    InitialContributionJobConsumerContainer container = itemReaderFactory.createInitialContributionConsumerContainer(tenantId,this);

    InitialContributionMessageListener initialContributionMessageListener = new InitialContributionMessageListener(new ContributionProcessor(this));

    container.tryStartOrCreateConsumer(initialContributionMessageListener);
  }

  public void runInitialContribution(InstanceIterationEvent event, String topic) {
    log.info("runInitialContribution:: parameters InstanceIterationEvent: {}, topic: {}", event, topic);
    var context = getContributionJobContext(); // added
    log.info("Initial: count: {}, iterationJobId: {}, recordsTotal: {} ",
      recordsProcessed.get(context.getTenantId()), context.getIterationJobId(), stats.getRecordsTotal());

    stats.setTopic(topic);
    stats.setTenantId(context.getTenantId());

    var contributionId = context.getContributionId();
    var iterationJobId = context.getIterationJobId();

    if (event == null) {
      log.info("Initial: cannot contribute record for null event, contribution id: {}", contributionId);
      return;
    }
    log.info("Initial: processing instance iteration event instance id: {}", event.getInstanceId());

    var instanceId = event.getInstanceId();
    boolean isUnknownEvent = isUnknownEvent(event, iterationJobId);
    log.info("Initial: isUnknownEvent: {}", isUnknownEvent);
    if (isUnknownEvent) {
      log.info("Initial: skipping unknown event, current job is: {}, instance id: {}", iterationJobId, instanceId);
      return;
    }

    Instance instance = loadInstanceWithItems(instanceId);

    if (instance == null) {
      log.info("Initial: instance is null, skipping");
      return;
    }

    var centralServerId = context.getCentralServerId();

    if (isEligibleForContribution(centralServerId, instance)) {
      log.info("Initial: Eligible for Contribution centralServerId: {}, instanceId: {}", centralServerId, instanceId);
      contributeInstance(centralServerId, instance, stats);
      contributeInstanceItems(centralServerId, instance, stats);
    } else if (isContributed(centralServerId, instance)) {
      log.info("Initial: deContributeInstance centralServerId: {}, instanceId: {}", centralServerId, instanceId);
      deContributeInstance(centralServerId, instance, stats);
    }
    else {
      // to test if non-eligible increasing count to verify the stopping condition
      log.info("Initial: non-eligible instance id: {}", instanceId);
      ContributionJobRunner.recordsProcessed.put(context.getTenantId(), recordsProcessed.get(context.getTenantId()) == null ? 1
        : recordsProcessed.get(context.getTenantId())+1);

    }

    if (Objects.equals(recordsProcessed.get(context.getTenantId()), totalRecords.get(context.getTenantId()))) {
      log.info("Initial: consumer is stopping as all processed instance id: {}", instanceId);
      completeContribution(context);
      stopContribution(context.getTenantId());
      InitialContributionJobConsumerContainer.stopConsumer(topic);
    }
  }

  public void stopContribution(String tenantId) {
    log.info("stopContribution:: stopContribution called, parameters tenantId: {}", tenantId);
    endContributionJobContext();
    totalRecords.remove(tenantId);
    recordsProcessed.remove(tenantId);
  }

  public void cancelContributionIfRetryExhausted(UUID centralServerId) {
    log.info("cancelContributionIfRetryExhausted:: parameters centralServerId: {}", centralServerId);
    contributionService.cancelCurrent(centralServerId);
  }

  public void runInstanceContribution(UUID centralServerId, Instance instance) {
    log.info("Ongoing: validating instance {} for contribution to central server {}", instance.getId(), centralServerId);

    boolean eligibleInstance = isEligibleForContribution(centralServerId, instance);
    boolean contributedInstance = isContributed(centralServerId, instance);
    log.info("Ongoing: eligibleInstance: {}, contributedInstance: {}", eligibleInstance, contributedInstance);
    if (!eligibleInstance && !contributedInstance) {
      log.info("Ongoing: skipping ineligible and non-contributed instance");
      return;
    }

    runOngoing(centralServerId, (ctx, statistics) -> {
      log.info("Ongoing: starting ongoing instance contribution job {}, centralServerId: {}", ctx, centralServerId);

      if (eligibleInstance) {
        log.info("Ongoing: contributing instance id: {}", instance.getId());
        contributeInstance(centralServerId, instance, statistics);

        if (!contributedInstance) {
          log.info("Ongoing: contributing items of new instance id: {}", instance.getId());
          contributeInstanceItems(centralServerId, instance, statistics);
        }
      } else if (contributedInstance) {
        log.info("Ongoing : " + DE_CONTRIBUTE_INSTANCE_MSG+", instance id : {}", instance.getId());
        deContributeInstance(centralServerId, instance, statistics);
      }
    });
  }

  public void runInstanceDeContribution(UUID centralServerId, Instance deletedInstance) {
    log.info("Validating instance {} for de-contribution from central server {}", deletedInstance.getId(), centralServerId);

    if (!isContributed(centralServerId, deletedInstance)) {
      log.info("Skipping non-contributed instance ,centralServer id: {}, instance id: {}", centralServerId, deletedInstance.getId());
      return;
    }

    runOngoing(centralServerId, (ctx, statistics) -> {
      log.info("Starting ongoing instance de-contribution job {} centralServer id: {},  instance id: {}", ctx, centralServerId, deletedInstance.getId());
      deContributeInstance(centralServerId, deletedInstance, statistics);
    });
  }

  public void runItemContribution(UUID centralServerId, Instance instance, Item item) {
    log.info("Ongoing: validating item {} for contribution to central server {} with instance id: {}", item.getId(), centralServerId, instance.getId());

    boolean eligibleItem = isEligibleForContribution(centralServerId, item);
    boolean contributedItem = isContributed(centralServerId, instance, item);
    log.info("Ongoing: eligibleItem: {}, contributedItem: {}", eligibleItem, contributedItem);
    if (!eligibleItem && !contributedItem) {
      log.info("Ongoing: skipping ineligible and non-contributed centralServer id: {}, item id: {}, instance id : {}", centralServerId, item.getId(), instance.getId());
      return;
    }

    runOngoing(centralServerId, (ctx, statistics) -> {
      log.info("Starting ongoing centralServer id: {}, item id: {} contribution job {}, instance id : {}", centralServerId, item.getId(), ctx, instance.getId());

      if (isEligibleForContribution(centralServerId, instance)) {
        log.info("Ongoing: Re-contributing instance to update bib status, centralServer id: {}, instance id : {}, item id: {}", centralServerId, instance.getId(), item.getId());
        contributeInstance(centralServerId, instance, statistics);

        if (eligibleItem) {
          log.info("Ongoing: contributing centralServer id:{}, instance id : {}, item id: {}", centralServerId, instance.getId(), item.getId());
          contributeItem(centralServerId, instance.getHrid(), item, statistics);
        } else if (contributedItem) {
          log.info("Ongoing: de-contributing centralServer id: {}, instance id : {}, item id: {}", centralServerId, instance.getId(), item.getId());
          deContributeItem(centralServerId, item, statistics);
        }
      } else if (contributedItem) {
        log.info(" Ongoing : " + DE_CONTRIBUTE_INSTANCE_MSG+", centralServer id: {}, instance id : {}, item id: {}", centralServerId, instance.getId(), item.getId());
        deContributeInstance(centralServerId, instance, statistics);
      }
    });
  }

  public void runItemMove(UUID centralServerId, Instance newInstance, Instance oldInstance, Item item) {
    log.info("Validating item {} for moving to a new instance id : {} from old instance id : {} on central server {}", item.getId(), newInstance.getId(), oldInstance.getId(), centralServerId);

    boolean eligibleItem = isEligibleForContribution(centralServerId, item);
    boolean contributedItem = isContributed(centralServerId, oldInstance, item);
    log.info("eligibleItem: {}, contributedItem: {}", eligibleItem, contributedItem);
    if (!eligibleItem && !contributedItem) {
      log.info("Skipping ineligible and non-contributed item id: {}, new instance id: {}, old instance id: {}", item.getId(), newInstance.getId(), oldInstance.getId());
      return;
    }

    runOngoing(centralServerId, (ctx, statistics) -> {
      log.info("Starting ongoing item move job {}, item id: {}, new instance id: {}, old instance id: {}", ctx, item.getId(), newInstance.getId(), oldInstance.getId());

      // de-contribute item and update old instance
      if (contributedItem) {
        if (isEligibleForContribution(centralServerId, oldInstance)) {
          log.info("Ongoing: de-contributing item : {} from old instance id : {}", item.getId(), oldInstance.getId());
          deContributeItem(centralServerId, item, statistics);

          log.info("Ongoing: re-contributing old instance id:{} to update bib status, item id; {}", oldInstance.getId(), item.getId());
          contributeInstance(centralServerId, oldInstance, statistics);
        } else {
          log.info("Ongoing: e-contributing old instance id: {}, item id: {}", oldInstance.getId(), item.getId());
          deContributeInstance(centralServerId, oldInstance, statistics);
        }
      }

      // contribute item to a new instance
      if (isEligibleForContribution(centralServerId, newInstance)) {
        log.info("Ongoing: re-contributing new instance id: {} to update bib status, item id: {}", newInstance.getId(), item.getId());
        contributeInstance(centralServerId, newInstance, statistics);

        if (eligibleItem) {
          log.info("Ongoing: Contributing item to new instance id: {}, item id: {}", newInstance.getId(), item.getId());
          contributeItem(centralServerId, newInstance.getHrid(), item, statistics);
        }
      }
    });
  }

  public void runItemDeContribution(UUID centralServerId, Instance instance, Item deletedItem) {
    log.info("Validating item id: {} for de-contribution from central server: {} with instance id: {}", deletedItem.getId(), centralServerId, instance.getId());
    if (!isContributed(centralServerId, instance, deletedItem)) {
      log.info("Skipping non-contributed item id: {}, instance id: {}", deletedItem.getId(), instance.getId());
      return;
    }

    runOngoing(centralServerId, (context, statistics) -> {
      log.info("Starting ongoing item de-contribution job {}, centralServer id: {}, item id: {}, instance id: {}", context, centralServerId, deletedItem.getId(), instance.getId());

      if (isEligibleForContribution(centralServerId, instance)) {
        log.info("Ongoing: de-contributing centralServer id: {}, item id: {}, instance id: {}", centralServerId, deletedItem.getId(), instance.getId());
        deContributeItem(centralServerId, deletedItem, statistics);

        log.info("Ongoing: re-contributing instance to update bib status centralServer id: {}, item id: {}, instance id: {}", centralServerId, deletedItem.getId(), instance.getId());
        contributeInstance(centralServerId, instance, statistics);
      } else {
        log.info("Ongoing: " + DE_CONTRIBUTE_INSTANCE_MSG+", centralServer id: {}, item id: {}, instance id : {}", centralServerId, deletedItem.getId(), instance.getId());
        deContributeInstance(centralServerId, instance, statistics);
      }
    });
  }

  public void cancelJobs() {
    log.debug("cancelJobs:: Cancelling unfinished contributions");
    try (var context = new FolioExecutionContextSetter(folioExecutionContextBuilder.withUserId(folioContext,null))) {
      contributionService.cancelAll();
      runningInitialContributions.clear();
    }
  }

  public void cancelInitialContribution(UUID contributionId) {
    log.info("Cancelling initial contribution job {}", contributionId);
    runningInitialContributions.remove(contributionId);
  }

  private boolean isEligibleForContribution(UUID centralServerId, Instance instance) {
    return validationService.isEligibleForContribution(centralServerId, instance);
  }

  private boolean isContributed(UUID centralServerId, Instance instance) {
    log.info("isContributed:: parameters centralServerId: {}, instance id: {}", centralServerId, instance.getId());
    return recordContributionService.isContributed(centralServerId, instance);
  }

  private boolean isEligibleForContribution(UUID centralServerId, Item item) {
    log.info("isEligibleForContribution:: parameters centralServerId: {}, item id: {}", centralServerId, item.getId());
    return validationService.isEligibleForContribution(centralServerId, item);
  }

  private boolean isContributed(UUID centralServerId, Instance instance, Item item) {
    log.info("isContributed:: parameters centralServerId: {}, instance id: {}, item id: {}", centralServerId, instance.getId(), item.getId());
    return recordContributionService.isContributed(centralServerId, instance, item);
  }

  private void contributeInstanceItems(UUID centralServerId, Instance instance, Statistics stats) {
    log.info("contributeInstanceItems:: parameters centralServerId: {}, instance id: {}", centralServerId, instance.getId());
    var bibId = instance.getHrid();
    var items = instance.getItems().stream()
      .filter(i -> isEligibleForContribution(centralServerId, i))
      .collect(Collectors.toList());

    if (items.isEmpty()) {
      log.info("item is empty while contributing instance id: {}", instance.getId());
      addRecordProcessed();
    }

    int chunkSize = max(jobProperties.getChunkSize(), 1);

    StreamSupport.stream(Iterables.partition(items, chunkSize).spliterator(), false)
      .forEach(itemsChunk -> contributeItemsChunk(centralServerId, bibId, itemsChunk, stats));
  }

  private void contributeItem(UUID centralServerId, String bibId, Item item, Statistics stats) {
    log.info("contributeItem:: parameters centralServerId: {}, bibId: {}, item id: {}", centralServerId, bibId, item.getId());
    contributeItemsChunk(centralServerId, bibId, List.of(item), stats);
  }

  private void contributeItemsChunk(UUID centralServerId, String bibId, List<Item> items, Statistics stats) {
    log.info("contributeItemsChunk:: paramerters centralServerId: {}, bibId: {}, items count: {}", centralServerId, bibId, items.size());
    var itemsCount = items.size();
    try {
      stats.addRecordsTotal(itemsCount);
      recordContributionService.contributeItems(centralServerId, bibId, items);
      stats.addRecordsContributed(itemsCount);
      addRecordProcessed();
    }
    catch (ServiceSuspendedException | FeignException | InnReachConnectionException e) {
      log.info(getContributionJobContext().isInitialContribution() ? "Initial: exception occurred: {}": "Ongoing: exception occurred: {}", e);
      throw e;
    }
    catch (SocketTimeoutException socketTimeoutException) {
      log.info(getContributionJobContext().isInitialContribution() ? "Initial: socket exception occurred: {}": "Ongoing: socket exception occurred: {}", socketTimeoutException);
      throw new SocketTimeOutExceptionWrapper(socketTimeoutException.getMessage());
    }
    catch (Exception e) {
      // not possible to guess what item failed when the chunk of multiple items is being contributed
      var recordId = items.size() == 1 ? items.get(0).getId() : null;
      itemExceptionListener.logWriteError(e, recordId);
      log.info("contributeItemsChunk:: recordId: {}, exception occurred: {}", recordId, e);
      addRecordProcessed();
    } finally {
      stats.addRecordsProcessed(itemsCount);
      statsListener.updateStats(stats);
    }
  }

  private void addRecordProcessed() {
    if (getContributionJobContext().isInitialContribution()) {
      ContributionJobRunner.recordsProcessed.put(getContributionJobContext().getTenantId(), recordsProcessed.get(getContributionJobContext().getTenantId()) == null ? 1
        : recordsProcessed.get(getContributionJobContext().getTenantId()) + 1);
    }
  }

  private void contributeInstance(UUID centralServerId, Instance instance, Statistics stats) {
    log.info("Initial: contributeInstance centralServerId: {}, instanceId: {}", centralServerId, instance.getId());
    try {
      stats.addRecordsTotal(1);
      recordContributionService.contributeInstance(centralServerId, instance);
      stats.addRecordsContributed(1);
    }
    catch (ServiceSuspendedException | FeignException | InnReachConnectionException e) {
      log.info(getContributionJobContext().isInitialContribution() ? "Initial: instance id:{}, exception occurred: {}": "Ongoing: instance id:{}, exception occurred: {}", instance.getId(), e);
      throw e;
    }
    catch (SocketTimeoutException socketTimeoutException) {
      log.info(getContributionJobContext().isInitialContribution() ? "Initial: instance id:{}, socket exception occurred: {}": "Ongoing: instance id:{}, socket exception occurred: {}", instance.getId(), socketTimeoutException);
      throw new SocketTimeOutExceptionWrapper(socketTimeoutException.getMessage());
    }
    catch (Exception e) {
      log.info("Initial: instance id: {}, exception caught in contributeInstance e: {}", instance.getId(), e);
      instanceExceptionListener.logWriteError(e, instance.getId());
    } finally {
      log.info("Initial: contributeInstance finally called, instance id: {}", instance.getId());
      stats.addRecordsProcessed(1);
      updateStats(stats);
    }
  }

  private void deContributeInstance(UUID centralServerId, Instance instance, Statistics stats) {
    log.info("Initial: deContributeInstance centralServerId: {}, instanceId: {}", centralServerId, instance.getId());
    try {
      stats.addRecordsTotal(1);
      recordContributionService.deContributeInstance(centralServerId, instance);
      stats.addRecordsDeContributed(1);
      addRecordProcessed();
    }
    catch (ServiceSuspendedException | FeignException | InnReachConnectionException e) {
      log.info("Initial: instance id: {}, deContributeInstance exception occurred e: {}",instance.getId(), e);
      throw e;
    }
    catch (SocketTimeoutException socketTimeoutException) {
      log.info("instance id: {}, socketTimeoutException occur: {}",instance.getId(), socketTimeoutException);
      throw new SocketTimeOutExceptionWrapper(socketTimeoutException.getMessage());
    }
    catch (Exception e) {
      log.info("Initial: deContributeInstance exception occurred e: {} for instanceId: {}", e, instance.getId());
      instanceExceptionListener.logWriteError(e, instance.getId());
    } finally {
      log.info("Initial: deContributeInstance finally added processed records and updated stats, instance id: {}", instance.getId());
      stats.addRecordsProcessed(1);
      updateStats(stats);
    }
  }

  private void deContributeItem(UUID centralServerId, Item item, Statistics stats) {
    log.info("Initial: deContributeItem centralServerId: {}, item id: {}", centralServerId, item.getId());
    try {
      stats.addRecordsTotal(1);
      recordContributionService.deContributeItem(centralServerId, item);
      stats.addRecordsDeContributed(1);
      addRecordProcessed();
    }
    catch (ServiceSuspendedException | FeignException | InnReachConnectionException e) {
      log.info("Initial: item id: {}, deContributeItem exception occurred e: {}", item.getId(), e);
      throw e;
    }
    catch (Exception e) {
      log.info("Initial: deContributeItem exception occurred e: {} for item id: {}", e, item.getId());
      itemExceptionListener.logWriteError(e, item.getId());
    } finally {
      log.info("Initial: deContributeItem finally added processed records and updated stats, item id: {}", item.getId());
      stats.addRecordsProcessed(1);
      updateStats(stats);
    }
  }

  private void runOngoing(UUID centralServerId, BiConsumer<ContributionJobContext, Statistics> processor) {
    log.info("Initial: runOngoing  centralServerId: {}", centralServerId);
    var contribution = contributionService.createOngoingContribution(centralServerId);
    var context = ContributionJobContext.builder()
      .contributionId(contribution.getId())
      .centralServerId(centralServerId)
      .tenantId(folioContext.getTenantId())
      .build();

    var statistics = new Statistics();
    try {
      if(getContributionJobContext()==null) {
        log.info("setting ongoing contribution context for contributionID:{}", context.getContributionId());
        beginContributionJobContext(context);
      }
      processor.accept(context, statistics);
      completeContribution(context);
      endContributionJobContext();
    }
    catch (ServiceSuspendedException | FeignException | InnReachConnectionException | SocketTimeOutExceptionWrapper e) {
      log.info("exception thrown from runOngoing : {}", e);
      throw e;
    }
    catch (Exception e) {
      log.info("contributeInstance exception block : {}", e);
      throw e;
    }
  }

  private boolean isUnknownEvent(InstanceIterationEvent event, UUID iterationJobId) {
    return !Objects.equals(event.getJobId(), iterationJobId);
  }

  public void completeContribution(ContributionJobContext context) {
    log.info("completeContribution:: parameters context contribution id: {}", context.getContributionId());
    try {
      contributionService.completeContribution(context.getContributionId());
      log.info("Completed contribution");
    } catch (Exception e) {
      log.info("Failed to complete contribution job: {}", context, e);
    }
  }

  private void updateStats(Statistics stats) {
    statsListener.updateStats(stats);
  }

  private Instance loadInstanceWithItems(UUID instanceId) {
    log.info("loadInstanceWithItems:: parameters instanceId: {}", instanceId);
    Instance instance = null;
    try {
      instance = retryTemplate.execute(r -> inventoryViewService.getInstance(instanceId));
    } catch (Exception e) {
      log.info("loadInstanceWithItems:: exception occurred with instance id: {} and e: {}", instanceId, e);
      instanceExceptionListener.logProcessError(e, instanceId);
    }
    log.info("loadInstanceWithItems:: loaded instance with items, instance id: {}", instanceId);
    return instance;
  }
}
