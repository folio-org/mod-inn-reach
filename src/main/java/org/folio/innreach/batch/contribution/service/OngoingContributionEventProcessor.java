package org.folio.innreach.batch.contribution.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.innreach.domain.entity.OngoingContributionStatus;
import org.folio.innreach.domain.event.DomainEventType;
import org.folio.innreach.domain.service.ContributionActionService;
import org.folio.innreach.domain.service.InnReachTransactionActionService;
import org.folio.innreach.domain.service.impl.TenantScopedExecutionService;
import org.folio.innreach.dto.Holding;
import org.folio.innreach.dto.Instance;
import org.folio.innreach.dto.Item;
import org.folio.innreach.external.exception.InnReachConnectionException;
import org.folio.innreach.external.exception.InnReachContributionRequestException;
import org.folio.innreach.external.exception.InnReachGatewayException;
import org.folio.innreach.external.exception.InnReachOngoingContributionException;
import org.folio.innreach.external.exception.ServiceSuspendedException;
import org.folio.innreach.external.exception.InnReachTimeOutException;
import org.folio.innreach.util.JsonHelper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import static org.folio.innreach.domain.entity.ContributionStatus.FAILED;
import static org.folio.innreach.domain.entity.ContributionStatus.RETRY;
import static org.folio.innreach.external.exception.InnReachOngoingContributionException.ongoingContributionRetryExhausted;
import static org.folio.innreach.util.InnReachConstants.UNKNOWN_EVENT_NAME_MESSAGE;
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
    log.info("processOngoingContribution:: Processing ongoing contribution, event id: [{}], tenant: [{}]",
      ongoingContributionStatus.getId(), ongoingContributionStatus.getTenant());
    executionService.executeAsyncTenantScoped(ongoingContributionStatus.getTenant(),
      () -> {
        try {
          checkRetryLimit(ongoingContributionStatus);
          switch (ongoingContributionStatus.getDomainEventName()) {
            case ITEM -> processItem(ongoingContributionStatus);
            case HOLDINGS -> processHoldings(ongoingContributionStatus);
            case INSTANCE -> processInstance(ongoingContributionStatus);
            default ->
              ongoingContributionStatusService.updateOngoingContribution(ongoingContributionStatus, UNKNOWN_EVENT_NAME_MESSAGE, FAILED);
          }
        } catch (ServiceSuspendedException | InnReachConnectionException | InnReachTimeOutException |
                 InnReachGatewayException | InnReachContributionRequestException ex) {
          log.warn("processOngoingContribution:: {} occurred while processing ongoing contribution: {}", ex.getClass().getSimpleName(), ex.getMessage());
          ongoingContributionStatusService.updateOngoingContribution(ongoingContributionStatus, RETRY);
        } catch (InnReachOngoingContributionException ex) {
          log.warn("processOngoingContribution:: Retry exhausted for event id: [{}], tenant: [{}]: {}",
            ongoingContributionStatus.getId(), ongoingContributionStatus.getTenant(), ex.getMessage());
          ongoingContributionStatusService.updateOngoingContribution(ongoingContributionStatus, ex.getMessage(), FAILED);
        } catch (Exception ex) {
          log.error("processOngoingContribution:: Exception occurred while processing job: {}", ex.getMessage(), ex);
          ongoingContributionStatusService.updateOngoingContribution(ongoingContributionStatus, ex.getMessage(), FAILED);
        }
      });
  }

  private void processItem(OngoingContributionStatus ongoingContributionStatus) {
    var eventType = ongoingContributionStatus.getDomainEventType();

    switch (eventType) {
      case CREATED -> {
        var newEntity = jsonHelper.fromJson(ongoingContributionStatus.getNewEntity(), Item.class);
        log.info("processItem:: processing CREATED for Item id: [{}]", newEntity.getId());
        contributionActionService.handleItemCreation(newEntity, ongoingContributionStatus);
      }
      case UPDATED -> {
        var oldEntity = jsonHelper.fromJson(ongoingContributionStatus.getOldEntity(), Item.class);
        var newEntity = jsonHelper.fromJson(ongoingContributionStatus.getNewEntity(), Item.class);
        log.info("processItem:: processing UPDATED for Item id: [{}]", newEntity.getId());
        contributionActionService.handleItemUpdate(newEntity, oldEntity, ongoingContributionStatus);
        transactionActionService.handleItemUpdate(newEntity, oldEntity);
      }
      case DELETED -> {
        var oldEntity = jsonHelper.fromJson(ongoingContributionStatus.getOldEntity(), Item.class);
        log.info("processItem:: processing DELETED for Item id: [{}]", oldEntity.getId());
        contributionActionService.handleItemDelete(oldEntity, ongoingContributionStatus);
      }
      default ->
        ongoingContributionStatusService.updateOngoingContribution(ongoingContributionStatus, UNKNOWN_TYPE_MESSAGE, FAILED);
    }
  }

  private void processHoldings(OngoingContributionStatus ongoingContributionStatus) {
    switch (ongoingContributionStatus.getDomainEventType()) {
      case UPDATED -> {
        var newEntity = jsonHelper.fromJson(ongoingContributionStatus.getNewEntity(), Holding.class);
        contributionActionService.handleHoldingUpdate(newEntity, ongoingContributionStatus);
      }
      case DELETED -> {
        var oldEntity = jsonHelper.fromJson(ongoingContributionStatus.getOldEntity(), Holding.class);
        contributionActionService.handleHoldingDelete(oldEntity, ongoingContributionStatus);
      }
      default -> ongoingContributionStatusService.updateOngoingContribution(ongoingContributionStatus, UNKNOWN_TYPE_MESSAGE, FAILED);
    }
  }

  private void processInstance(OngoingContributionStatus ongoingContributionStatus) {
    switch (ongoingContributionStatus.getDomainEventType()) {
      case CREATED -> {
        var newEntity = jsonHelper.fromJson(ongoingContributionStatus.getNewEntity(), Instance.class);
        contributionActionService.handleInstanceCreation(newEntity, ongoingContributionStatus);
      }
      case UPDATED -> {
        var newEntity = jsonHelper.fromJson(ongoingContributionStatus.getNewEntity(), Instance.class);
        contributionActionService.handleInstanceUpdate(newEntity, ongoingContributionStatus);
      }
      case DELETED -> {
        var oldEntity = jsonHelper.fromJson(ongoingContributionStatus.getOldEntity(), Instance.class);
        contributionActionService.handleInstanceDelete(oldEntity, ongoingContributionStatus);
      }
      default -> ongoingContributionStatusService.updateOngoingContribution(ongoingContributionStatus, UNKNOWN_TYPE_MESSAGE, FAILED);
    }
  }

  private void checkRetryLimit(OngoingContributionStatus job) {
    if (maxRetryAttempts != 0 && job.getRetryAttempts() > maxRetryAttempts) {
      log.warn("checkRetryLimit:: ongoing job id {} retry attempts {} exceeds  max retry attempts {}",
        job.getId(), job.getRetryAttempts(), maxRetryAttempts);
      throw ongoingContributionRetryExhausted();
    }
  }

}
