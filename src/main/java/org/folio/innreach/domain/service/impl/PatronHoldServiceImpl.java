package org.folio.innreach.domain.service.impl;

import static org.folio.innreach.dto.ItemStatus.NameEnum.AVAILABLE;

import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import org.folio.innreach.client.HridSettingsClient;
import org.folio.innreach.domain.entity.InnReachTransaction;
import org.folio.innreach.domain.entity.TransactionPatronHold;
import org.folio.innreach.domain.service.AgencyMappingService;
import org.folio.innreach.domain.service.CentralServerService;
import org.folio.innreach.domain.service.InventoryStorageService;
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

@Service
@RequiredArgsConstructor
public class PatronHoldServiceImpl implements PatronHoldService {

  public static final String INN_REACH_AUTHOR = "INN-Reach author";
  public static final String INN_REACH_TEMPORARY_RECORD = "INN-Reach temporary record";
  public static final String RECORD_SOURCE = "INN-Reach";

  private final AgencyMappingService agencyMappingService;
  private final CentralServerService centralServerService;
  private final MaterialTypeMappingService materialTypeMappingService;
  private final RequestService requestService;
  private final InventoryStorageService inventoryStorageService;

  @Async
  @Override
  public void createVirtualItems(InnReachTransaction transaction) {
    var hold = (TransactionPatronHold) transaction.getHold();
    var hridSettings = inventoryStorageService.getHridSettings();
    var centralServer = centralServerService.getCentralServerByCentralCode(transaction.getCentralServerCode());

    var instance = createInstance(transaction, hold, hridSettings);

    createHoldingItem(transaction, hold, hridSettings, centralServer, instance);

    var patronId = UUIDHelper.fromStringWithoutHyphens(hold.getPatronId());
    var locationCode = hold.getPickupLocation().getPickupLocCode();
    var servicePointId = getServicePointIdByCode(locationCode);

    requestService.createItemRequest(transaction, centralServer.getId(), servicePointId, patronId);
  }

  @Async
  @Override
  public void updateVirtualItems(InnReachTransaction transaction) {
    var hold = (TransactionPatronHold) transaction.getHold();
    var hridSettings = inventoryStorageService.getHridSettings();
    var centralServer = centralServerService.getCentralServerByCentralCode(transaction.getCentralServerCode());

    var instanceHrid = getInstanceHrid(transaction, hridSettings);

    var instance = fetchInstance(instanceHrid);

    createHoldingItem(transaction, hold, hridSettings, centralServer, instance);

    requestService.moveItemRequest(transaction);
  }

  private Instance createInstance(InnReachTransaction transaction, TransactionPatronHold hold, HridSettingsClient.HridSettings hridSettings) {
    var instanceHrid = getInstanceHrid(transaction, hridSettings);

    var instance = new Instance()
      .hrid(instanceHrid)
      .instanceTypeId(getInstanceTypeId())
      .title(hold.getTitle())
      .addContributorsItem(getInstanceContributor())
      .source(RECORD_SOURCE)
      .staffSuppress(true)
      .discoverySuppress(true);

    return inventoryStorageService.createInstance(instance);
  }

  private String getInstanceHrid(InnReachTransaction transaction, HridSettingsClient.HridSettings hridSettings) {
    return hridSettings.getInstances().getPrefix() + transaction.getTrackingId() + transaction.getCentralServerCode();
  }

  private Instance fetchInstance(String instanceHrid) {
    return inventoryStorageService.queryInstanceByHrid(instanceHrid);
  }

  private void createHoldingItem(InnReachTransaction transaction,
                                 TransactionPatronHold hold,
                                 HridSettingsClient.HridSettings hridSettings,
                                 CentralServerDTO centralServer,
                                 Instance instance) {

    var holding = prepareHolding(centralServer.getId(), transaction, hold, hridSettings);
    holding.setInstanceId(instance.getId());
    holding = inventoryStorageService.createHolding(holding);

    var item = prepareItem(centralServer, transaction, hold, hridSettings);
    item.setHoldingsRecordId(holding.getId());
    inventoryStorageService.createItem(item);
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
      materialTypeMappingService.getMappingByCentralType(centralServerId, centralItemType).getMaterialTypeId();

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
