package org.folio.innreach.domain.service.impl;

import java.util.List;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import org.folio.innreach.client.HridSettingsClient;
import org.folio.innreach.domain.dto.folio.inventory.InventoryInstanceDTO;
import org.folio.innreach.domain.dto.folio.inventory.InventoryInstanceDTO.ContributorDTO;
import org.folio.innreach.domain.dto.folio.inventory.InventoryItemDTO;
import org.folio.innreach.domain.dto.folio.inventory.InventoryItemDTO.MaterialType;
import org.folio.innreach.domain.dto.folio.inventory.InventoryItemDTO.PermanentLoanType;
import org.folio.innreach.domain.dto.folio.inventory.InventoryItemStatus;
import org.folio.innreach.domain.entity.InnReachTransaction;
import org.folio.innreach.domain.entity.TransactionPatronHold;
import org.folio.innreach.domain.service.AgencyMappingService;
import org.folio.innreach.domain.service.CentralServerService;
import org.folio.innreach.domain.service.InventoryService;
import org.folio.innreach.domain.service.ItemTypeMappingService;
import org.folio.innreach.domain.service.PatronHoldService;
import org.folio.innreach.domain.service.RequestService;
import org.folio.innreach.dto.CentralServerDTO;
import org.folio.innreach.dto.Holding;
import org.folio.innreach.util.UUIDHelper;

@Log4j2
@Service
@RequiredArgsConstructor
public class PatronHoldServiceImpl implements PatronHoldService {

  public static final String INN_REACH_AUTHOR = "INN-Reach author";
  public static final String INN_REACH_TEMPORARY_RECORD = "INN-Reach temporary record";
  public static final String RECORD_SOURCE = "INN-Reach";

  private final AgencyMappingService agencyMappingService;
  private final CentralServerService centralServerService;
  private final ItemTypeMappingService itemTypeMappingService;
  private final RequestService requestService;
  private final InventoryService inventoryService;

  @Async
  @Override
  public void createVirtualItems(InnReachTransaction transaction) {
    log.info("Creating inventory virtual items for transaction {}", transaction);

    var hold = (TransactionPatronHold) transaction.getHold();
    var hridSettings = inventoryService.getHridSettings();
    var centralServer = centralServerService.getCentralServerByCentralCode(transaction.getCentralServerCode());
    var locationCode = hold.getPickupLocation().getPickupLocCode();
    var servicePointId = getServicePointIdByCode(locationCode);
    var patronId = UUIDHelper.fromStringWithoutHyphens(hold.getPatronId());

    var instance = prepareInstance(transaction, hold, hridSettings);
    var holding = prepareHolding(centralServer.getId(), transaction, hold, hridSettings);
    var item = prepareItem(centralServer, transaction, hold, hridSettings);

    instance = inventoryService.createInstance(instance);

    createHoldingAndItem(instance, holding, item, hold);

    requestService.createItemRequest(transaction, centralServer.getId(), servicePointId, patronId);
  }

  @Async
  @Override
  public void updateVirtualItems(InnReachTransaction transaction) {
    log.info("Updating inventory virtual items for transaction {}", transaction);

    var hold = (TransactionPatronHold) transaction.getHold();
    var hridSettings = inventoryService.getHridSettings();
    var centralServer = centralServerService.getCentralServerByCentralCode(transaction.getCentralServerCode());

    var instance = fetchInstance(transaction, hridSettings);
    var holding = prepareHolding(centralServer.getId(), transaction, hold, hridSettings);
    var item = prepareItem(centralServer, transaction, hold, hridSettings);

    createHoldingAndItem(instance, holding, item, hold);

    requestService.moveItemRequest(transaction);
  }

  private InventoryInstanceDTO prepareInstance(InnReachTransaction transaction, TransactionPatronHold hold, HridSettingsClient.HridSettings hridSettings) {
    var instanceHrid = getInstanceHrid(transaction, hridSettings);
    var instanceTypeId = getInstanceTypeId();
    var author = getInstanceContributor();

    return InventoryInstanceDTO.builder()
      .hrid(instanceHrid)
      .version("1")
      .instanceTypeId(instanceTypeId)
      .title(hold.getTitle())
      .contributors(List.of(author))
      .source(RECORD_SOURCE)
      .staffSuppress(true)
      .discoverySuppress(true)
      .build();
  }

  private String getInstanceHrid(InnReachTransaction transaction, HridSettingsClient.HridSettings hridSettings) {
    return hridSettings.getInstances().getPrefix()
      + transaction.getTrackingId()
      + transaction.getCentralServerCode();
  }

  private InventoryInstanceDTO fetchInstance(InnReachTransaction transaction, HridSettingsClient.HridSettings hridSettings) {
    var instanceHrid = getInstanceHrid(transaction, hridSettings);

    return inventoryService.queryInstanceByHrid(instanceHrid);
  }

  private void createHoldingAndItem(InventoryInstanceDTO instance, Holding holding, InventoryItemDTO item, TransactionPatronHold hold) {
    holding.setInstanceId(instance.getId());
    holding = inventoryService.createHolding(holding);

    item.setHoldingsRecordId(holding.getId());
    inventoryService.createItem(item);

    hold.setItemId(item.getHrid());
  }

  private Holding prepareHolding(UUID centralServerId, InnReachTransaction transaction, TransactionPatronHold hold, HridSettingsClient.HridSettings settings) {
    var itemAgencyCode = hold.getItemAgencyCode();
    var holdingHrid = settings.getHoldings().getPrefix()
      + transaction.getTrackingId()
      + hold.getItemAgencyCode();

    var locationId = agencyMappingService.getLocationIdByAgencyCode(centralServerId, itemAgencyCode);

    return new Holding()
      .hrid(holdingHrid)
      .callNumber(hold.getCallNumber())
      .discoverySuppress(true)
      .permanentLocationId(locationId);
  }

  private InventoryItemDTO prepareItem(CentralServerDTO centralServer, InnReachTransaction transaction, TransactionPatronHold hold, HridSettingsClient.HridSettings settings) {
    var centralItemType = hold.getCentralItemType();
    var centralServerId = centralServer.getId();
    var itemHrid = settings.getItems().getPrefix()
      + transaction.getTrackingId()
      + hold.getItemAgencyCode();

    var materialTypeId =
      itemTypeMappingService.getMappingByCentralType(centralServerId, centralItemType)
        .getMaterialTypeId();

    return InventoryItemDTO.builder()
      .hrid(itemHrid)
      .version("1")
      .discoverySuppress(true)
      .materialType(new MaterialType(materialTypeId, null))
      .permanentLoanType(new PermanentLoanType(centralServer.getLoanTypeId(), null))
      .status(InventoryItemStatus.AVAILABLE)
      .build();
  }

  private UUID getServicePointIdByCode(String locationCode) {
    return inventoryService.queryServicePointByCode(locationCode).getId();
  }

  private UUID getInstanceTypeId() {
    var instanceTypes = inventoryService.queryInstanceTypeByName(INN_REACH_TEMPORARY_RECORD);

    return instanceTypes.getId();
  }

  private ContributorDTO getInstanceContributor() {
    var author = inventoryService.queryContributorTypeByName(INN_REACH_AUTHOR);

    return new ContributorDTO(author.getId(), author.getName());
  }

}
