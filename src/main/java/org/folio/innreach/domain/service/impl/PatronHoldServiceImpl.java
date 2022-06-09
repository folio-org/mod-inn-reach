package org.folio.innreach.domain.service.impl;

import static java.util.Objects.nonNull;

import java.util.List;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import org.folio.innreach.client.HridSettingsClient;
import org.folio.innreach.domain.dto.folio.User;
import org.folio.innreach.domain.dto.folio.circulation.RequestDTO.RequestType;
import org.folio.innreach.domain.dto.folio.inventory.InventoryInstanceDTO;
import org.folio.innreach.domain.dto.folio.inventory.InventoryInstanceDTO.ContributorDTO;
import org.folio.innreach.domain.dto.folio.inventory.InventoryItemDTO;
import org.folio.innreach.domain.dto.folio.inventory.InventoryItemDTO.MaterialType;
import org.folio.innreach.domain.dto.folio.inventory.InventoryItemDTO.LoanType;
import org.folio.innreach.domain.dto.folio.inventory.InventoryItemStatus;
import org.folio.innreach.domain.entity.InnReachTransaction;
import org.folio.innreach.domain.entity.TransactionHold;
import org.folio.innreach.domain.entity.TransactionPatronHold;
import org.folio.innreach.domain.service.AgencyMappingService;
import org.folio.innreach.domain.service.CentralServerService;
import org.folio.innreach.domain.service.HoldingsService;
import org.folio.innreach.domain.service.InstanceService;
import org.folio.innreach.domain.service.InventoryService;
import org.folio.innreach.domain.service.ItemService;
import org.folio.innreach.domain.service.ItemTypeMappingService;
import org.folio.innreach.domain.service.PatronHoldService;
import org.folio.innreach.domain.service.RequestService;
import org.folio.innreach.domain.service.UpdateTemplate;
import org.folio.innreach.domain.service.UserService;
import org.folio.innreach.dto.CentralServerDTO;
import org.folio.innreach.dto.Holding;
import org.folio.innreach.dto.HoldingSourceDTO;
import org.folio.innreach.repository.InnReachTransactionRepository;
import org.folio.innreach.util.UUIDEncoder;

@Log4j2
@Service
@RequiredArgsConstructor
public class PatronHoldServiceImpl implements PatronHoldService {

  public static final String INN_REACH_AUTHOR = "INN-Reach author";
  public static final String INN_REACH_TEMPORARY_RECORD = "INN-Reach temporary record";
  public static final String RECORD_SOURCE = "INN-Reach";
  public static final String HOLDING_SOURCE = "FOLIO";

  private final AgencyMappingService agencyMappingService;
  private final CentralServerService centralServerService;
  private final ItemTypeMappingService itemTypeMappingService;
  private final RequestService requestService;
  private final InventoryService inventoryService;
  private final InstanceService instanceService;
  private final ItemService itemService;
  private final HoldingsService holdingsService;
  private final UserService userService;
  private final InnReachTransactionRepository repository;

  @Async
  @Override
  public void createVirtualItems(InnReachTransaction transaction) {
    log.info("Creating inventory virtual items for transaction {}", transaction);

    var hold = (TransactionPatronHold) transaction.getHold();
    var hridSettings = inventoryService.getHridSettings();
    var centralServer = centralServerService.getCentralServerByCentralCode(transaction.getCentralServerCode());
    var locationCode = hold.getPickupLocation().getPickupLocCode();
    var servicePointId = requestService.getServicePointIdByCode(locationCode);
    var patron = getPatron(hold);

    var instance = prepareInstance(transaction, hold, hridSettings);
    var holding = prepareHolding(centralServer.getId(), transaction, hold, hridSettings);
    var item = prepareItem(centralServer, transaction, hold, hridSettings);

    instance = instanceService.create(instance);

    holding = createHolding(instance, holding);
    item = createItem(holding, item);

    requestService.createItemRequest(transaction, holding, item, patron, servicePointId, RequestType.PAGE);
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

    holding = createHolding(instance, holding);
    item = createItem(holding, item);

    var request = requestService.moveItemRequest(hold.getFolioRequestId(), item);

    hold.setFolioItemId(request.getItemId());
    hold.setFolioInstanceId(request.getInstanceId());
    hold.setFolioHoldingId(request.getHoldingsRecordId());
    hold.setFolioItemBarcode(item.getBarcode());

    repository.save(transaction);
  }

  @Override
  public void addItemBarcode(InnReachTransaction transaction, String itemBarcode) {
    addItemBarcodeAndCallNumber(transaction, itemBarcode, null);
  }

  @Override
  public void addItemBarcodeAndCallNumber(InnReachTransaction transaction, String itemBarcode, String callNumber) {
    var hold = (TransactionPatronHold) transaction.getHold();
    var folioItemBarcode = itemBarcode;

    var itemByBarcode = itemService.findItemByBarcode(itemBarcode);
    if (itemByBarcode.isPresent()) {
      log.info("Item barcode {} already exists, appending agency code", itemBarcode);
      folioItemBarcode += hold.getItemAgencyCode();
    }

    hold.setShippedItemBarcode(itemBarcode);
    hold.setFolioItemBarcode(folioItemBarcode);

    if (nonNull(callNumber)) {
      hold.setCallNumber(callNumber);
    }

    var folioItemId = hold.getFolioItemId();
    itemService.changeAndUpdate(folioItemId,
      () -> new IllegalArgumentException("Item with id = " + folioItemId + " not found!"),
      changeFolioAssociatedItem(folioItemBarcode, callNumber));
  }

  private User getPatron(TransactionHold hold) {
    var patronId = UUIDEncoder.decode(hold.getPatronId());

    return userService.getUserById(patronId)
      .orElseThrow(() -> new IllegalArgumentException("Patron is not found by id: " + patronId));
  }

  private InventoryInstanceDTO prepareInstance(InnReachTransaction transaction, TransactionPatronHold hold, HridSettingsClient.HridSettings hridSettings) {
    var instanceHrid = getInstanceHrid(transaction, hridSettings);
    var instanceTypeId = getInstanceTypeId();
    var author = getInstanceContributor();

    return InventoryInstanceDTO.builder()
      .hrid(instanceHrid)
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

    return instanceService.queryInstanceByHrid(instanceHrid);
  }

  private Holding createHolding(InventoryInstanceDTO instance, Holding holding) {
    holding.setInstanceId(instance.getId());
    return holdingsService.create(holding);
  }

  private InventoryItemDTO createItem(Holding holding, InventoryItemDTO item) {
    item.setHoldingsRecordId(holding.getId());
    return itemService.create(item);
  }

  private Holding prepareHolding(UUID centralServerId, InnReachTransaction transaction, TransactionPatronHold hold, HridSettingsClient.HridSettings settings) {
    var itemAgencyCode = hold.getItemAgencyCode();
    var holdingHrid = settings.getHoldings().getPrefix()
      + transaction.getTrackingId()
      + hold.getItemAgencyCode();

    var locationId = agencyMappingService.getLocationIdByAgencyCode(centralServerId, itemAgencyCode);

    var holdingSourceId = getHoldingSourceId();

    return new Holding()
      .hrid(holdingHrid)
      .callNumber(hold.getCallNumber())
      .discoverySuppress(true)
      .permanentLocationId(locationId)
      .sourceId(holdingSourceId);
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
      .discoverySuppress(true)
      .materialType(new MaterialType(materialTypeId, null))
      .permanentLoanType(new LoanType(centralServer.getLoanTypeId(), null))
      .status(InventoryItemStatus.AVAILABLE)
      .build();
  }

  private UUID getInstanceTypeId() {
    var instanceTypes = inventoryService.queryInstanceTypeByName(INN_REACH_TEMPORARY_RECORD);

    return instanceTypes.getId();
  }

  private ContributorDTO getInstanceContributor() {
    var author = inventoryService.queryContributorTypeByName(INN_REACH_AUTHOR);

    return new ContributorDTO(author.getId(), author.getName(), false);
  }

  private UUID getHoldingSourceId() {
    return holdingsService.findHoldingSourceByName(HOLDING_SOURCE)
      .map(HoldingSourceDTO::getId)
      .orElseThrow(() -> new IllegalArgumentException("Can't find holding source for name: " + HOLDING_SOURCE));
  }

  private UpdateTemplate.UpdateOperation<InventoryItemDTO> changeFolioAssociatedItem(String folioItemBarcode, String callNumber) {
    return item -> {
      if (nonNull(folioItemBarcode)) {
        item.setBarcode(folioItemBarcode);
      }

      if (nonNull(callNumber)) {
        item.setCallNumber(callNumber);
      }

      return item;
    };
  }

}
