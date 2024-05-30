package org.folio.innreach.batch.contribution.service;

import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.innreach.domain.entity.OngoingContributionStatus;
import org.folio.innreach.domain.service.ContributionActionService;
import org.folio.innreach.domain.service.InnReachTransactionActionService;
import org.folio.innreach.external.exception.InnReachConnectionException;
import org.folio.innreach.external.exception.InnReachGatewayException;
import org.folio.innreach.external.exception.ServiceSuspendedException;
import org.folio.innreach.external.exception.SocketTimeOutExceptionWrapper;
import org.folio.innreach.util.JsonHelper;
import org.springframework.stereotype.Service;
import org.folio.innreach.dto.Item;

import static org.folio.innreach.domain.entity.ContributionStatus.FAILED;
import static org.folio.innreach.domain.entity.ContributionStatus.RETRY;
import static org.folio.innreach.util.Constants.UNKNOWN_TYPE_MESSAGE;

@Service
@AllArgsConstructor
@Log4j2
public class OngoingContributionEventProcessor {

  private final ContributionActionService contributionActionService;
  private final InnReachTransactionActionService transactionActionService;
  private final JsonHelper jsonHelper;
  private final OngoingContributionStatusService ongoingContributionStatusService;

  public void processOngoingContribution(OngoingContributionStatus ongoingContributionStatus) {
    try {
      switch (ongoingContributionStatus.getDomainEventName()) {
        case ITEM: processItem(ongoingContributionStatus);
        default:
      }
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
      case CREATED ->
        contributionActionService.handleItemCreation(newEntity, ongoingContributionStatus);
      case UPDATED -> {
        contributionActionService.handleItemUpdate(newEntity, oldEntity, ongoingContributionStatus);
        transactionActionService.handleItemUpdate(newEntity, oldEntity);
      }
      case DELETED ->
        contributionActionService.handleItemDelete(oldEntity, ongoingContributionStatus);
      default ->
        ongoingContributionStatusService.updateOngoingContribution(ongoingContributionStatus, UNKNOWN_TYPE_MESSAGE, FAILED);
    }
  }

}
