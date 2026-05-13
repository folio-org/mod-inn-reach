package org.folio.innreach.batch.contribution.service;

import static java.lang.Math.max;
import static org.folio.innreach.batch.contribution.ContributionJobContextManager.beginContributionJobContext;
import static org.folio.innreach.batch.contribution.ContributionJobContextManager.endContributionJobContext;
import static org.folio.innreach.batch.contribution.ContributionJobContextManager.getContributionJobContext;
import static org.folio.innreach.domain.entity.ContributionStatus.DE_CONTRIBUTED;
import static org.folio.innreach.domain.entity.ContributionStatus.FAILED;
import static org.folio.innreach.domain.entity.ContributionStatus.PROCESSED;
import static org.folio.innreach.util.InnReachConstants.DE_CONTRIBUTE_INSTANCE_MSG;
import static org.folio.innreach.util.InnReachConstants.SKIPPING_INELIGIBLE_INSTANCE_ITEM_MSG;
import static org.folio.innreach.util.InnReachConstants.SKIPPING_INELIGIBLE_INSTANCE_MSG;
import static org.folio.innreach.util.InnReachConstants.SKIPPING_INELIGIBLE_MSG;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.StreamSupport;

import com.google.common.collect.Iterables;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.innreach.domain.entity.OngoingContributionStatus;
import org.folio.innreach.external.exception.InnReachConnectionException;
import org.folio.innreach.external.exception.InnReachContributionRequestException;
import org.folio.innreach.external.exception.ServiceSuspendedException;
import org.folio.innreach.batch.contribution.InitialContributionJobConsumerContainer;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.retry.RetryTemplate;
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
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

@Service
@Log4j2
@RequiredArgsConstructor
public class ContributionJobRunner {

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
  @Qualifier("contributionRetryTemplate")
  private final RetryTemplate retryTemplate;
  private final IterationEventReaderFactory itemReaderFactory;

  private final Statistics stats = new Statistics();

  private static final List<UUID> runningInitialContributions = Collections.synchronizedList(new ArrayList<>());
  private static Map<String,Integer> totalRecords = new HashMap<>();
  private static ConcurrentHashMap<String, Integer> recordsProcessed = new ConcurrentHashMap<>();
  private final OngoingContributionStatusService ongoingContributionStatusService;


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
    if (isUnknownEvent(event, iterationJobId)) {
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
    } else {
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

  public void runOngoingInstanceContribution(UUID centralServerId, Instance instance, OngoingContributionStatus ongoingContributionStatus) {
    boolean eligibleInstance = isEligibleForContribution(centralServerId, instance);
    boolean contributedInstance = isContributed(centralServerId, instance);
    log.info("runOngoingInstanceContribution:: eligibleInstance: {}, contributedInstance: {}", eligibleInstance, contributedInstance);
    if (!eligibleInstance && !contributedInstance) {
      log.info("runOngoingInstanceContribution:: {}, instance id : {}", SKIPPING_INELIGIBLE_INSTANCE_MSG, instance.getId());
      ongoingContributionStatusService.updateOngoingContribution(ongoingContributionStatus, SKIPPING_INELIGIBLE_INSTANCE_MSG, FAILED);
      return;
    }
    if (eligibleInstance) {
      log.info("runOngoingInstanceContribution:: contributing instance id: {}", instance.getId());
      recordContributionService.contributeInstance(centralServerId, instance);
      if (!contributedInstance) {
        log.info("runOngoingInstanceContribution:: contributing items of new instance id: {}", instance.getId());
        contributeOngoingItems(centralServerId, instance);
      }
      ongoingContributionStatusService.updateOngoingContribution(ongoingContributionStatus, PROCESSED);
    } else if (contributedInstance) {
      log.info("runOngoingInstanceContribution:: {}, instance id : {}", DE_CONTRIBUTE_INSTANCE_MSG, instance.getId());
      recordContributionService.deContributeInstance(centralServerId, instance);
      ongoingContributionStatusService.updateOngoingContribution(ongoingContributionStatus, DE_CONTRIBUTED);
    }
  }

  public void runOngoingInstanceDeContribution(UUID centralServerId, Instance deletedInstance, OngoingContributionStatus ongoingContributionStatus) {
    if (!isContributed(centralServerId, deletedInstance)) {
      log.info("runOngoingInstanceDeContribution:: Skipping non-contributed instance, centralServer id: {}, instance id: {}", centralServerId, deletedInstance.getId());
      ongoingContributionStatusService.updateOngoingContribution(ongoingContributionStatus, SKIPPING_INELIGIBLE_MSG, FAILED);
      return;
    }
    recordContributionService.deContributeInstance(centralServerId, deletedInstance);
    ongoingContributionStatusService.updateOngoingContribution(ongoingContributionStatus, DE_CONTRIBUTED);
  }

  public void runItemContribution(UUID centralServerId, Instance instance, Item item, OngoingContributionStatus ongoingContributionStatus) {
    boolean eligibleItem = isEligibleForContribution(centralServerId, item);
    boolean contributedItem = isContributed(centralServerId, instance, item);
    log.info("runItemContribution:: eligibleItem: {}, contributedItem: {}", eligibleItem, contributedItem);
    if (!eligibleItem && !contributedItem) {
      log.info("runItemContribution:: skipping ineligible and non-contributed centralServer id: {}, item id: {}, instance id : {}", centralServerId, item.getId(), instance.getId());
      ongoingContributionStatusService.updateOngoingContribution(ongoingContributionStatus, SKIPPING_INELIGIBLE_MSG, FAILED);
      return;
    }
    if (isEligibleForContribution(centralServerId, instance)) {
      log.info("runItemContribution:: Re-contributing instance to update bib status, centralServer id: {}, instance id : {}, item id: {}", centralServerId, instance.getId(), item.getId());
      recordContributionService.contributeInstance(centralServerId, instance);
      if (eligibleItem) {
        log.info("runItemContribution:: contributing centralServer id: {}, instance id : {}, item id: {}", centralServerId, instance.getId(), item.getId());
        recordContributionService.contributeItems(centralServerId, instance.getHrid(), List.of(item));
      } else if (contributedItem) {
        log.info("runItemContribution:: de-contributing centralServer id: {}, instance id : {}, item id: {}", centralServerId, instance.getId(), item.getId());
        recordContributionService.deContributeItem(centralServerId, item);
      }
      ongoingContributionStatusService.updateOngoingContribution(ongoingContributionStatus, PROCESSED);
    } else if (contributedItem) {
      log.info("runItemContribution:: {}, centralServer id: {}, instance id : {}, item id: {}", DE_CONTRIBUTE_INSTANCE_MSG, centralServerId, instance.getId(), item.getId());
      recordContributionService.deContributeInstance(centralServerId, instance);
      ongoingContributionStatusService.updateOngoingContribution(ongoingContributionStatus, DE_CONTRIBUTED);
    } else {
      log.info("runItemContribution:: {}, centralServer id: {}, instance id : {}, item id: {}", SKIPPING_INELIGIBLE_INSTANCE_ITEM_MSG, centralServerId, instance.getId(), item.getId());
      ongoingContributionStatusService.updateOngoingContribution(ongoingContributionStatus, SKIPPING_INELIGIBLE_INSTANCE_ITEM_MSG, FAILED);
    }
  }

  public void runItemMove(UUID centralServerId, Instance newInstance, Instance oldInstance, Item item, OngoingContributionStatus ongoingContributionStatus) {
    boolean eligibleItem = isEligibleForContribution(centralServerId, item);
    boolean contributedItem = isContributed(centralServerId, oldInstance, item);
    log.info("runItemMove:: eligibleItem: {}, contributedItem: {}", eligibleItem, contributedItem);
    if (!eligibleItem && !contributedItem) {
      log.info("runItemMove:: Skipping ineligible and non-contributed item id: {}, new instance id: {}, old instance id: {}", item.getId(), newInstance.getId(), oldInstance.getId());
      ongoingContributionStatusService.updateOngoingContribution(ongoingContributionStatus, SKIPPING_INELIGIBLE_MSG, FAILED);
      return;
    }

    // de-contribute item and update old instance
    if (contributedItem) {
      if (isEligibleForContribution(centralServerId, oldInstance)) {
        log.info("runItemMove:: de-contributing item : {} from old instance id : {}", item.getId(), oldInstance.getId());
        recordContributionService.deContributeItem(centralServerId, item);
        log.info("runItemMove:: re-contributing old instance id:{} to update bib status, item id; {}", oldInstance.getId(), item.getId());
        recordContributionService.contributeInstance(centralServerId, oldInstance);
      } else {
        log.info("runItemMove:: e-contributing old instance id: {}, item id: {}", oldInstance.getId(), item.getId());
        recordContributionService.deContributeInstance(centralServerId, oldInstance);
      }
    }

    // contribute item to a new instance
    if (isEligibleForContribution(centralServerId, newInstance)) {
      log.info("runItemMove:: re-contributing new instance id: {} to update bib status, item id: {}", newInstance.getId(), item.getId());
      recordContributionService.contributeInstance(centralServerId, newInstance);

      if (eligibleItem) {
        log.info("runItemMove:: Contributing item to new instance id: {}, item id: {}", newInstance.getId(), item.getId());
        recordContributionService.contributeItems(centralServerId, newInstance.getHrid(), List.of(item));
      }
    }
    ongoingContributionStatusService.updateOngoingContribution(ongoingContributionStatus, PROCESSED);
  }

  public void runItemDeContribution(UUID centralServerId, Instance instance, Item deletedItem, OngoingContributionStatus ongoingContributionStatus) {
    if (!isContributed(centralServerId, instance, deletedItem)) {
      log.info("runItemDeContribution:: Skipping non-contributed item id: {}, instance id: {}", deletedItem.getId(), instance.getId());
      ongoingContributionStatusService.updateOngoingContribution(ongoingContributionStatus, SKIPPING_INELIGIBLE_MSG, FAILED);
      return;
    }

    if (isEligibleForContribution(centralServerId, instance)) {
      log.info("runItemDeContribution:: de-contributing centralServer id: {}, item id: {}, instance id: {}",
        centralServerId, deletedItem.getId(), instance.getId());
      recordContributionService.deContributeItem(centralServerId, deletedItem);

      log.info("runItemDeContribution:: re-contributing instance to update bib status centralServer id: {}, item id: {}, instance id: {}",
        centralServerId, deletedItem.getId(), instance.getId());
      recordContributionService.contributeInstance(centralServerId, instance);
      ongoingContributionStatusService.updateOngoingContribution(ongoingContributionStatus, PROCESSED);
    } else {
      log.info("runItemDeContribution:: {}, centralServer id: {}, item id: {}, instance id : {}",
        DE_CONTRIBUTE_INSTANCE_MSG, centralServerId, deletedItem.getId(), instance.getId());
      recordContributionService.deContributeInstance(centralServerId, instance);
      ongoingContributionStatusService.updateOngoingContribution(ongoingContributionStatus, DE_CONTRIBUTED);
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
      .toList();

    if (items.isEmpty()) {
      log.info("item is empty while contributing instance id: {}", instance.getId());
      addRecordProcessed();
    }

    int chunkSize = max(jobProperties.getChunkSize(), 1);

    StreamSupport.stream(Iterables.partition(items, chunkSize).spliterator(), false)
      .forEach(itemsChunk -> contributeItemsChunk(centralServerId, bibId, itemsChunk, stats));
  }

  private void contributeOngoingItems(UUID centralServerId, Instance instance) {
    log.debug("contributeOngoingItems:: parameters centralServerId: {}, instance id: {}", centralServerId, instance.getId());
    var bibId = instance.getHrid();
    var items = instance.getItems().stream()
      .filter(i -> isEligibleForContribution(centralServerId, i)).toList();
    if (items.isEmpty()) {
      log.info("contributeOngoingItems:: no items for contribution for instance id: {}", instance.getId());
      return;
    }
    int chunkSize = max(jobProperties.getChunkSize(), 1);
    StreamSupport.stream(Iterables.partition(items, chunkSize).spliterator(), false)
      .forEach(itemsChunk -> recordContributionService.contributeItemsWithoutRetry(centralServerId, bibId, itemsChunk));
    log.info("contributeOngoingItems:: Item contribution completed for instanceId {} ", instance.getId());
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
    catch (ServiceSuspendedException | HttpClientErrorException | HttpServerErrorException | InnReachConnectionException | InnReachContributionRequestException ex) {
      log.error("contributeItemsChunk:: exception occurred: {}", ex.getMessage(), ex);
      throw ex;
    } catch (Exception ex) {
      // not possible to guess what item failed when the chunk of multiple items is being contributed
      var recordId = items.size() == 1 ? items.getFirst().getId() : null;
      itemExceptionListener.logWriteError(ex, recordId);
      log.warn("contributeItemsChunk:: unexpected exception occurred on items contribution for bib id: {}", bibId, ex);
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
    try {
      stats.addRecordsTotal(1);
      recordContributionService.contributeInstance(centralServerId, instance);
      stats.addRecordsContributed(1);
    }
    catch (ServiceSuspendedException | HttpServerErrorException | HttpClientErrorException | InnReachConnectionException |
           InnReachContributionRequestException e) {
      log.warn(getContributionJobContext().isInitialContribution() ? "Initial: instance id:{}, exception occurred: {}": "Ongoing: instance id:{}, exception occurred: {}", instance.getId(), e);
      throw e;
    } catch (Exception e) {
      log.warn("contributeInstance:: Initial: instance id: {}, exception caught: {}", instance.getId(), e.getMessage(), e);
      instanceExceptionListener.logWriteError(e, instance.getId());
    } finally {
      log.info("contributeInstance:: contribution for instance: {} finished", instance.getId());
      stats.addRecordsProcessed(1);
      updateStats(stats);
    }
  }

  private void deContributeInstance(UUID centralServerId, Instance instance, Statistics stats) {
    try {
      stats.addRecordsTotal(1);
      recordContributionService.deContributeInstance(centralServerId, instance);
      stats.addRecordsDeContributed(1);
      addRecordProcessed();
    } catch (ServiceSuspendedException | HttpClientErrorException | HttpServerErrorException | InnReachConnectionException ex) {
      log.error("deContributeInstance:: exception occurred on de-contribution of instance id {}: {}", instance.getId(), ex.getMessage(), ex);
      throw ex;
    } catch (Exception ex) {
      log.error("deContributeInstance:: unexpected exception occurred on de-contribution of instance id {}: {}", instance.getId(), ex.getMessage(), ex);
      instanceExceptionListener.logWriteError(ex, instance.getId());
    } finally {
      log.info("deContributeInstance:: de-contribution of instance id {} has finished", instance.getId());
      stats.addRecordsProcessed(1);
      updateStats(stats);
    }
  }

  private boolean isUnknownEvent(InstanceIterationEvent event, UUID iterationJobId) {
    return !Objects.equals(event.getJobId(), iterationJobId);
  }

  public void completeContribution(ContributionJobContext context) {
    try {
      contributionService.completeContribution(context.getContributionId());
      log.info("completeContribution:: Completed contribution: {}", context.getContributionId());
    } catch (Exception e) {
      log.error("completeContribution:: Failed to complete contribution with id: {}", context.getContributionId(), e);
    }
  }

  private void updateStats(Statistics stats) {
    statsListener.updateStats(stats);
  }

  private Instance loadInstanceWithItems(UUID instanceId) {
    Instance instance = null;
    try {
      instance = retryTemplate.execute(() -> inventoryViewService.getInstance(instanceId));
    } catch (Exception e) {
      log.error("loadInstanceWithItems:: exception occurred with instance id: {}", instanceId, e);
      instanceExceptionListener.logProcessError(e, instanceId);
    }
    log.info("loadInstanceWithItems:: loaded instance with items, instance id: {}", instanceId);
    return instance;
  }
}
