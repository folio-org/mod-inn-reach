package org.folio.innreach.domain.service.impl;

import static org.folio.innreach.domain.entity.ContributionStatus.FAILED;
import static org.folio.innreach.domain.entity.ContributionStatus.PROCESSED;
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
import org.folio.innreach.domain.event.DomainEventType;
import org.folio.innreach.external.exception.InnReachConnectionException;
import org.folio.innreach.external.exception.ServiceSuspendedException;
import org.folio.innreach.external.exception.SocketTimeOutExceptionWrapper;
import org.folio.innreach.util.JsonHelper;
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
  private final JsonHelper jsonHelper;

  @Override
  public void handleInstanceCreation(Instance newInstance, OngoingContributionStatus ongoingContributionStatus) {
    handleInstanceUpdate(newInstance, ongoingContributionStatus);
  }

  @Override
  public void handleInstanceUpdate(Instance updatedInstance, OngoingContributionStatus ongoingContributionStatus) {
    log.info("Handling instance creation/update {}", updatedInstance.getId());

    if (!isMARCRecord(updatedInstance)) {
      ongoingContributionStatusService.updateOngoingContribution(ongoingContributionStatus, MARC_ERROR_MSG, FAILED);
      return;
    }

    var instance = fetchInstanceWithItems(updatedInstance.getId());

    var centralServerId = ongoingContributionStatus.getCentralServerId();
    if (checkCentralServerValid(centralServerId)) {
      contributionJobRunner.runOngoingInstanceContribution(centralServerId, instance, ongoingContributionStatus);
    } else {
      ongoingContributionStatusService.updateOngoingContribution(ongoingContributionStatus, INVALID_CENTRAL_SERVER_ID, FAILED);
    }
  }

  @Override
  public void handleInstanceDelete(Instance deletedInstance, OngoingContributionStatus ongoingContributionStatus) {
    log.info("handleInstanceDelete:: Handling instance delete {}", deletedInstance.getId());
    if (!isMARCRecord(deletedInstance)) {
      ongoingContributionStatusService.updateOngoingContribution(ongoingContributionStatus, MARC_ERROR_MSG, FAILED);
      return;
    }
    contributionJobRunner.runOngoingInstanceDeContribution(ongoingContributionStatus.getCentralServerId(), deletedInstance, ongoingContributionStatus);
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
    if (checkCentralServerValid(centralServerId)) {
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
    if (checkCentralServerValid(centralServerId)) {
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
  public void handleHoldingUpdate(Holding holding, OngoingContributionStatus ongoingContributionStatus) {
    log.info("Handling holding update {}", holding.getId());
    var instance = fetchInstanceWithItems(holding.getInstanceId());
    if (!isMARCRecord(instance)) {
      ongoingContributionStatusService.updateOngoingContribution(ongoingContributionStatus, MARC_ERROR_MSG, FAILED);
      return;
    }
    // Filter out the list of item associated with the updated holdings
    var items = instance.getItems()
      .stream()
      .filter(item -> item.getHoldingsRecordId().equals(holding.getId()))
      .toList();
    var centralServerId = ongoingContributionStatus.getCentralServerId();
    if (checkCentralServerValid(centralServerId)) {
      items.forEach(item -> {
        var newItemJob = createNewOngoingContributionStatus(ongoingContributionStatus, item);
        newItemJob.setDomainEventType(DomainEventType.UPDATED);
        contributionJobRunner.runItemContribution(centralServerId, instance, item, newItemJob);
      });
      ongoingContributionStatusService.updateOngoingContribution(ongoingContributionStatus, PROCESSED);
    } else {
      ongoingContributionStatusService.updateOngoingContribution(ongoingContributionStatus, INVALID_CENTRAL_SERVER_ID, FAILED);
    }
  }

  private OngoingContributionStatus createNewOngoingContributionStatus(OngoingContributionStatus holdingJob, Item item) {
    var ongoingContribution = new OngoingContributionStatus();
    ongoingContribution.setParentId(holdingJob.getId());
    ongoingContribution.setTenant(holdingJob.getTenant());
    ongoingContribution.setDomainEventName(OngoingContributionStatus.EventName.ITEM);
    ongoingContribution.setCentralServerId(holdingJob.getCentralServerId());
    ongoingContribution.setOldEntity(jsonHelper.toJson(item));
    return ongoingContribution;
  }

  @Override
  public void handleHoldingDelete(Holding holding, OngoingContributionStatus ongoingContributionStatus) {
    log.info("Handling holding delete {}", holding.getId());

    var instance = fetchInstanceWithItems(holding.getInstanceId());
    if (!isMARCRecord(instance)) {
      ongoingContributionStatusService.updateOngoingContribution(ongoingContributionStatus, MARC_ERROR_MSG, FAILED);
      return;
    }

    var centralServerId = ongoingContributionStatus.getCentralServerId();
    // Filter out the list of item associated with the deleted holdings
    var items = instance.getItems()
      .stream()
      .filter(item -> item.getHoldingsRecordId().equals(holding.getId()))
      .toList();
    items.forEach(item -> {
      var newItemJob = createNewOngoingContributionStatus(ongoingContributionStatus, item);
      newItemJob.setDomainEventType(DomainEventType.DELETED);
      contributionJobRunner.runItemDeContribution(centralServerId, instance, item, newItemJob);
    });
    ongoingContributionStatusService.updateOngoingContribution(ongoingContributionStatus, PROCESSED);
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
