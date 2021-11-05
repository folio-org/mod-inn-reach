package org.folio.innreach.domain.service.impl;

import static org.folio.innreach.dto.ItemStatus.NameEnum.AVAILABLE;

import java.util.UUID;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import org.folio.innreach.client.HridSettingsClient;
import org.folio.innreach.domain.entity.InnReachTransaction;
import org.folio.innreach.domain.entity.TransactionPatronHold;
import org.folio.innreach.domain.service.AgencyMappingService;
import org.folio.innreach.domain.service.CentralServerService;
import org.folio.innreach.domain.service.InventoryStorageService;
import org.folio.innreach.domain.service.ItemTypeMappingService;
import org.folio.innreach.domain.service.MaterialTypeMappingService;
import org.folio.innreach.domain.service.PatronHoldService;
import org.folio.innreach.domain.service.RequestService;
import org.folio.innreach.dto.CentralServerDTO;
import org.folio.innreach.dto.Holding;
import org.folio.innreach.dto.Instance;
import org.folio.innreach.dto.InstanceContributors;
import org.folio.innreach.dto.Item;
import org.folio.innreach.dto.ItemStatus;
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
  private final InventoryStorageService inventoryStorageService;

  @Async
  @Override
  public void createVirtualItems(InnReachTransaction transaction) {
    log.info("Creating inventory virtual items for transaction {}", transaction);

    var hold = (TransactionPatronHold) transaction.getHold();
    var hridSettings = inventoryStorageService.getHridSettings();
    var centralServer = centralServerService.getCentralServerByCentralCode(transaction.getCentralServerCode());
    var locationCode = hold.getPickupLocation().getPickupLocCode();
    var servicePointId = getServicePointIdByCode(locationCode);
    var patronId = UUIDHelper.fromStringWithoutHyphens(hold.getPatronId());

    var instance = prepareInstance(transaction, hold, hridSettings);
    var holding = prepareHolding(centralServer.getId(), transaction, hold, hridSettings);
    var item = prepareItem(centralServer, transaction, hold, hridSettings);

    instance = inventoryStorageService.createInstance(instance);

    createHoldingAndItem(instance, holding, item, hold);

    requestService.createItemRequest(transaction, centralServer.getId(), servicePointId, patronId);
  }

  @Async
  @Override
  public void updateVirtualItems(InnReachTransaction transaction) {
    log.info("Updating inventory virtual items for transaction {}", transaction);

    var hold = (TransactionPatronHold) transaction.getHold();
    var hridSettings = inventoryStorageService.getHridSettings();
    var centralServer = centralServerService.getCentralServerByCentralCode(transaction.getCentralServerCode());

    var instance = fetchInstance(transaction, hridSettings);
    var holding = prepareHolding(centralServer.getId(), transaction, hold, hridSettings);
    var item = prepareItem(centralServer, transaction, hold, hridSettings);

    createHoldingAndItem(instance, holding, item, hold);

    requestService.moveItemRequest(transaction);
  }

  private Instance prepareInstance(InnReachTransaction transaction, TransactionPatronHold hold, HridSettingsClient.HridSettings hridSettings) {
    var instanceHrid = getInstanceHrid(transaction, hridSettings);
    var instanceTypeId = getInstanceTypeId();
    var author = getInstanceContributor();

    return new Instance()
      .hrid(instanceHrid)
      .instanceTypeId(instanceTypeId)
      .title(hold.getTitle())
      .addContributorsItem(author)
      .source(RECORD_SOURCE)
      .staffSuppress(true)
      .discoverySuppress(true);
  }

  private String getInstanceHrid(InnReachTransaction transaction, HridSettingsClient.HridSettings hridSettings) {
    return hridSettings.getInstances().getPrefix()
      + transaction.getTrackingId()
      + transaction.getCentralServerCode();
  }

  private Instance fetchInstance(InnReachTransaction transaction, HridSettingsClient.HridSettings hridSettings) {
    var instanceHrid = getInstanceHrid(transaction, hridSettings);

    return inventoryStorageService.queryInstanceByHrid(instanceHrid);
  }

  private void createHoldingAndItem(Instance instance, Holding holding, Item item, TransactionPatronHold hold) {
    holding.setInstanceId(instance.getId());
    holding = inventoryStorageService.createHolding(holding);

    item.setHoldingsRecordId(holding.getId());
    inventoryStorageService.createItem(item);

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

  private Item prepareItem(CentralServerDTO centralServer, InnReachTransaction transaction, TransactionPatronHold hold, HridSettingsClient.HridSettings settings) {
    var centralItemType = hold.getCentralItemType();
    var centralServerId = centralServer.getId();
    var itemHrid = settings.getItems().getPrefix()
      + transaction.getTrackingId()
      + hold.getItemAgencyCode();

    var materialTypeId =
      itemTypeMappingService.getMappingByCentralType(centralServerId, centralItemType)
        .getMaterialTypeId();

    return new Item()
      .hrid(itemHrid)
      .discoverySuppress(true)
      .materialTypeId(materialTypeId)
      .permanentLoanTypeId(centralServer.getLoanTypeId())
      .status(new ItemStatus().name(AVAILABLE));
  }

  private UUID getServicePointIdByCode(String locationCode) {
    return inventoryStorageService.queryServicePointByCode(locationCode).getId();
  }

  private UUID getInstanceTypeId() {
    var instanceTypes = inventoryStorageService.queryInstanceTypeByName(INN_REACH_TEMPORARY_RECORD);

    return instanceTypes.getId();
  }

  private InstanceContributors getInstanceContributor() {
    var author = inventoryStorageService.queryContributorTypeByName(INN_REACH_AUTHOR);

    return new InstanceContributors()
      .name(author.getName())
      .contributorNameTypeId(author.getId());
  }

}
