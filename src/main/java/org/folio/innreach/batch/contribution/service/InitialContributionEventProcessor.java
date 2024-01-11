package org.folio.innreach.batch.contribution.service;

import com.google.common.collect.Iterables;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.innreach.batch.contribution.listener.ContributionExceptionListener;
import org.folio.innreach.dto.Instance;
import org.folio.innreach.dto.Item;
import org.folio.innreach.config.props.ContributionJobProperties;
import org.folio.innreach.domain.entity.Contribution;
import org.folio.innreach.domain.entity.JobExecutionStatus;
import org.folio.innreach.domain.service.ContributionValidationService;
import org.folio.innreach.domain.service.InventoryViewService;
import org.folio.innreach.domain.service.RecordContributionService;
import org.folio.innreach.domain.service.impl.TenantScopedExecutionService;
import org.folio.innreach.external.exception.InnReachConnectionException;
import org.folio.innreach.external.exception.InnReachGatewayException;
import org.folio.innreach.external.exception.RetryException;
import org.folio.innreach.external.exception.ServiceSuspendedException;
import org.folio.innreach.external.exception.SocketTimeOutExceptionWrapper;
import org.folio.innreach.repository.ContributionRepository;
import org.folio.innreach.repository.JobExecutionStatusRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.net.SocketTimeoutException;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.StreamSupport;

import static java.lang.Math.max;
import static org.folio.innreach.domain.entity.JobExecutionStatus.Status.DE_CONTRIBUTED;
import static org.folio.innreach.domain.entity.JobExecutionStatus.Status.FAILED;
import static org.folio.innreach.domain.entity.JobExecutionStatus.Status.PROCESSED;
import static org.folio.innreach.domain.entity.JobExecutionStatus.Status.READY;
import static org.folio.innreach.domain.entity.JobExecutionStatus.Status.RETRY;

@Service
@Log4j2
@RequiredArgsConstructor
public class InitialContributionEventProcessor {
  @Qualifier("instanceExceptionListener")
  private final ContributionExceptionListener instanceExceptionListener;
  @Qualifier("itemExceptionListener")
  private final ContributionExceptionListener itemExceptionListener;
  private final InventoryViewService inventoryViewService;
  private final RecordContributionService recordContributionService;
  private final ContributionValidationService validationService;
  private final ContributionJobProperties jobProperties;
  private final JobExecutionStatusRepository jobExecutionStatusRepository;
  private static final ConcurrentHashMap<UUID, Contribution> contributionRecord = new ConcurrentHashMap<>();
  private final ContributionRepository contributionRepository;
  private final TenantScopedExecutionService executionService;
  @Value("${initial-contribution.retry-attempts}")
  private int maxRetryAttempts;

  @Async("schedulerTaskExecutor")
  public void processInitialContributionEvents(JobExecutionStatus job) {
    executionService.executeAsyncTenantScoped(job.getTenant(), () -> {
      log.info("processInitialContributionEvents:: Processing Initial contribution events {}", job);
      try {
        var instanceId = job.getInstanceId();
        var centralServerId = contributionRecord.get(job.getJobId()) != null ?
          contributionRecord.get(job.getJobId()).getCentralServer().getId() : getCentralServerId(job.getJobId());
        var instance = inventoryViewService.getInstance(instanceId);
        if (centralServerId == null || instance == null) {
          log.warn("processInitialContributionEvents:: Unable to process event with instance " +
            "id {} centralServerId {} ", instanceId, centralServerId);
          updateJobAndContributionStatus(job, FAILED, job.isInstanceContributed());
          return;
        }
        checkRetryLimit(job);
        startContribution(centralServerId, instance, job);
      } catch (ServiceSuspendedException | InnReachConnectionException |
               SocketTimeOutExceptionWrapper | InnReachGatewayException ex) {
        log.warn("processInitialContributionEvents:: Retrying the contribution for {}th time with instanceId {} due to {}",
          job.getRetryAttempts(), job.getInstanceId(), ex.getMessage());
        updateJobAndContributionStatus(job, RETRY, job.isInstanceContributed());
      } catch (Exception ex) {
        log.warn("processInitialContributionEvents:: Exception while processing instanceId {}", job.getInstanceId());
        logException(job, ex, contributionRecord.get(job.getJobId()).getId());
        updateJobAndContributionStatus(job, FAILED, job.isInstanceContributed());
      }
    });

  }

  private void checkRetryLimit(JobExecutionStatus job) {
    if (maxRetryAttempts != 0 && job.getRetryAttempts() > maxRetryAttempts) {
      log.warn("checkRetryLimit:: Retry limit exhausted for instanceId {} ", job.getInstanceId());
      throw new RetryException("Retry limit exhausted");
    }
  }

  private UUID getCentralServerId(UUID jobId) {
    Contribution contribution = contributionRepository.findByJobId(jobId);
    if (contribution != null && contribution.getCentralServer() != null) {
      contributionRecord.put(jobId, contribution);
      return contribution.getCentralServer().getId();
    }
    return null;
  }

  private void startContribution(UUID centralServerId, Instance instance, JobExecutionStatus job) throws SocketTimeoutException {
    var instanceId = job.getInstanceId();
    if (isEligibleForContribution(centralServerId, instance)) {
      if (job.isInstanceContributed()) {
        log.info("startContribution:: Item contribution started for centralServerId: {}, instanceId: {}", centralServerId, instanceId);
        contributeItems(job, centralServerId, instance);
      } else {
        log.info("startContribution:: Instance contribution started for instanceId {} ", instance.getId());
        recordContributionService.contributeInstanceWithoutRetry(centralServerId, instance);
        updateJobAndContributionStatus(job, READY, true);
      }
    } else if (isContributed(centralServerId, instance)) {
      log.info("startContribution:: deContributeInstance centralServerId: {}, instanceId: {}", centralServerId, instanceId);
      recordContributionService.deContributeInstance(centralServerId, instance);
      updateJobAndContributionStatus(job, DE_CONTRIBUTED, job.isInstanceContributed());
    } else {
      // Update the status of non-eligible instance id
      log.info("startContribution:: non-eligible instance id: {}", instanceId);
      updateJobAndContributionStatus(job, FAILED, job.isInstanceContributed());
    }
  }

  private void contributeItems(JobExecutionStatus job, UUID centralServerId, Instance instance) {
    log.debug("contributeItem:: parameters centralServerId: {}, instance id: {}", centralServerId, instance.getId());
    var bibId = instance.getHrid();
    var items = instance.getItems().stream()
      .filter(i -> isEligibleForContribution(centralServerId, i)).toList();
    if (items.isEmpty()) {
      log.info("item is empty while contributing instance id: {}", instance.getId());
      updateJobAndContributionStatus(job, PROCESSED, job.isInstanceContributed());
    }
    int chunkSize = max(jobProperties.getChunkSize(), 1);
    StreamSupport.stream(Iterables.partition(items, chunkSize).spliterator(), false)
      .forEach(itemsChunk -> recordContributionService.contributeItemsWithoutRetry(centralServerId, bibId, itemsChunk));
    log.info("contributeItem:: Item contribution completed for instanceId {} ", instance.getId());
    updateJobAndContributionStatus(job, PROCESSED, job.isInstanceContributed());
  }

  private void updateJobAndContributionStatus(JobExecutionStatus job, JobExecutionStatus.Status status, boolean isInstanceContributed) {
    job.setStatus(status);
    job.setInstanceContributed(isInstanceContributed);
    job.setRetryAttempts(status.equals(RETRY) ? job.getRetryAttempts() + 1 : job.getRetryAttempts());
    jobExecutionStatusRepository.save(job);
  }

  private boolean isEligibleForContribution(UUID centralServerId, Instance instance) {
    log.info("isEligibleForContribution:: parameters centralServerId: {} and instance id: {}",
      centralServerId, instance);
    return validationService.isEligibleForContribution(centralServerId, instance);
  }

  private boolean isContributed(UUID centralServerId, Instance instance) {
    log.info("isContributed:: parameters centralServerId: {}, instance id: {}",
      centralServerId, instance.getId());
    return recordContributionService.isContributed(centralServerId, instance);
  }

  private boolean isEligibleForContribution(UUID centralServerId, Item item) {
    log.info("isEligibleForContribution:: parameters centralServerId: {}, item id: {}",
      centralServerId, item.getId());
    return validationService.isEligibleForContribution(centralServerId, item);
  }

  private void logException(JobExecutionStatus job, Exception ex, UUID contributionId) {
    if(job.isInstanceContributed()) {
      itemExceptionListener.logError(ex, job.getInstanceId(), contributionId);
    } else {
      instanceExceptionListener.logError(ex, job.getInstanceId(), contributionId);
    }
  }

}
