package org.folio.innreach.batch.contribution.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.innreach.domain.entity.OngoingContributionStatus;
import org.folio.innreach.domain.service.ContributionActionService;
import org.folio.innreach.domain.service.InnReachTransactionActionService;
import org.folio.innreach.domain.service.impl.TenantScopedExecutionService;
import org.folio.innreach.external.exception.InnReachConnectionException;
import org.folio.innreach.external.exception.InnReachGatewayException;
import org.folio.innreach.external.exception.RetryException;
import org.folio.innreach.external.exception.ServiceSuspendedException;
import org.folio.innreach.external.exception.SocketTimeOutExceptionWrapper;
import org.folio.innreach.util.JsonHelper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.folio.innreach.dto.Item;

import static org.folio.innreach.domain.entity.ContributionStatus.FAILED;
import static org.folio.innreach.domain.entity.ContributionStatus.RETRY;
import static org.folio.innreach.util.InnReachConstants.RETRY_LIMIT_MESSAGE;
import static org.folio.innreach.util.InnReachConstants.UNKNOWN_TYPE_MESSAGE;

@Service
@RequiredArgsConstructor
@Log4j2
public class OngoingContributionEventProcessor {

  private final ContributionActionService contributionActionService;
  private final InnReachTransactionActionService transactionActionService;
  private final JsonHelper jsonHelper;
  private final OngoingContributionStatusService ongoingContributionStatusService;
  private final TenantScopedExecutionService executionService;
  @Value("${contribution.retry-attempts}")
  private int maxRetryAttempts;

  @Async("ongoingSchedulerTaskExecutor")
  public void processOngoingContribution(OngoingContributionStatus ongoingContributionStatus) {
    try {
      log.info("processOngoingContribution:: Processing ongoing contribution event with id {} , tenant {}",
        ongoingContributionStatus.getId(), ongoingContributionStatus.getTenant());
      executionService.executeAsyncTenantScoped(ongoingContributionStatus.getTenant(),
        () -> {
          checkRetryLimit(ongoingContributionStatus);
          switch (ongoingContributionStatus.getDomainEventName()) {
            case ITEM:
              processItem(ongoingContributionStatus);
            default:
          }
        });
    } catch (ServiceSuspendedException | InnReachConnectionException |
             SocketTimeOutExceptionWrapper | InnReachGatewayException ex) {
      ongoingContributionStatusService.updateOngoingContribution(ongoingContributionStatus, RETRY);
    } catch (Exception ex) {
      ongoingContributionStatusService.updateOngoingContribution(ongoingContributionStatus, ex.getMessage(), FAILED);
    }
  }

  private void processItem(OngoingContributionStatus ongoingContributionStatus) {
    Item oldEntity = jsonHelper.fromJson(ongoingContributionStatus.getOldEntity(), Item.class);
    Item newEntity = jsonHelper.fromJson(ongoingContributionStatus.getNewEntity(), Item.class);
    switch (ongoingContributionStatus.getDomainEventType()) {
      case CREATED -> contributionActionService.handleItemCreation(newEntity, ongoingContributionStatus);
      case UPDATED -> {
        contributionActionService.handleItemUpdate(newEntity, oldEntity, ongoingContributionStatus);
        transactionActionService.handleItemUpdate(newEntity, oldEntity);
      }
      case DELETED -> contributionActionService.handleItemDelete(oldEntity, ongoingContributionStatus);
      default ->
        ongoingContributionStatusService.updateOngoingContribution(ongoingContributionStatus, UNKNOWN_TYPE_MESSAGE, FAILED);
    }
  }

  private void checkRetryLimit(OngoingContributionStatus job) {
    if (maxRetryAttempts != 0 && job.getRetryAttempts() > maxRetryAttempts) {
      log.warn("checkRetryLimit:: Retry limit exhausted for jobId {} ", job.getId());
      throw new RetryException(RETRY_LIMIT_MESSAGE);
    }
  }

}
