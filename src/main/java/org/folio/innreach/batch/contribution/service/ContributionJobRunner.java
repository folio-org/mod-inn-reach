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
import org.folio.innreach.domain.entity.JobExecution;
import org.folio.innreach.domain.entity.JobExecutionStatus;
import org.folio.innreach.external.exception.InnReachConnectionException;
import org.folio.innreach.external.exception.ServiceSuspendedException;
import org.folio.innreach.batch.contribution.InitialContributionJobConsumerContainer;
import org.folio.innreach.external.exception.SocketTimeOutExceptionWrapper;
import org.folio.innreach.repository.JobExecutionStatusRepository;
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

  private static Map<String,Integer> totalRecords = new HashMap<>();
  private static Map<String, JobExecution> jobExecutions = new HashMap<>();
  private static ConcurrentHashMap<String, Integer> recordsProcessed = new ConcurrentHashMap<>();
  private final JobExecutionStatusRepository jobExecutionStatusRepository;


  public void startInitialContribution(UUID centralServerId, String tenantId, UUID contributionId, UUID iterationJobId, Integer numberOfRecords, JobExecution jobExecution) {
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
    jobExecutions.remove(tenantId);
    recordsProcessed.remove(tenantId);
    stats.clearStats();

    beginContributionJobContext(context);

    totalRecords.put(context.getTenantId(), numberOfRecords);
    jobExecutions.put(context.getTenantId(), jobExecution);

    log.info("Starting initial contribution: totalRecords: {}", totalRecords);

    InitialContributionJobConsumerContainer container = itemReaderFactory.createInitialContributionConsumerContainer(tenantId,this);

    InitialContributionMessageListener initialContributionMessageListener = new InitialContributionMessageListener(new ContributionProcessor(this));

    container.tryStartOrCreateConsumer(initialContributionMessageListener);
  }

  public void runInitialContribution(InstanceIterationEvent event, String topic) {

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
    log.info("Initial: processing instance iteration event: {}", event);

    var instanceId = event.getInstanceId();

    if (isUnknownEvent(event, iterationJobId)) {
      log.info("Initial: skipping unknown event, current job is {}", iterationJobId);
      return;
    }

    Instance instance = loadInstanceWithItems(instanceId);

    if (instance == null) {
      log.info("Initial: instance is null, skipping");
      return;
    }

    var centralServerId = context.getCentralServerId();
    try{
      JobExecutionStatus jobExecutionStatus = new JobExecutionStatus();
      jobExecutionStatus.setJobExecution(jobExecutions.get(context.getTenantId()));
      jobExecutionStatus.setType(event.getType());
      jobExecutionStatus.setTenant(event.getTenant());
      jobExecutionStatus.setInstanceId(event.getInstanceId());
      jobExecutionStatus.setStatus(JobExecutionStatus.Status.READY);
      log.info("saving job execution status {} ", jobExecutionStatus);
      jobExecutionStatusRepository.save(jobExecutionStatus);
    }catch (Exception ex) {
      log.info("Inside exception {}", ex.getMessage());
    }
//    recordsProcessed.put(context.getTenantId(), recordsProcessed.get(context.getTenantId()) == null ? 1
//        : recordsProcessed.get(context.getTenantId())+1);

//    if (isEligibleForContribution(centralServerId, instance)) {
//      contributeInstance(centralServerId, instance, stats);
//      contributeInstanceItems(centralServerId, instance, stats);
//    } else if (isContributed(centralServerId, instance)) {
//      log.info("Initial: deContributeInstance");
//      deContributeInstance(centralServerId, instance, stats);
//    }
//    else {
//      // to test if non-eligible increasing count to verify the stopping condition
//      log.info("Initial: non-eligible instance");
//      ContributionJobRunner.recordsProcessed.put(context.getTenantId(), recordsProcessed.get(context.getTenantId()) == null ? 1
//        : recordsProcessed.get(context.getTenantId())+1);
//
//    }
    long recordsInserted = jobExecutionStatusRepository.countByJobExecutionId(jobExecutions.get(context.getTenantId()).getId());
    long totalRecords = jobExecutions.get(context.getTenantId()).getTotalRecords();
    log.info("recordsInserted {} total records {} ", recordsInserted, totalRecords);
    if(Objects.equals(recordsInserted, totalRecords)) {
      log.info("Initial: consumer is stopping as all processed");
      contributionService.completeJobExecution(jobExecutions.get(context.getTenantId()).getId());
      completeContribution(context);
      stopContribution(context.getTenantId());
      InitialContributionJobConsumerContainer.stopConsumer(topic);
    }
//    if (Objects.equals(recordsProcessed.get(context.getTenantId()), totalRecords.get(context.getTenantId()))) {
//      log.info("Initial: consumer is stopping as all processed");
//      completeContribution(context);
//      stopContribution(context.getTenantId());
//      InitialContributionJobConsumerContainer.stopConsumer(topic);
//    }
  }

  public void stopContribution(String tenantId) {
    log.info("stopContribution called");
    endContributionJobContext();
    totalRecords.remove(tenantId);
    recordsProcessed.remove(tenantId);
  }

  public void cancelContributionIfRetryExhausted(UUID centralServerId) {
    log.info("cancelContributionIfRetryExhausted called");
    contributionService.cancelCurrent(centralServerId);
  }

  public void runInstanceContribution(UUID centralServerId, Instance instance) {
    log.info("Ongoing: validating instance {} for contribution to central server {}", instance.getId(), centralServerId);

    boolean eligibleInstance = isEligibleForContribution(centralServerId, instance);
    boolean contributedInstance = isContributed(centralServerId, instance);

    if (!eligibleInstance && !contributedInstance) {
      log.info("Ongoing: skipping ineligible and non-contributed instance");
      return;
    }

    runOngoing(centralServerId, (ctx, statistics) -> {
      log.info("Ongoing: starting ongoing instance contribution job {}", ctx);

      if (eligibleInstance) {
        log.info("Ongoing: contributing instance");
        contributeInstance(centralServerId, instance, statistics);

        if (!contributedInstance) {
          log.info("Ongoing: contributing items of new instance");
          contributeInstanceItems(centralServerId, instance, statistics);
        }
      } else if (contributedInstance) {
        log.info("Ongoing: " + DE_CONTRIBUTE_INSTANCE_MSG);
        deContributeInstance(centralServerId, instance, statistics);
      }
    });
  }

  public void runInstanceDeContribution(UUID centralServerId, Instance deletedInstance) {
    log.info("Validating instance {} for de-contribution from central server {}", deletedInstance.getId(), centralServerId);

    if (!isContributed(centralServerId, deletedInstance)) {
      log.info("Skipping non-contributed instance");
      return;
    }

    runOngoing(centralServerId, (ctx, statistics) -> {
      log.info("Starting ongoing instance de-contribution job {}", ctx);
      deContributeInstance(centralServerId, deletedInstance, statistics);
    });
  }

  public void runItemContribution(UUID centralServerId, Instance instance, Item item) {
    log.info("Ongoing: validating item {} for contribution to central server {}", item.getId(), centralServerId);

    boolean eligibleItem = isEligibleForContribution(centralServerId, item);
    boolean contributedItem = isContributed(centralServerId, instance, item);

    if (!eligibleItem && !contributedItem) {
      log.info("Ongoing: skipping ineligible and non-contributed item");
      return;
    }

    runOngoing(centralServerId, (ctx, statistics) -> {
      log.info("Starting ongoing item contribution job {}", ctx);

      if (isEligibleForContribution(centralServerId, instance)) {
        log.info("Ongoing: Re-contributing instance to update bib status");
        contributeInstance(centralServerId, instance, statistics);

        if (eligibleItem) {
          log.info("Ongoing: contributing item");
          contributeItem(centralServerId, instance.getHrid(), item, statistics);
        } else if (contributedItem) {
          log.info("Ongoing: de-contributing item");
          deContributeItem(centralServerId, item, statistics);
        }
      } else if (contributedItem) {
        log.info("Ongoing: " + DE_CONTRIBUTE_INSTANCE_MSG);
        deContributeInstance(centralServerId, instance, statistics);
      }
    });
  }

  public void runItemMove(UUID centralServerId, Instance newInstance, Instance oldInstance, Item item) {
    log.info("Validating item {} for moving to a new instance {} on central server {}", item.getId(), newInstance.getId(), centralServerId);

    boolean eligibleItem = isEligibleForContribution(centralServerId, item);
    boolean contributedItem = isContributed(centralServerId, oldInstance, item);

    if (!eligibleItem && !contributedItem) {
      log.info("Skipping ineligible and non-contributed item");
      return;
    }

    runOngoing(centralServerId, (ctx, statistics) -> {
      log.info("Starting ongoing item move job {}", ctx);

      // de-contribute item and update old instance
      if (contributedItem) {
        if (isEligibleForContribution(centralServerId, oldInstance)) {
          log.info("Ongoing: de-contributing item from old instance");
          deContributeItem(centralServerId, item, statistics);

          log.info("Ongoing: re-contributing old instance to update bib status");
          contributeInstance(centralServerId, oldInstance, statistics);
        } else {
          log.info("Ongoing: e-contributing old instance");
          deContributeInstance(centralServerId, oldInstance, statistics);
        }
      }

      // contribute item to a new instance
      if (isEligibleForContribution(centralServerId, newInstance)) {
        log.info("Ongoing: re-contributing new instance to update bib status");
        contributeInstance(centralServerId, newInstance, statistics);

        if (eligibleItem) {
          log.info("Ongoing: Contributing item to new instance");
          contributeItem(centralServerId, newInstance.getHrid(), item, statistics);
        }
      }
    });
  }

  public void runItemDeContribution(UUID centralServerId, Instance instance, Item deletedItem) {
    log.info("Validating item {} for de-contribution from central server {}", deletedItem.getId(), centralServerId);
    if (!isContributed(centralServerId, instance, deletedItem)) {
      log.info("Skipping non-contributed item");
      return;
    }

    runOngoing(centralServerId, (context, statistics) -> {
      log.info("Starting ongoing item de-contribution job {}", context);

      if (isEligibleForContribution(centralServerId, instance)) {
        log.info("Ongoing: de-contributing item");
        deContributeItem(centralServerId, deletedItem, statistics);

        log.info("Ongoing: re-contributing instance to update bib status");
        contributeInstance(centralServerId, instance, statistics);
      } else {
        log.info("Ongoing: " + DE_CONTRIBUTE_INSTANCE_MSG);
        deContributeInstance(centralServerId, instance, statistics);
      }
    });
  }

  public void cancelJobs() {
    log.info("Cancelling unfinished contributions...");
    contributionService.cancelAll();
    runningInitialContributions.clear();
  }

  public void cancelInitialContribution(UUID contributionId) {
    log.info("Cancelling initial contribution job {}", contributionId);
    runningInitialContributions.remove(contributionId);
  }

  private boolean isEligibleForContribution(UUID centralServerId, Instance instance) {
    return validationService.isEligibleForContribution(centralServerId, instance);
  }

  private boolean isContributed(UUID centralServerId, Instance instance) {
    return recordContributionService.isContributed(centralServerId, instance);
  }

  private boolean isEligibleForContribution(UUID centralServerId, Item item) {
    return validationService.isEligibleForContribution(centralServerId, item);
  }

  private boolean isContributed(UUID centralServerId, Instance instance, Item item) {
    return recordContributionService.isContributed(centralServerId, instance, item);
  }

  private void contributeInstanceItems(UUID centralServerId, Instance instance, Statistics stats) {
    var bibId = instance.getHrid();
    var items = instance.getItems().stream()
      .filter(i -> isEligibleForContribution(centralServerId, i))
      .collect(Collectors.toList());

    if (items.isEmpty()) {
      log.info("item is empty while contributing");
      addRecordProcessed();
    }

    int chunkSize = max(jobProperties.getChunkSize(), 1);

    StreamSupport.stream(Iterables.partition(items, chunkSize).spliterator(), false)
      .forEach(itemsChunk -> contributeItemsChunk(centralServerId, bibId, itemsChunk, stats));
  }

  private void contributeItem(UUID centralServerId, String bibId, Item item, Statistics stats) {
    contributeItemsChunk(centralServerId, bibId, List.of(item), stats);
  }

  private void contributeItemsChunk(UUID centralServerId, String bibId, List<Item> items, Statistics stats) {
    var itemsCount = items.size();
    try {
      stats.addRecordsTotal(itemsCount);
      recordContributionService.contributeItems(centralServerId, bibId, items);
      stats.addRecordsContributed(itemsCount);
      addRecordProcessed();
    }
    catch (ServiceSuspendedException | FeignException | InnReachConnectionException e) {
      log.info(getContributionJobContext().isInitialContribution() ? "Initial: exception occurred:": "Ongoing: exception occurred:", e);
      throw e;
    }
    catch (SocketTimeoutException socketTimeoutException) {
      log.info(getContributionJobContext().isInitialContribution() ? "Initial: socket exception occurred:": "Ongoing: socket exception occurred:", socketTimeoutException);
      throw new SocketTimeOutExceptionWrapper(socketTimeoutException.getMessage());
    }
    catch (Exception e) {
      // not possible to guess what item failed when the chunk of multiple items is being contributed
      var recordId = items.size() == 1 ? items.get(0).getId() : null;
      itemExceptionListener.logWriteError(e, recordId);
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
    log.info("Initial: contributeInstance instanceId: {}", instance.getId());
    try {
      stats.addRecordsTotal(1);
      recordContributionService.contributeInstance(centralServerId, instance);
      stats.addRecordsContributed(1);
    }
    catch (ServiceSuspendedException | FeignException | InnReachConnectionException e) {
      log.info(getContributionJobContext().isInitialContribution() ? "Initial: exception occurred:": "Ongoing: exception occurred:", e);
      throw e;
    }
    catch (SocketTimeoutException socketTimeoutException) {
      log.info(getContributionJobContext().isInitialContribution() ? "Initial: socket exception occurred:": "Ongoing: socket exception occurred:", socketTimeoutException);
      throw new SocketTimeOutExceptionWrapper(socketTimeoutException.getMessage());
    }
    catch (Exception e) {
      log.info("Initial: exception caught in contributeInstance");
      instanceExceptionListener.logWriteError(e, instance.getId());
    } finally {
      log.info("Initial: contributeInstance finally called");
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
    }
    catch (ServiceSuspendedException | FeignException | InnReachConnectionException e) {
      throw e;
    }
    catch (SocketTimeoutException socketTimeoutException) {
      log.info("socketTimeoutException occur");
      throw new SocketTimeOutExceptionWrapper(socketTimeoutException.getMessage());
    }
    catch (Exception e) {
      instanceExceptionListener.logWriteError(e, instance.getId());
    } finally {
      stats.addRecordsProcessed(1);
      updateStats(stats);
    }
  }

  private void deContributeItem(UUID centralServerId, Item item, Statistics stats) {
    try {
      stats.addRecordsTotal(1);
      recordContributionService.deContributeItem(centralServerId, item);
      stats.addRecordsDeContributed(1);
      addRecordProcessed();
    }
    catch (ServiceSuspendedException | FeignException | InnReachConnectionException e) {
      throw e;
    }
    catch (Exception e) {
      itemExceptionListener.logWriteError(e, item.getId());
    } finally {
      stats.addRecordsProcessed(1);
      updateStats(stats);
    }
  }

  private void runOngoing(UUID centralServerId, BiConsumer<ContributionJobContext, Statistics> processor) {
    var contribution = contributionService.createOngoingContribution(centralServerId);
    var context = ContributionJobContext.builder()
      .contributionId(contribution.getId())
      .centralServerId(centralServerId)
      .tenantId(folioContext.getTenantId())
      .build();

    var statistics = new Statistics();
    try {
      if(getContributionJobContext()==null) {
        log.info("setting ongoing contribution context for contributionID:{}",context.getContributionId());
        beginContributionJobContext(context);
      }
      processor.accept(context, statistics);
      completeContribution(context);
      endContributionJobContext();
    }
    catch (ServiceSuspendedException | FeignException | InnReachConnectionException | SocketTimeOutExceptionWrapper e) {
      log.info("exception thrown from runOngoing");
      throw e;
    }
    catch (Exception e) {
      log.info("contributeInstance exception block :{}",e.getMessage());
      throw e;
    }
  }

  private boolean isUnknownEvent(InstanceIterationEvent event, UUID iterationJobId) {
    return !Objects.equals(event.getJobId(), iterationJobId);
  }

  public void completeContribution(ContributionJobContext context) {
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
    Instance instance = null;
    try {
      instance = retryTemplate.execute(r -> inventoryViewService.getInstance(instanceId));
    } catch (Exception e) {
      instanceExceptionListener.logProcessError(e, instanceId);
    }
    return instance;
  }
}
