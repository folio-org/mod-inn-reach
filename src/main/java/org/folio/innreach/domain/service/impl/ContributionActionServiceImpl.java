package org.folio.innreach.domain.service.impl;

import static org.folio.innreach.domain.entity.ContributionStatus.FAILED;
import static org.folio.innreach.domain.service.impl.MARCRecordTransformationServiceImpl.isMARCRecord;
import static org.folio.innreach.dto.MappingValidationStatusDTO.VALID;
import static org.folio.innreach.util.InnReachConstants.INVALID_CENTRAL_SERVER_ID;
import static org.folio.innreach.util.InnReachConstants.MARC_ERROR_MSG;

import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.innreach.batch.contribution.service.OngoingContributionStatusService;
import org.folio.innreach.domain.entity.OngoingContributionStatus;
import org.folio.innreach.external.exception.InnReachConnectionException;
import org.folio.innreach.external.exception.ServiceSuspendedException;
import org.folio.innreach.external.exception.SocketTimeOutExceptionWrapper;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

import org.folio.innreach.batch.contribution.service.ContributionJobRunner;
import org.folio.innreach.client.InstanceStorageClient;
import org.folio.innreach.client.ItemStorageClient;
import org.folio.innreach.domain.dto.folio.circulation.RequestDTO;
import org.folio.innreach.domain.service.ContributionActionService;
import org.folio.innreach.domain.service.ContributionValidationService;
import org.folio.innreach.domain.service.HoldingsService;
import org.folio.innreach.domain.service.InventoryViewService;
import org.folio.innreach.dto.Holding;
import org.folio.innreach.dto.Instance;
import org.folio.innreach.dto.Item;
import org.folio.innreach.dto.StorageLoanDTO;
import org.folio.innreach.repository.CentralServerRepository;
import org.folio.spring.data.OffsetRequest;

@Log4j2
@RequiredArgsConstructor
@Service
public class ContributionActionServiceImpl implements ContributionActionService {

  private final ContributionJobRunner contributionJobRunner;
  private final CentralServerRepository centralServerRepository;
  private final ItemStorageClient itemStorageClient;
  private final InventoryViewService inventoryViewService;
  private final InstanceStorageClient instanceStorageClient;
  private final HoldingsService holdingsService;
  private final ContributionValidationService validationService;
  private final OngoingContributionStatusService ongoingContributionStatusService;


  @Override
  public void handleInstanceCreation(Instance newInstance) {
    handleInstanceUpdate(newInstance);
  }

  @Override
  public void handleInstanceUpdate(Instance updatedInstance) {
    log.info("Handling instance creation/update {}", updatedInstance.getId());

    if (!isMARCRecord(updatedInstance)) {
      return;
    }

    var instance = fetchInstanceWithItems(updatedInstance.getId());

    handlePerCentralServer(instance.getId(), csId -> contributionJobRunner.runInstanceContribution(csId, instance));
  }


  @Override
  public void handleInstanceDelete(Instance deletedInstance) {
    log.info("Handling instance delete {}", deletedInstance.getId());

    if (!isMARCRecord(deletedInstance)) {
      return;
    }
    try {
      for (var csId : getCentralServerIds()) {
        contributionJobRunner.runInstanceDeContribution(csId, deletedInstance);
      }
    }
    catch (ServiceSuspendedException | FeignException | InnReachConnectionException e) {
      log.info("exception thrown from handleInstanceDelete");
      throw e;
    }
  }


  @Override
  public void handleItemCreation(Item newItem) {
    log.info("Handling item creation {}", newItem.getId());

    var instance = fetchInstanceWithItems(newItem);
    if (!isMARCRecord(instance)) {
      return;
    }

    handlePerCentralServer(newItem.getId(), csId -> contributionJobRunner.runItemContribution(csId, instance, newItem));
  }

  @Override
  public void handleItemCreation(Item newItem, OngoingContributionStatus ongoingContributionStatus) {
    log.info("Handling item creation {}", newItem.getId());

    var instance = fetchInstanceWithItems(newItem);
    if (!isMARCRecord(instance)) {
      ongoingContributionStatusService.updateOngoingContribution(ongoingContributionStatus, MARC_ERROR_MSG, FAILED);
      return;
    }
    var centralServerId = ongoingContributionStatus.getCentralServerId();
    if(checkCentralServerValid(centralServerId)) {
      contributionJobRunner.runItemContribution(centralServerId, instance, newItem, ongoingContributionStatus);
    } else {
      ongoingContributionStatusService.updateOngoingContribution(ongoingContributionStatus, INVALID_CENTRAL_SERVER_ID, FAILED);
    }
  }

  @Override
  public void handleItemUpdate(Item newItem, Item oldItem, OngoingContributionStatus ongoingContributionStatus) {
    log.info("Handling item update {}", newItem.getId());

    var instance = fetchInstanceWithItems(newItem);
    if (!isMARCRecord(instance)) {
      ongoingContributionStatusService.updateOngoingContribution(ongoingContributionStatus, MARC_ERROR_MSG, FAILED);
      return;
    }

    var oldInstance = fetchOldInstance(newItem, oldItem, instance);
    boolean itemMoved = !oldInstance.getId().equals(instance.getId());
    var centralServerId = ongoingContributionStatus.getCentralServerId();
    if(checkCentralServerValid(centralServerId)) {
      if (itemMoved) {
        contributionJobRunner.runItemMove(centralServerId, instance, oldInstance, newItem, ongoingContributionStatus);
      } else {
        contributionJobRunner.runItemContribution(centralServerId, instance, newItem, ongoingContributionStatus);
      }
    } else {
      ongoingContributionStatusService.updateOngoingContribution(ongoingContributionStatus, INVALID_CENTRAL_SERVER_ID, FAILED);
    }
  }

  @Override
  public void handleItemUpdate(Item newItem, Item oldItem) {
    log.info("Handling item update {}", newItem.getId());

    var instance = fetchInstanceWithItems(newItem);
    if (!isMARCRecord(instance)) {
      return;
    }

    var oldInstance = fetchOldInstance(newItem, oldItem, instance);
    boolean itemMoved = !oldInstance.getId().equals(instance.getId());

    handlePerCentralServer(newItem.getId(), csId -> {
      if (itemMoved) {
        contributionJobRunner.runItemMove(csId, instance, oldInstance, newItem);
      } else {
        contributionJobRunner.runItemContribution(csId, instance, newItem);
      }
    });
  }


  @Override
  public void handleItemDelete(Item deletedItem, OngoingContributionStatus ongoingContributionStatus) {
    log.info("Handling item delete {}", deletedItem.getId());

    var instance = fetchInstanceWithItems(deletedItem);
    if (!isMARCRecord(instance)) {
      ongoingContributionStatusService.updateOngoingContribution(ongoingContributionStatus, MARC_ERROR_MSG, FAILED);
      return;
    }
    contributionJobRunner.runItemDeContribution(ongoingContributionStatus.getCentralServerId(), instance, deletedItem, ongoingContributionStatus);
  }


  @Override
  public void handleLoanCreation(StorageLoanDTO loan) {
    log.info("Handling loan creation {}", loan.getId());

    var item = fetchItem(loan.getItemId());
    var instance = fetchInstanceWithItems(item);
    if (!isMARCRecord(instance)) {
      return;
    }

    handlePerCentralServer(item.getId(), csId -> contributionJobRunner.runItemContribution(csId, instance, item));
  }


  @Override
  public void handleLoanUpdate(StorageLoanDTO loan) {
    log.info("Handling loan update {}", loan.getId());

    var loanAction = loan.getAction();

    if ("renewed".equalsIgnoreCase(loanAction)) {
      log.info("Triggering ongoing contribution on the renewal of loan {}", loan.getId());

      var item = fetchItem(loan.getItemId());
      var instance = fetchInstanceWithItems(item);
      if (!isMARCRecord(instance)) {
        return;
      }

      handlePerCentralServer(item.getId(), csId -> contributionJobRunner.runItemContribution(csId, instance, item));
    }
  }


  @Override
  public void handleRequestChange(RequestDTO request) {
    log.info("Handling request {}", request.getId());

    var item = fetchItem(request.getItemId());
    var instance = fetchInstanceWithItems(item);
    if (!isMARCRecord(instance)) {
      return;
    }

    handlePerCentralServer(item.getId(), csId -> contributionJobRunner.runItemContribution(csId, instance, item));
  }


  @Override
  public void handleHoldingUpdate(Holding holding) {
    log.info("Handling holding update {}", holding.getId());

    var instance = fetchInstanceWithItems(holding.getInstanceId());
    if (!isMARCRecord(instance)) {
      return;
    }

    var items = holding.getHoldingsItems();
    handlePerCentralServer(holding.getId(), csId ->
      items.forEach(i -> contributionJobRunner.runItemContribution(csId, instance, i)));
  }


  @Override
  public void handleHoldingDelete(Holding holding) {
    log.info("Handling holding delete {}", holding.getId());

    var instance = fetchInstanceWithItems(holding.getInstanceId());
    if (!isMARCRecord(instance)) {
      return;
    }

    var items = holding.getHoldingsItems();
    try {
      for (var csId : getCentralServerIds()) {
        items.forEach(item -> contributionJobRunner.runItemDeContribution(csId, instance, item));
      }
    }
    catch (ServiceSuspendedException | FeignException | InnReachConnectionException e) {
      log.info("exception thrown from handleHoldingDelete", e);
      throw e;
    }
  }

  private void handlePerCentralServer(UUID recordId, Consumer<UUID> centralServerHandler) {
    for (var csId : getCentralServerIds()) {
      try {
        if (validationService.getItemTypeMappingStatus(csId) == VALID &&
          validationService.getLocationMappingStatus(csId) == VALID) {
          centralServerHandler.accept(csId);
        } else {
          log.warn("Central server {} contribution configuration is not valid", csId);
        }
      }
      catch (ServiceSuspendedException | FeignException | InnReachConnectionException | SocketTimeOutExceptionWrapper e) {
        log.info("exception thrown from handlePerCentralServer", e);
        throw e;
      }
      catch (Exception e) {
        log.error("Unable to handle record {} for central server {}", recordId, csId, e);
      }
    }
  }

  private boolean checkCentralServerValid(UUID centralServerId) {
    return centralServerId != null
      && validationService.getItemTypeMappingStatus(centralServerId) == VALID
      && validationService.getLocationMappingStatus(centralServerId) == VALID;
  }

  private Instance fetchOldInstance(Item newItem, Item oldItem, Instance newInstance) {
    var oldHoldingId = oldItem.getHoldingsRecordId();
    var newHoldingId = newItem.getHoldingsRecordId();
    if (!oldHoldingId.equals(newHoldingId)) {
      var oldInstanceId = fetchHolding(oldHoldingId).getInstanceId();
      return instanceStorageClient.getInstanceById(oldInstanceId);
    }
    return newInstance;
  }

  private List<UUID> getCentralServerIds() {
    Page<UUID> ids = centralServerRepository.getIds(new OffsetRequest(0, 2000));
    return ids.getContent();
  }

  private Instance fetchInstanceWithItems(Item item) {
    var holding = fetchHolding(item.getHoldingsRecordId());
    return fetchInstanceWithItems(holding.getInstanceId());
  }

  private Instance fetchInstanceWithItems(UUID instanceId) {
    return inventoryViewService.getInstance(instanceId);
  }

  private Holding fetchHolding(UUID holdingId) {
    return holdingsService.find(holdingId)
      .orElseThrow(() -> new IllegalArgumentException("Holding record is not found by id: " + holdingId));
  }

  private Item fetchItem(UUID itemId) {
    return itemStorageClient.getItemById(itemId)
      .orElseThrow(() -> new IllegalArgumentException("Item is not found by id: " + itemId));
  }

}
