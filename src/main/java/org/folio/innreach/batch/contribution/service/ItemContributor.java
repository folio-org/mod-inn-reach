package org.folio.innreach.batch.contribution.service;

import static java.util.stream.Collectors.toMap;

import static org.folio.innreach.batch.contribution.ContributionJobContextManager.getContributionJobContext;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import org.folio.innreach.batch.contribution.ContributionJobContextManager;
import org.folio.innreach.batch.contribution.listener.ContributionExceptionListener;
import org.folio.innreach.domain.dto.folio.ContributionItemCirculationStatus;
import org.folio.innreach.domain.service.CentralServerService;
import org.folio.innreach.domain.service.ContributionValidationService;
import org.folio.innreach.domain.service.InnReachLocationService;
import org.folio.innreach.domain.service.LibraryMappingService;
import org.folio.innreach.domain.service.LocationMappingService;
import org.folio.innreach.domain.service.MaterialTypeMappingService;
import org.folio.innreach.domain.service.impl.FolioLocationService;
import org.folio.innreach.dto.InnReachLocationDTO;
import org.folio.innreach.dto.Item;
import org.folio.innreach.dto.ItemEffectiveCallNumberComponents;
import org.folio.innreach.dto.MaterialTypeMappingDTO;
import org.folio.innreach.external.dto.BibItem;
import org.folio.innreach.external.dto.BibItemsInfo;
import org.folio.innreach.external.service.InnReachContributionService;

@Log4j2
@Service
@RequiredArgsConstructor
public class ItemContributor {

  private static final int FETCH_LIMIT = 2000;
  private static final String NON_DIGIT_REGEX = "\\D+";

  @Qualifier("itemExceptionListener")
  private final ContributionExceptionListener exceptionListener;
  private final InnReachContributionService irContributionService;
  private final ContributionValidationService validationService;
  private final MaterialTypeMappingService typeMappingService;
  private final LibraryMappingService libraryMappingService;
  private final InnReachLocationService irLocationService;
  private final CentralServerService centralServerService;
  private final LocationMappingService locationMappingService;
  private final FolioLocationService folioLocationService;

  public int contributeItems(String bibId, List<Item> items) {
    log.info("Processing items of bib {}", bibId);
    var centralServerId = getContributionJobContext().getCentralServerId();

    var mappings = getContributionMappings();
    log.info("Resolved contribution mappings: {}", mappings);

    var bibItems = items.stream()
      .filter(item -> validationService.isEligibleForContribution(centralServerId, item))
      .map(item -> convertItem(item, mappings))
      .filter(Objects::nonNull)
      .collect(Collectors.toList());

    var bibItemsInfo = BibItemsInfo.of(bibItems);

    int writeCount = bibItemsInfo.getItemInfo().size();

    log.info("Loaded {} items", writeCount);

    var response = irContributionService.contributeBibItems(getCentralServerId(), bibId, bibItemsInfo);
    Assert.isTrue(response.isOk(), "Unexpected items contribution response: " + response);

    log.info("Finished contributing items of bib {}", bibId);

    return writeCount;
  }

  private BibItem convertItem(Item item, ContributionMappings mappings) {
    log.info("Loading item {} info", item.getHrid());
    try {
      var suppressionStatus = getSuppressionStatus(item);
      var circulationStatus = getCirculationStatus(item);

      var copyNumber = Optional.ofNullable(item.getCopyNumber())
        .map(n -> n.replaceAll(NON_DIGIT_REGEX, ""))
        .filter(NumberUtils::isCreatable)
        .map(NumberUtils::createInteger)
        .filter(n -> n != 0)
        .orElse(null);

      var callNumber = Optional.ofNullable(item.getEffectiveCallNumberComponents())
        .map(ItemEffectiveCallNumberComponents::getCallNumber)
        .orElse(null);

      var folLocId = item.getEffectiveLocationId();
      var folLibId = mappings.getLibraryId(folLocId);

      var bibItem = BibItem.builder()
        .itemId(item.getHrid())
        .itemCircStatus(circulationStatus.getStatus())
        .copyNumber(copyNumber)
        .callNumber(callNumber)
        .suppress(suppressionStatus)
        .centralItemType(mappings.getCentralType(item.getMaterialTypeId()))
        .locationKey(mappings.getLocationKey(folLocId, folLibId))
        .agencyCode(mappings.getAgencyCode(folLibId))
        .build();

      log.info("Loaded bibItem {}", bibItem);

      return bibItem;
    } catch (Exception e) {
      exceptionListener.logWriteError(new RuntimeException("Unable to load item info: " + e.getMessage(), e), item.getId());
      return null;
    }
  }

  private Character getSuppressionStatus(Item item) {
    var centralServerId = getCentralServerId();
    Character itemSuppress = validationService.getSuppressionStatus(centralServerId, item.getStatisticalCodeIds());

    return itemSuppress != null ? itemSuppress :
      validationService.getSuppressionStatus(centralServerId, item.getHoldingStatisticalCodeIds());
  }

  private ContributionItemCirculationStatus getCirculationStatus(Item item) {
    return validationService.getItemCirculationStatus(getCentralServerId(), item);
  }

  private ContributionMappings getContributionMappings() {
    Map<UUID, String> irLocIdToLocKeys = irLocationService.getAllInnReachLocations(0, FETCH_LIMIT)
      .getLocations()
      .stream()
      .collect(toMap(InnReachLocationDTO::getId, InnReachLocationDTO::getCode));

    Map<UUID, Integer> materialToCentralTypeMappings = getTypeMappings();
    Map<UUID, String> libIdToLocKeyMappings = getLibraryMappings(irLocIdToLocKeys);
    Map<UUID, String> locIdToLocKeyMappings = getLocationMappings(irLocIdToLocKeys, libIdToLocKeyMappings.keySet());
    Map<UUID, UUID> locIdToLibIdMappings = folioLocationService.getLocationLibraryMappings();
    Map<UUID, String> libIdToAgencyCodeMappings = getAgencyMappings();

    return ContributionMappings.builder()
      .materialToCentralTypes(materialToCentralTypeMappings)
      .libIdToLocKeys(libIdToLocKeyMappings)
      .locIdToLocKeys(locIdToLocKeyMappings)
      .locIdToLibIds(locIdToLibIdMappings)
      .libIdToAgencyCodes(libIdToAgencyCodeMappings)
      .build();
  }

  private Map<UUID, Integer> getTypeMappings() {
    return typeMappingService.getAllMappings(getCentralServerId(), 0, FETCH_LIMIT)
      .getMaterialTypeMappings()
      .stream()
      .collect(toMap(MaterialTypeMappingDTO::getMaterialTypeId, MaterialTypeMappingDTO::getCentralItemType));
  }

  private Map<UUID, String> getLibraryMappings(Map<UUID, String> irLocations) {
    var libraryMappings =
      libraryMappingService.getAllMappings(getCentralServerId(), 0, FETCH_LIMIT).getLibraryMappings();

    Map<UUID, String> mappings = new HashMap<>();
    for (var mapping : libraryMappings) {
      var code = irLocations.get(mapping.getInnReachLocationId());
      mappings.put(mapping.getLibraryId(), code);
    }

    return mappings;
  }

  private Map<UUID, String> getLocationMappings(Map<UUID, String> irLocations, Collection<UUID> libraryIds) {
    Map<UUID, String> mappings = new HashMap<>();

    for (var libId : libraryIds) {
      var locationMappings =
        locationMappingService.getAllMappings(getCentralServerId(), libId, 0, FETCH_LIMIT).getLocationMappings();

      locationMappings.forEach(loc -> mappings.put(loc.getLocationId(), irLocations.get(loc.getInnReachLocationId())));
    }

    return mappings;
  }

  private Map<UUID, String> getAgencyMappings() {
    var centralServer = centralServerService.getCentralServer(getCentralServerId());

    var localAgencies = centralServer.getLocalAgencies();
    Map<UUID, String> mappings = new HashMap<>();

    for (var localAgency : localAgencies) {
      localAgency.getFolioLibraryIds().forEach(libId -> mappings.put(libId, localAgency.getCode()));
    }

    return mappings;
  }

  private UUID getCentralServerId() {
    return ContributionJobContextManager.getContributionJobContext().getCentralServerId();
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
