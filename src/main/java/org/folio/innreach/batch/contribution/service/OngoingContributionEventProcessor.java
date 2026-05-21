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
    try {
      log.info("processOngoingContribution:: Processing ongoing contribution, event id: [{}], tenant: [{}]",
        ongoingContributionStatus.getId(), ongoingContributionStatus.getTenant());
      executionService.executeAsyncTenantScoped(ongoingContributionStatus.getTenant(),
        () -> {
          checkRetryLimit(ongoingContributionStatus);
          switch (ongoingContributionStatus.getDomainEventName()) {
            case ITEM -> processItem(ongoingContributionStatus);
            case HOLDINGS -> processHoldings(ongoingContributionStatus);
            case INSTANCE -> processInstance(ongoingContributionStatus);
            default ->
              ongoingContributionStatusService.updateOngoingContribution(ongoingContributionStatus, UNKNOWN_EVENT_NAME_MESSAGE, FAILED);
          }
        });
    } catch (ServiceSuspendedException | InnReachConnectionException | InnReachTimeOutException |
             InnReachGatewayException | InnReachContributionRequestException ex) {
      log.warn("processOngoingContribution:: {} occurred while processing ongoing contribution: {}", ex.getClass().getSimpleName(), ex.getMessage());
      ongoingContributionStatusService.updateOngoingContribution(ongoingContributionStatus, RETRY);
    } catch (Exception ex) {
      log.error("processOngoingContribution:: Exception occurred while processing job: {}", ex.getMessage(), ex);
      ongoingContributionStatusService.updateOngoingContribution(ongoingContributionStatus, ex.getMessage(), FAILED);
    }
  }

  private void processItem(OngoingContributionStatus ongoingContributionStatus) {
    var oldEntity = jsonHelper.fromJson(ongoingContributionStatus.getOldEntity(), Item.class);
    var newEntity = jsonHelper.fromJson(ongoingContributionStatus.getNewEntity(), Item.class);
    var eventType = ongoingContributionStatus.getDomainEventType();
    var itemId = eventType == DomainEventType.CREATED ? newEntity.getId() : oldEntity.getId();
    log.debug("processItem:: processing item with oldEntity {} and newEntity {}", oldEntity, newEntity);
    log.info("processItem:: processing event type [{}] for Item id: [{}]", eventType, itemId);

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

  private void processHoldings(OngoingContributionStatus ongoingContributionStatus) {
    Holding oldEntity = jsonHelper.fromJson(ongoingContributionStatus.getOldEntity(), Holding.class);
    Holding newEntity = jsonHelper.fromJson(ongoingContributionStatus.getNewEntity(), Holding.class);
    switch (ongoingContributionStatus.getDomainEventType()) {
      case UPDATED -> contributionActionService.handleHoldingUpdate(newEntity, ongoingContributionStatus);
      case DELETED -> contributionActionService.handleHoldingDelete(oldEntity, ongoingContributionStatus);
      default -> ongoingContributionStatusService.updateOngoingContribution(ongoingContributionStatus, UNKNOWN_TYPE_MESSAGE, FAILED);
    }
  }

  private void processInstance(OngoingContributionStatus ongoingContributionStatus) {
    Instance oldEntity = jsonHelper.fromJson(ongoingContributionStatus.getOldEntity(), Instance.class);
    Instance newEntity = jsonHelper.fromJson(ongoingContributionStatus.getNewEntity(), Instance.class);
    switch (ongoingContributionStatus.getDomainEventType()) {
      case CREATED -> contributionActionService.handleInstanceCreation(newEntity, ongoingContributionStatus);
      case UPDATED -> contributionActionService.handleInstanceUpdate(newEntity, ongoingContributionStatus);
      case DELETED -> contributionActionService.handleInstanceDelete(oldEntity, ongoingContributionStatus);
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
