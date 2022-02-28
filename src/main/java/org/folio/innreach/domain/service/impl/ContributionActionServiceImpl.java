package org.folio.innreach.domain.service.impl;

import static org.folio.innreach.domain.service.impl.MARCRecordTransformationServiceImpl.isMARCRecord;
import static org.folio.innreach.dto.MappingValidationStatusDTO.VALID;

import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.domain.Page;
import org.springframework.scheduling.annotation.Async;
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

  @Async
  @Override
  public void handleInstanceCreation(Instance newInstance) {
    handleInstanceUpdate(newInstance);
  }

  @Async
  @Override
  public void handleInstanceUpdate(Instance updatedInstance) {
    if (!isMARCRecord(updatedInstance)) {
      return;
    }

    var instance = fetchInstanceWithItems(updatedInstance.getId());

    handleRecordPerCentralServer(instance.getId(), csId -> contributionJobRunner.runInstanceContribution(csId, instance));
  }

  @Async
  @Override
  public void handleInstanceDelete(Instance deletedInstance) {
    if (!isMARCRecord(deletedInstance)) {
      return;
    }

    for (var csId : getCentralServerIds()) {
      contributionJobRunner.runInstanceDeContribution(csId, deletedInstance);
    }
  }

  @Async
  @Override
  public void handleItemCreation(Item newItem) {
    var instance = fetchInstanceWithItems(newItem);
    if (!isMARCRecord(instance)) {
      return;
    }

    handleRecordPerCentralServer(newItem.getId(), csId -> contributionJobRunner.runItemContribution(csId, instance, newItem));
  }

  @Async
  @Override
  public void handleItemUpdate(Item newItem, Item oldItem) {
    var instance = fetchInstanceWithItems(newItem);
    if (!isMARCRecord(instance)) {
      return;
    }

    var oldInstance = fetchOldInstance(newItem, oldItem, instance);
    boolean itemMoved = !oldInstance.getId().equals(instance.getId());

    handleRecordPerCentralServer(newItem.getId(), csId -> {
      if (itemMoved) {
        contributionJobRunner.runItemMove(csId, instance, oldInstance, newItem);
      } else {
        contributionJobRunner.runItemContribution(csId, instance, newItem);
      }
    });
  }

  @Async
  @Override
  public void handleItemDelete(Item deletedItem) {
    var instance = fetchInstanceWithItems(deletedItem);
    if (!isMARCRecord(instance)) {
      return;
    }

    for (var csId : getCentralServerIds()) {
      contributionJobRunner.runItemDeContribution(csId, instance, deletedItem);
    }
  }

  @Async
  @Override
  public void handleLoanCreation(StorageLoanDTO loan) {
    log.info("Handling loan creation {}", loan.getId());

    var item = fetchItem(loan.getItemId());
    var instance = fetchInstanceWithItems(item);
    if (!isMARCRecord(instance)) {
      return;
    }

    handleRecordPerCentralServer(item.getId(), csId -> contributionJobRunner.runItemContribution(csId, instance, item));
  }

  @Async
  @Override
  public void handleLoanUpdate(StorageLoanDTO loan) {
    var loanAction = loan.getAction();

    if ("renewed".equalsIgnoreCase(loanAction)) {
      log.info("Triggering ongoing contribution on the renewal of loan {}", loan.getId());

      var item = fetchItem(loan.getItemId());
      var instance = fetchInstanceWithItems(item);
      if (!isMARCRecord(instance)) {
        return;
      }

      handleRecordPerCentralServer(item.getId(), csId -> contributionJobRunner.runItemContribution(csId, instance, item));
    }
  }

  @Async
  @Override
  public void handleRequestChange(RequestDTO request) {
    log.info("Triggering ongoing contribution on the request changes {}", request.getId());

    var item = fetchItem(request.getItemId());
    var instance = fetchInstanceWithItems(item);
    if (!isMARCRecord(instance)) {
      return;
    }

    handleRecordPerCentralServer(item.getId(), csId -> contributionJobRunner.runItemContribution(csId, instance, item));
  }

  @Async
  @Override
  public void handleHoldingUpdate(Holding holding) {
    var instance = fetchInstanceWithItems(holding.getInstanceId());
    if (!isMARCRecord(instance)) {
      return;
    }

    var items = holding.getHoldingsItems();
    handleRecordPerCentralServer(holding.getId(), csId ->
      items.forEach(i -> contributionJobRunner.runItemContribution(csId, instance, i)));
  }

  @Async
  @Override
  public void handleHoldingDelete(Holding holding) {
    var instance = fetchInstanceWithItems(holding.getInstanceId());
    if (!isMARCRecord(instance)) {
      return;
    }

    var items = holding.getHoldingsItems();
    for (var csId : getCentralServerIds()) {
      items.forEach(item -> contributionJobRunner.runItemDeContribution(csId, instance, item));
    }
  }

  private void handleRecordPerCentralServer(UUID recordId, Consumer<UUID> centralServerHandler) {
    for (var csId : getCentralServerIds()) {
      try {
        if (validationService.getItemTypeMappingStatus(csId) == VALID &&
          validationService.getLocationMappingStatus(csId) == VALID) {
          centralServerHandler.accept(csId);
        } else {
          log.warn("Central server {} contribution configuration is not valid", csId);
        }
      } catch (Exception e) {
        log.error("Unable to handle record {} for central server {}", recordId, csId, e);
      }
    }
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
