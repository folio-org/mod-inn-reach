package org.folio.innreach.batch.contribution.service;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Service;

import org.folio.innreach.batch.contribution.ContributionJobContext;
import org.folio.innreach.domain.dto.folio.ContributionItemCirculationStatus;
import org.folio.innreach.domain.service.CentralServerService;
import org.folio.innreach.domain.service.ContributionValidationService;
import org.folio.innreach.domain.service.InnReachLocationService;
import org.folio.innreach.domain.service.LibraryMappingService;
import org.folio.innreach.domain.service.LocationMappingService;
import org.folio.innreach.domain.service.MaterialTypeMappingService;
import org.folio.innreach.dto.InnReachLocationDTO;
import org.folio.innreach.dto.Item;
import org.folio.innreach.dto.ItemEffectiveCallNumberComponents;
import org.folio.innreach.dto.MaterialTypeMappingDTO;
import org.folio.innreach.external.dto.BibItem;
import org.folio.innreach.external.service.InnReachContributionService;

@Log4j2
@StepScope
@Service
@RequiredArgsConstructor
public class ItemContributor implements ItemWriter<Item> {

  private static final int FETCH_LIMIT = 2000;
  private static final String NON_DIGIT_REGEX = "\\D+";

  private final InnReachContributionService irContributionService;
  private final ContributionValidationService validationService;
  private final MaterialTypeMappingService typeMappingService;
  private final LibraryMappingService libraryMappingService;
  private final InnReachLocationService irLocationService;
  private final CentralServerService centralServerService;
  private final LocationMappingService locationMappingService;

  private final ContributionJobContext jobContext;

  @Override
  public void write(List<? extends Item> items) {
    ContributionMappings mappings = getContributionMappings();

    items.stream()
      .collect(Collectors.groupingBy(Item::getInstanceHrid))
      .forEach((instanceHrid, instanceItems)
        -> contributeItems(instanceHrid, instanceItems, mappings));
  }

  private void contributeItems(String bibId, List<? extends Item> items, ContributionMappings mappings) {
    var bibItems = items.stream()
      .map(item -> convertItem(item, mappings))
      .collect(Collectors.toList());

    irContributionService.contributeBibItems(jobContext.getCentralServerId(), bibId, bibItems);
  }

  private BibItem convertItem(Item item, ContributionMappings mappings) {
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

    var folLocId = item.getPermanentLocationId();
    var folLibId = mappings.getLibraryId(folLocId);

    return BibItem.builder()
      .itemId(item.getHrid())
      .itemCircStatus(circulationStatus)
      .copyNumber(copyNumber)
      .callNumber(callNumber)
      .suppress(suppressionStatus)
      .centralItemType(mappings.getCentralType(item.getMaterialTypeId()))
      .locationKey(mappings.getLocationKey(folLocId, folLibId))
      .agencyCode(mappings.getAgencyCode(folLibId))
      .build();
  }

  private Character getSuppressionStatus(Item item) {
    return validationService.getSuppressionStatus(jobContext.getCentralServerId(), item.getStatisticalCodeIds());
  }

  private ContributionItemCirculationStatus getCirculationStatus(Item item) {
    return validationService.getItemCirculationStatus(jobContext.getCentralServerId(), item.getId());
  }

  private ContributionMappings getContributionMappings() {
    Map<UUID, String> irLocIdToLocKeys = irLocationService.getAllInnReachLocations(0, FETCH_LIMIT)
      .getLocations()
      .stream()
      .collect(Collectors.toMap(InnReachLocationDTO::getId, InnReachLocationDTO::getCode));

    Map<UUID, Integer> materialToCentralTypeMappings = getTypeMappings();
    Map<UUID, String> libIdToLocKeyMappings = getLibraryMappings(irLocIdToLocKeys);
    Map<UUID, String> locIdToLocKeyMappings = getLocationMappings(irLocIdToLocKeys, libIdToLocKeyMappings.keySet());
    Map<UUID, UUID> locItToLibIdMappings = getLocationLibraryMappings(libIdToLocKeyMappings.keySet());
    Map<UUID, String> libIdToAgencyCodeMappings = getAgencyMappings();

    return ContributionMappings.builder()
      .materialToCentralTypes(materialToCentralTypeMappings)
      .libIdToLocKeys(libIdToLocKeyMappings)
      .locIdToLocKeys(locIdToLocKeyMappings)
      .locIdToLibIds(locItToLibIdMappings)
      .libIdToAgencyCodes(libIdToAgencyCodeMappings)
      .build();
  }

  private Map<UUID, Integer> getTypeMappings() {
    return typeMappingService.getAllMappings(jobContext.getCentralServerId(), 0, FETCH_LIMIT)
      .getMaterialTypeMappings()
      .stream()
      .collect(Collectors.toMap(MaterialTypeMappingDTO::getMaterialTypeId, MaterialTypeMappingDTO::getCentralItemType));
  }

  private Map<UUID, String> getLibraryMappings(Map<UUID, String> irLocations) {
    var libraryMappings =
      libraryMappingService.getAllMappings(jobContext.getCentralServerId(), 0, FETCH_LIMIT).getLibraryMappings();

    Map<UUID, String> mappings = new HashMap<>();
    for (var mapping : libraryMappings) {
      var code = irLocations.get(mapping.getInnReachLocationId());
      mappings.put(mapping.getLibraryId(), code);
    }

    return mappings;
  }

  private Map<UUID, UUID> getLocationLibraryMappings(Set<UUID> libraryIds) {
    Map<UUID, UUID> mappings = new HashMap<>();

    for (var libId : libraryIds) {
      var locationMappings = locationMappingService
        .getAllMappings(jobContext.getCentralServerId(), libId, 0, FETCH_LIMIT)
        .getLocationMappings();

      locationMappings.forEach(loc -> mappings.put(loc.getLocationId(), libId));
    }

    return mappings;
  }

  private Map<UUID, String> getLocationMappings(Map<UUID, String> irLocations, Collection<UUID> libraryIds) {
    Map<UUID, String> mappings = new HashMap<>();

    for (var libId : libraryIds) {
      var locationMappings =
        locationMappingService.getAllMappings(jobContext.getCentralServerId(), libId, 0, FETCH_LIMIT).getLocationMappings();

      locationMappings.forEach(loc -> mappings.put(loc.getLocationId(), irLocations.get(loc.getInnReachLocationId())));
    }

    return mappings;
  }

  private Map<UUID, String> getAgencyMappings() {
    var centralServer = centralServerService.getCentralServer(jobContext.getCentralServerId());

    var localAgencies = centralServer.getLocalAgencies();
    Map<UUID, String> mappings = new HashMap<>();

    for (var localAgency : localAgencies) {
      localAgency.getFolioLibraryIds().forEach(libId -> mappings.put(libId, localAgency.getCode()));
    }

    return mappings;
  }

  @Builder
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
