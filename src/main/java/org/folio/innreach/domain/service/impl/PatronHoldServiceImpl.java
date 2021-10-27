package org.folio.innreach.domain.service.impl;

import static org.folio.innreach.dto.ItemStatus.NameEnum.AVAILABLE;

import java.util.UUID;
import java.util.function.Function;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import org.folio.innreach.client.HoldingsStorageClient;
import org.folio.innreach.client.HridSettingsClient;
import org.folio.innreach.client.InstanceContributorTypeClient;
import org.folio.innreach.client.InstanceTypeClient;
import org.folio.innreach.client.InventoryClient;
import org.folio.innreach.client.ServicePointsClient;
import org.folio.innreach.domain.entity.InnReachTransaction;
import org.folio.innreach.domain.entity.TransactionPatronHold;
import org.folio.innreach.domain.service.AgencyMappingService;
import org.folio.innreach.domain.service.CentralServerService;
import org.folio.innreach.domain.service.MaterialTypeMappingService;
import org.folio.innreach.domain.service.PatronHoldService;
import org.folio.innreach.domain.service.RequestService;
import org.folio.innreach.dto.CentralServerDTO;
import org.folio.innreach.dto.Holding;
import org.folio.innreach.dto.Instance;
import org.folio.innreach.dto.InstanceContributors;
import org.folio.innreach.dto.Item;
import org.folio.innreach.dto.ItemStatus;
import org.folio.innreach.util.ListUtils;
import org.folio.innreach.util.UUIDHelper;

@Service
@RequiredArgsConstructor
public class PatronHoldServiceImpl implements PatronHoldService {

  public static final String INN_REACH_AUTHOR = "INN-Reach author";
  public static final String INN_REACH_TEMPORARY_RECORD = "INN-Reach temporary record";

  private final HridSettingsClient hridSettingsClient;
  private final InstanceTypeClient instanceTypeClient;
  private final InstanceContributorTypeClient nameTypeClient;
  private final AgencyMappingService agencyMappingService;
  private final CentralServerService centralServerService;
  private final MaterialTypeMappingService materialTypeMappingService;
  private final RequestService requestService;
  private final InventoryClient inventoryClient;
  private final HoldingsStorageClient holdingsStorageClient;
  private final ServicePointsClient servicePointsClient;

  @Async
  @Override
  public void createVirtualItems(InnReachTransaction transaction) {
    var hold = (TransactionPatronHold) transaction.getHold();
    var hridSettings = hridSettingsClient.getHridSettings();
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
    var hridSettings = hridSettingsClient.getHridSettings();
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
      .staffSuppress(true)
      .discoverySuppress(true);

    return inventoryClient.createInstance(instance);
  }

  private String getInstanceHrid(InnReachTransaction transaction, HridSettingsClient.HridSettings hridSettings) {
    return hridSettings.getInstances().getPrefix() + transaction.getTrackingId() + transaction.getCentralServerCode();
  }

  private Instance fetchInstance(String instanceHrid) {
    return inventoryClient.queryInstanceByHrid(instanceHrid).getResult().stream()
      .findFirst()
      .orElseThrow(() -> new IllegalArgumentException("No instance found by hrid " + instanceHrid));
  }

  private void createHoldingItem(InnReachTransaction transaction,
                                 TransactionPatronHold hold,
                                 HridSettingsClient.HridSettings hridSettings,
                                 CentralServerDTO centralServer,
                                 Instance instance) {

    var holding = prepareHolding(centralServer.getId(), transaction, hold, hridSettings);
    holding.setInstanceId(instance.getId());
    holding = holdingsStorageClient.createHolding(holding);

    var item = prepareItem(centralServer, transaction, hold, hridSettings);
    item.setHoldingsRecordId(holding.getId());
    inventoryClient.createItem(item);
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
    var servicePoints = servicePointsClient.queryServicePointByCode(locationCode);

    return ListUtils.mapFirstItem(servicePoints, ServicePointsClient.ServicePoint::getId)
      .orElseThrow(() -> new IllegalStateException("Service point is not found for pickup location code: " + locationCode));
  }

  private UUID getInstanceTypeId() {
    var instanceTypes = instanceTypeClient.queryInstanceTypeByName(INN_REACH_TEMPORARY_RECORD);

    return ListUtils.mapFirstItem(instanceTypes, InstanceTypeClient.InstanceType::getId)
      .orElseThrow(() -> new IllegalStateException("Instance type is not found by name: " + INN_REACH_TEMPORARY_RECORD));
  }

  private InstanceContributors getInstanceContributor() {
    var author = ListUtils.mapFirstItem(nameTypeClient.queryContributorType(INN_REACH_AUTHOR), Function.identity())
      .orElseThrow(() -> new IllegalStateException("Contributor name type is not found by name: " + INN_REACH_AUTHOR));

    return new InstanceContributors()
      .name(author.getName())
      .contributorNameTypeId(author.getId());
  }

}
