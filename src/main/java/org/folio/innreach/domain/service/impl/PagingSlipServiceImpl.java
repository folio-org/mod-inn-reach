package org.folio.innreach.domain.service.impl;

import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Service;

import org.folio.innreach.client.LocationsClient;
import org.folio.innreach.domain.dto.folio.circulation.RequestDTO;
import org.folio.innreach.domain.dto.folio.inventory.InventoryInstanceDTO;
import org.folio.innreach.domain.dto.folio.inventory.InventoryItemDTO;
import org.folio.innreach.domain.dto.folio.inventorystorage.LocationDTO;
import org.folio.innreach.domain.service.CentralServerConfigurationService;
import org.folio.innreach.domain.service.CentralServerService;
import org.folio.innreach.domain.service.InnReachTransactionService;
import org.folio.innreach.domain.service.InstanceService;
import org.folio.innreach.domain.service.ItemService;
import org.folio.innreach.domain.service.PagingSlipService;
import org.folio.innreach.domain.service.RequestService;
import org.folio.innreach.dto.AgenciesPerCentralServerDTO;
import org.folio.innreach.dto.Agency;
import org.folio.innreach.dto.CentralServerDTO;
import org.folio.innreach.dto.InnReachTransactionDTO;
import org.folio.innreach.dto.InnReachTransactionFilterParametersDTO;
import org.folio.innreach.dto.PagingSlip;
import org.folio.innreach.dto.PagingSlipsDTO;
import org.folio.innreach.dto.PagingSlipsDTOInnReachTransaction;
import org.folio.innreach.dto.PagingSlipsDTOItem;
import org.folio.innreach.dto.PagingSlipsDTOSlip;
import org.folio.innreach.dto.PatronType;
import org.folio.innreach.dto.PatronTypesPerCentralServerDTO;
import org.folio.innreach.dto.TransactionHoldDTO;
import org.folio.innreach.dto.TransactionStateEnum;
import org.folio.innreach.dto.TransactionTypeEnum;
import org.folio.innreach.mapper.InnReachTransactionPickupLocationMapper;

@Service
@Log4j2
@RequiredArgsConstructor
public class PagingSlipServiceImpl implements PagingSlipService {

  private static final int FETCH_LIMIT = 2000;
  private static final String SLIP_NAME_PREFIX = "INN-Reach Paging Slip - ";

  private final LocationsClient locationsClient;
  private final ItemService itemService;
  private final RequestService requestService;
  private final InstanceService instanceService;
  private final InnReachTransactionService transactionService;
  private final CentralServerConfigurationService centralServerConfigurationService;
  private final CentralServerService centralServerService;
  private final InnReachTransactionPickupLocationMapper pickupLocationMapper;

  @Override
  public PagingSlipsDTO getPagingSlipsByServicePoint(UUID servicePointId) {
    log.info("Preparing INN-Reach paging slip for service point {}", servicePointId);

    var transactions = fetchItemTransactions();
    if (transactions.isEmpty()) {
      return new PagingSlipsDTO();
    }

    var transactionWithInventoryRecordsMap = fetchSlipItemsAndInstances(servicePointId, transactions);
    if (transactionWithInventoryRecordsMap.isEmpty()) {
      return new PagingSlipsDTO();
    }

    var centralAgencies = fetchCentralAgenciesPerServerCode();
    var patronTypes = fetchPatronTypesPerServerCode();
    var centralServers = fetchCentralServersPerServerCode();

    var pagingSlips = new ArrayList<PagingSlip>();
    for (var entry : transactionWithInventoryRecordsMap.entrySet()) {
      var transaction = entry.getKey();
      var itemAndInstancePair = entry.getValue();
      var item = itemAndInstancePair.getLeft();
      var instance = itemAndInstancePair.getRight();

      var hold = transaction.getHold();
      var centralServer = centralServers.get(transaction.getCentralServerCode());

      var pagingSlip = createPagingSlip(hold, item, instance, centralServer, centralAgencies, patronTypes);

      pagingSlips.add(pagingSlip);
    }

    return new PagingSlipsDTO()
      .pagingSlips(pagingSlips)
      .totalRecords(pagingSlips.size());
  }

  private Map<InnReachTransactionDTO, Pair<InventoryItemDTO, InventoryInstanceDTO>> fetchSlipItemsAndInstances(
    UUID servicePointId, List<InnReachTransactionDTO> transactions) {

    var locationIds = fetchLocationIdsByServicePoint(servicePointId);
    if (locationIds.isEmpty()) {
      return Collections.emptyMap();
    }

    var itemIds = fetchItemIdsOfNotFilledRequests(transactions);
    if (itemIds.isEmpty()) {
      return Collections.emptyMap();
    }

    var items = fetchItems(itemIds, locationIds);
    var instances = fetchInstances(transactions);

    var transactionWithInventoryRecords = new LinkedHashMap<InnReachTransactionDTO, Pair<InventoryItemDTO, InventoryInstanceDTO>>();
    for (var transaction : transactions) {
      var hold = transaction.getHold();
      var item = items.get(hold.getFolioItemId());
      var instance = instances.get(hold.getFolioInstanceId());

      if (item != null && instance != null) {
        transactionWithInventoryRecords.put(transaction, Pair.of(item, instance));
      }
    }

    return transactionWithInventoryRecords;
  }

  private Map<UUID, InventoryItemDTO> fetchItems(Set<UUID> itemIds, Set<UUID> locationIds) {
    return itemService.findItemsByIdsAndLocations(itemIds, locationIds, FETCH_LIMIT)
      .stream()
      .collect(toMap(InventoryItemDTO::getId, Function.identity()));
  }

  private Map<UUID, InventoryInstanceDTO> fetchInstances(List<InnReachTransactionDTO> transactions) {
    var instanceIds = getTransactionInstanceIds(transactions);

    return instanceService.findInstancesByIds(instanceIds, FETCH_LIMIT)
      .stream()
      .collect(toMap(InventoryInstanceDTO::getId, Function.identity()));
  }

  private Set<UUID> fetchItemIdsOfNotFilledRequests(List<InnReachTransactionDTO> transactions) {
    var transactionRequestIds = getTransactionRequestIds(transactions);
    var notFilledRequests = requestService.findNotFilledRequestsByIds(transactionRequestIds, FETCH_LIMIT);

    return notFilledRequests.getResult().stream()
      .map(RequestDTO::getItemId)
      .collect(Collectors.toSet());
  }

  private List<InnReachTransactionDTO> fetchItemTransactions() {
    var transactionFilter = new InnReachTransactionFilterParametersDTO()
      .addTypeItem(TransactionTypeEnum.ITEM)
      .addStateItem(TransactionStateEnum.ITEM_HOLD)
      .addStateItem(TransactionStateEnum.TRANSFER);

    return transactionService.getAllTransactions(0, FETCH_LIMIT, transactionFilter).getTransactions();
  }

  private Map<String, CentralServerDTO> fetchCentralServersPerServerCode() {
    return centralServerService.getAllCentralServers(0, FETCH_LIMIT)
      .getCentralServers()
      .stream()
      .collect(toMap(CentralServerDTO::getCentralServerCode, Function.identity()));
  }

  private Map<String, List<PatronType>> fetchPatronTypesPerServerCode() {
    return centralServerConfigurationService.getAllPatronTypes()
      .getCentralServerPatronTypes()
      .stream()
      .collect(toMap(PatronTypesPerCentralServerDTO::getCentralServerCode, PatronTypesPerCentralServerDTO::getPatronTypes));
  }

  private Map<String, List<Agency>> fetchCentralAgenciesPerServerCode() {
    return centralServerConfigurationService.getAllAgencies()
      .getCentralServerAgencies()
      .stream()
      .collect(toMap(AgenciesPerCentralServerDTO::getCentralServerCode, AgenciesPerCentralServerDTO::getAgencies));
  }

  private Set<UUID> fetchLocationIdsByServicePoint(UUID servicePointId) {
    return locationsClient.queryLocationsByServicePoint(servicePointId, FETCH_LIMIT).getResult()
      .stream()
      .map(LocationDTO::getId)
      .collect(toSet());
  }

  private Set<UUID> getTransactionRequestIds(List<InnReachTransactionDTO> transactions) {
    return transactions.stream().map(InnReachTransactionDTO::getHold)
      .map(TransactionHoldDTO::getFolioRequestId)
      .filter(Objects::nonNull)
      .collect(Collectors.toSet());
  }

  private Set<UUID> getTransactionInstanceIds(List<InnReachTransactionDTO> transactions) {
    return transactions.stream()
      .map(InnReachTransactionDTO::getHold)
      .map(TransactionHoldDTO::getFolioInstanceId)
      .filter(Objects::nonNull)
      .collect(Collectors.toSet());
  }

  @SuppressWarnings("squid:S2637")
  private String getPatronTypeDescription(Map<String, List<PatronType>> patronTypes, String centralServerCode, Integer centralPatronType) {
    return patronTypes.get(centralServerCode).stream()
      .filter(patronType -> patronType.getCentralPatronType().equals(centralPatronType))
      .map(PatronType::getDescription)
      .findFirst()
      .orElse(null);
  }

  @SuppressWarnings("squid:S2637")
  private String getAgencyDescription(Map<String, List<Agency>> centralAgencies, String centralServerCode, String agencyCode) {
    return centralAgencies.get(centralServerCode).stream()
      .filter(agency -> agency.getAgencyCode().equals(agencyCode))
      .map(Agency::getDescription)
      .findFirst()
      .orElse(null);
  }

  private String getPrimaryContributorName(InventoryInstanceDTO instance) {
    return instance.getContributors().stream()
      .filter(InventoryInstanceDTO.ContributorDTO::getPrimary)
      .map(InventoryInstanceDTO.ContributorDTO::getName)
      .findFirst()
      .orElse(null);
  }

  private PagingSlip createPagingSlip(TransactionHoldDTO hold, InventoryItemDTO item, InventoryInstanceDTO instance,
                                      CentralServerDTO centralServer,
                                      Map<String, List<Agency>> centralAgencies,
                                      Map<String, List<PatronType>> patronTypes) {
    return new PagingSlip()
      .slip(createSlip(centralServer))
      .item(createItemSlip(instance, item))
      .innReachTransaction(createTransactionSlip(hold, centralServer, centralAgencies, patronTypes));
  }

  private PagingSlipsDTOInnReachTransaction createTransactionSlip(TransactionHoldDTO hold, CentralServerDTO centralServer,
                                                                  Map<String, List<Agency>> centralAgencies,
                                                                  Map<String, List<PatronType>> patronTypes) {
    var centralServerCode = centralServer.getCentralServerCode();
    var patronAgencyCode = hold.getPatronAgencyCode();
    var itemAgencyCode = hold.getItemAgencyCode();
    var centralPatronType = hold.getCentralPatronType();

    var patronAgencyDescription = getAgencyDescription(centralAgencies, centralServerCode, patronAgencyCode);
    var itemAgencyDescription = getAgencyDescription(centralAgencies, centralServerCode, itemAgencyCode);
    var patronTypeDescription = getPatronTypeDescription(patronTypes, centralServerCode, centralPatronType);
    var pickupLocation = pickupLocationMapper.fromString(hold.getPickupLocation());

    return new PagingSlipsDTOInnReachTransaction()
      .patronName(hold.getPatronName())
      .patronAgencyCode(patronAgencyCode)
      .patronAgencyDescription(patronAgencyDescription)
      .patronTypeCode(centralPatronType)
      .patronTypeDescription(patronTypeDescription)
      .centralServerCode(centralServerCode)
      .centralServerId(centralServer.getId())
      .localServerCode(centralServer.getLocalServerCode())
      .itemAgencyCode(itemAgencyCode)
      .itemAgencyDescription(itemAgencyDescription)
      .pickupLocationCode(pickupLocation.getPickupLocCode())
      .pickupLocationDisplayName(pickupLocation.getDisplayName())
      .pickupLocationPrintName(pickupLocation.getPrintName())
      .pickupLocationDeliveryStop(pickupLocation.getDeliveryStop());
  }

  private PagingSlipsDTOItem createItemSlip(InventoryInstanceDTO instance, InventoryItemDTO item) {
    var locationName = item.getEffectiveLocation().getName();
    var author = getPrimaryContributorName(instance);

    return new PagingSlipsDTOItem()
      .effectiveLocationFolioName(locationName)
      .title(item.getTitle())
      .barcode(item.getBarcode())
      .author(author)
      .effectiveCallNumber(item.getCallNumber())
      .shelvingOrder(item.getEffectiveShelvingOrder())
      .hrid(item.getHrid());
  }

  private PagingSlipsDTOSlip createSlip(CentralServerDTO centralServer) {
    return new PagingSlipsDTOSlip().name(SLIP_NAME_PREFIX + centralServer.getName());
  }

}
