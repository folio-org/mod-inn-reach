package org.folio.innreach.domain.service.impl;

import static java.util.stream.Collectors.toMap;

import static org.folio.innreach.domain.dto.folio.ContributionItemCirculationStatus.ON_LOAN;
import static org.folio.innreach.util.ListUtils.getFirstItem;
import static org.folio.innreach.util.ListUtils.getLastItem;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiConsumer;

import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.CharUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import org.folio.innreach.client.CirculationClient;
import org.folio.innreach.domain.dto.folio.ContributionItemCirculationStatus;
import org.folio.innreach.domain.service.CentralServerService;
import org.folio.innreach.domain.service.ContributionValidationService;
import org.folio.innreach.domain.service.HoldingsService;
import org.folio.innreach.domain.service.InnReachLocationService;
import org.folio.innreach.domain.service.LibraryMappingService;
import org.folio.innreach.domain.service.LocationMappingService;
import org.folio.innreach.domain.service.MARCRecordTransformationService;
import org.folio.innreach.domain.service.MaterialTypeMappingService;
import org.folio.innreach.domain.service.RecordTransformationService;
import org.folio.innreach.dto.BibInfo;
import org.folio.innreach.dto.Holding;
import org.folio.innreach.dto.InnReachLocationDTO;
import org.folio.innreach.dto.Instance;
import org.folio.innreach.dto.Item;
import org.folio.innreach.dto.ItemEffectiveCallNumberComponents;
import org.folio.innreach.dto.LoanDTO;
import org.folio.innreach.dto.MaterialTypeMappingDTO;
import org.folio.innreach.external.dto.BibItem;
import org.folio.innreach.util.DateHelper;

@RequiredArgsConstructor
@Log4j2
@Service
public class RecordTransformationServiceImpl implements RecordTransformationService {

  private static final String MARC_BIB_FORMAT = "ISO2709";
  private static final int FETCH_LIMIT = 2000;
  private static final String NON_DIGIT_REGEX = "\\D+";

  private final MARCRecordTransformationService marcService;
  private final ContributionValidationService validationService;

  private final HoldingsService holdingsService;
  private final MaterialTypeMappingService typeMappingService;
  private final LibraryMappingService libraryMappingService;
  private final InnReachLocationService irLocationService;
  private final CentralServerService centralServerService;
  private final LocationMappingService locationMappingService;
  private final FolioLocationService folioLocationService;

  private final CirculationClient circulationClient;

  @Override
  public BibInfo getBibInfo(UUID centralServerId, Instance instance) {
    log.debug("getBibInfo:: parameters centralServerId: {}, instance: {}", centralServerId, instance);
    var bibId = instance.getHrid();

    var suppressionStatus = validationService.getSuppressionStatus(centralServerId, instance.getStatisticalCodeIds());
    var marc = marcService.transformRecord(centralServerId, instance);

    var bibInfo = new BibInfo();
    bibInfo.setBibId(bibId);

    bibInfo.setSuppress(CharUtils.toString(suppressionStatus));
    bibInfo.setMarc21BibFormat(MARC_BIB_FORMAT);
    bibInfo.setMarc21BibData(marc.getBase64rawContent());
    bibInfo.setItemCount(countContributionItems(centralServerId, instance.getItems()));
    log.info("getBibInfo:: result: {}", bibInfo);
    return bibInfo;
  }

  @Override
  public List<BibItem> getBibItems(UUID centralServerId, List<Item> items, BiConsumer<Item, Exception> errorHandler) {
    log.debug("getBibItems:: parameters centralServerId: {}, item: {}, errorHandler: {}", centralServerId, items, errorHandler);
    var mappings = getContributionMappings(centralServerId);
    log.info("Resolved contribution mappings: {}", mappings);

    return items.stream()
      .map(item -> convertItem(centralServerId, item, mappings, errorHandler))
      .filter(Objects::nonNull)
      .toList();
  }

  private BibItem convertItem(UUID centralServerId, Item item, ContributionMappings mappings, BiConsumer<Item, Exception> errorHandler) {
    log.info("Loading item {} info", item.getHrid());
    try {
      var suppressionStatus = getSuppressionStatus(centralServerId, item);
      var circulationStatus = getCirculationStatus(centralServerId, item);

      var copyNumber = Optional.ofNullable(item.getCopyNumber())
        .map(n -> n.replaceAll(NON_DIGIT_REGEX, ""))
        .filter(NumberUtils::isCreatable)
        .map(NumberUtils::createInteger)
        .filter(n -> n != 0)
        .orElse(null);

      var volumeDesignation = item.getVolume();

      var callNumber = Optional.ofNullable(item.getEffectiveCallNumberComponents())
        .map(ItemEffectiveCallNumberComponents::getCallNumber)
        .map(StringUtils::trim)
        .orElse(null);

      var folLocId = item.getEffectiveLocationId();
      var folLibId = mappings.getLibraryId(folLocId);

      var itemId = item.getId();
      var holdCount = countRequests(itemId);
      var dueDateTime = getDueDateTime(itemId, circulationStatus);

      var bibItem = BibItem.builder()
        .itemId(item.getHrid())
        .itemCircStatus(circulationStatus.getStatus())
        .copyNumber(copyNumber)
        .callNumber(callNumber)
        .volumeDesignation(volumeDesignation)
        .suppress(suppressionStatus)
        .centralItemType(mappings.getCentralType(item.getMaterialTypeId()))
        .locationKey(mappings.getLocationKey(folLocId, folLibId))
        .agencyCode(mappings.getAgencyCode(folLibId))
        .holdCount(holdCount)
        .dueDateTime(dueDateTime)
        .build();

      validateRequiredFields(bibItem);

      log.info("Loaded bibItem {}", bibItem);

      return bibItem;
    } catch (Exception e) {
      errorHandler.accept(item, e);
      return null;
    }
  }

  private Integer getDueDateTime(UUID itemId, ContributionItemCirculationStatus circulationStatus) {
    try {
      if (circulationStatus == ON_LOAN) {
        return getLastItem(circulationClient.queryLoansByItemIdAndStatus(itemId, "Open"))
          .map(LoanDTO::getDueDate)
          .map(DateHelper::toEpochSec)
          .orElse(null);
      }
    } catch (Exception e) {
      log.warn("Failed to fetch due date time for item {}", itemId, e);
    }
    return null;
  }

  private Long countRequests(UUID itemId) {
    try {
      return circulationClient.queryRequestsByItemId(itemId).getResult()
        .stream()
        .filter(request -> request.getStatus().getName().startsWith("Open"))
        .count();
    } catch (Exception e) {
      log.warn("Failed to count requests for item {}", itemId, e);
      return null;
    }
  }

  private void validateRequiredFields(BibItem bibItem) {
    Assert.isTrue(bibItem.getItemId() != null, "itemId is not resolved");
    Assert.isTrue(bibItem.getItemCircStatus() != null, "itemCircStatus is not resolved");
    Assert.isTrue(bibItem.getCentralItemType() != null, "centralItemType is not resolved");
    Assert.isTrue(bibItem.getAgencyCode() != null, "agencyCode is not resolved");
    Assert.isTrue(bibItem.getLocationKey() != null, "locationKey is not resolved");
  }

  private int countContributionItems(UUID centralServerId, List<Item> items) {
    if (CollectionUtils.isEmpty(items)) {
      return 0;
    }
    return (int) items.stream()
      .filter(Objects::nonNull)
      .filter(i -> validationService.isEligibleForContribution(centralServerId, i))
      .count();
  }

  private Character getSuppressionStatus(UUID centralServerId, Item item) {
    Character itemSuppress = validationService.getSuppressionStatus(centralServerId, item.getStatisticalCodeIds());

    return itemSuppress != null ? itemSuppress :
      validationService.getSuppressionStatus(centralServerId, fetchHoldingStatisticalCodes(item));
  }

  private Set<UUID> fetchHoldingStatisticalCodes(Item item) {
    return holdingsService.find(item.getHoldingsRecordId())
      .map(Holding::getStatisticalCodeIds)
      .orElse(null);
  }

  private ContributionItemCirculationStatus getCirculationStatus(UUID centralServerId, Item item) {
    return validationService.getItemCirculationStatus(centralServerId, item);
  }

  private ContributionMappings getContributionMappings(UUID centralServerId) {
    Map<UUID, String> irLocIdToLocKeys = irLocationService.getAllInnReachLocations(0, FETCH_LIMIT)
      .getLocations()
      .stream()
      .collect(toMap(InnReachLocationDTO::getId, InnReachLocationDTO::getCode));

    Map<UUID, Integer> materialToCentralTypeMappings = getTypeMappings(centralServerId);
    Map<UUID, String> libIdToLocKeyMappings = getLibraryMappings(centralServerId, irLocIdToLocKeys);
    Map<UUID, String> locIdToLocKeyMappings = getLocationMappings(centralServerId, irLocIdToLocKeys, libIdToLocKeyMappings.keySet());
    Map<UUID, String> libIdToAgencyCodeMappings = getAgencyMappings(centralServerId);
    Map<UUID, UUID> locIdToLibIdMappings = folioLocationService.getLocationLibraryMappings();

    return ContributionMappings.builder()
      .materialToCentralTypes(materialToCentralTypeMappings)
      .libIdToLocKeys(libIdToLocKeyMappings)
      .locIdToLocKeys(locIdToLocKeyMappings)
      .locIdToLibIds(locIdToLibIdMappings)
      .libIdToAgencyCodes(libIdToAgencyCodeMappings)
      .build();
  }

  private Map<UUID, Integer> getTypeMappings(UUID centralServerId) {
    return typeMappingService.getAllMappings(centralServerId, 0, FETCH_LIMIT)
      .getMaterialTypeMappings()
      .stream()
      .collect(toMap(MaterialTypeMappingDTO::getMaterialTypeId, MaterialTypeMappingDTO::getCentralItemType));
  }

  private Map<UUID, String> getLibraryMappings(UUID centralServerId, Map<UUID, String> irLocations) {
    var libraryMappings =
      libraryMappingService.getAllMappings(centralServerId, 0, FETCH_LIMIT).getLibraryMappings();

    Map<UUID, String> mappings = new HashMap<>();
    for (var mapping : libraryMappings) {
      var code = irLocations.get(mapping.getInnReachLocationId());
      mappings.put(mapping.getLibraryId(), code);
    }

    return mappings;
  }

  private Map<UUID, String> getLocationMappings(UUID centralServerId, Map<UUID, String> irLocations, Collection<UUID> libraryIds) {
    Map<UUID, String> mappings = new HashMap<>();

    for (var libId : libraryIds) {
      var locationMappings =
        locationMappingService.getMappingsByLibraryId(centralServerId, libId, 0, FETCH_LIMIT).getLocationMappings();

      locationMappings.forEach(loc -> mappings.put(loc.getLocationId(), irLocations.get(loc.getInnReachLocationId())));
    }

    return mappings;
  }

  private Map<UUID, String> getAgencyMappings(UUID centralServerId) {
    var centralServer = centralServerService.getCentralServer(centralServerId);

    var localAgencies = centralServer.getLocalAgencies();
    Map<UUID, String> mappings = new HashMap<>();

    for (var localAgency : localAgencies) {
      localAgency.getFolioLibraryIds().forEach(libId -> mappings.put(libId, localAgency.getCode()));
    }

    return mappings;
  }

  @Builder
  @ToString
  private static class ContributionMappings {
    private Map<UUID, Integer> materialToCentralTypes;
    private Map<UUID, String> libIdToLocKeys;
    private Map<UUID, String> locIdToLocKeys;
    private Map<UUID, String> libIdToAgencyCodes;
    private Map<UUID, UUID> locIdToLibIds;

    public UUID getLibraryId(UUID locationId) {
      return locIdToLibIds.get(locationId);
    }

    public String getAgencyCode(UUID libraryId) {
      return libIdToAgencyCodes.get(libraryId);
    }

    public String getLocationKey(UUID locId, UUID libId) {
      return locIdToLocKeys.getOrDefault(locId, libIdToLocKeys.get(libId));
    }

    public Integer getCentralType(UUID materialTypeId) {
      return materialToCentralTypes.get(materialTypeId);
    }
  }

}
