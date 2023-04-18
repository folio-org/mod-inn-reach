package org.folio.innreach.domain.service.impl;

import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.folio.innreach.client.CirculationClient;
import org.folio.innreach.client.MaterialTypesClient;
import org.folio.innreach.domain.dto.folio.ContributionItemCirculationStatus;
import org.folio.innreach.domain.dto.folio.inventorystorage.MaterialTypeDTO;
import org.folio.innreach.domain.service.CentralServerService;
import org.folio.innreach.domain.service.ContributionCriteriaConfigurationService;
import org.folio.innreach.domain.service.ContributionValidationService;
import org.folio.innreach.domain.service.HoldingsService;
import org.folio.innreach.domain.service.InnReachLocationService;
import org.folio.innreach.domain.service.ItemContributionOptionsConfigurationService;
import org.folio.innreach.domain.service.LibraryMappingService;
import org.folio.innreach.domain.service.MaterialTypeMappingService;
import org.folio.innreach.dto.ContributionCriteriaDTO;
import org.folio.innreach.dto.Holding;
import org.folio.innreach.dto.InnReachLocationDTO;
import org.folio.innreach.dto.Instance;
import org.folio.innreach.dto.Item;
import org.folio.innreach.dto.ItemContributionOptionsConfigurationDTO;
import org.folio.innreach.dto.LibraryMappingDTO;
import org.folio.innreach.dto.MappingValidationStatusDTO;
import org.folio.innreach.external.service.InnReachLocationExternalService;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.util.List;
import java.util.Objects;

import java.util.Set;
import java.util.UUID;

import static java.util.Collections.emptySet;

import static org.apache.commons.collections4.ListUtils.emptyIfNull;
import static org.folio.innreach.domain.service.impl.MARCRecordTransformationServiceImpl.isMARCRecord;
import static org.folio.innreach.dto.ItemStatus.NameEnum.AVAILABLE;
import static org.folio.innreach.dto.ItemStatus.NameEnum.CHECKED_OUT;
import static org.folio.innreach.dto.ItemStatus.NameEnum.IN_TRANSIT;
import static org.folio.innreach.dto.MappingValidationStatusDTO.INVALID;
import static org.folio.innreach.dto.MappingValidationStatusDTO.VALID;
import static org.folio.innreach.util.ListUtils.mapItems;

@Log4j2
@AllArgsConstructor
@Service
public class ContributionValidationServiceImpl implements ContributionValidationService {

  private static final String MATERIAL_TYPES_CQL = "cql.allRecords=1";
  private static final int LIMIT = 2000;
  public static final Character DO_NOT_CONTRIBUTE_CODE = Character.valueOf('n');

  private final MaterialTypesClient materialTypesClient;
  private final MaterialTypeMappingService typeMappingService;

  private final HoldingsService holdingsService;
  private final LibraryMappingService libraryMappingService;
  private final CentralServerService centralServerService;
  private final ContributionCriteriaConfigurationService contributionConfigService;
  private final InnReachLocationService innReachLocationService;
  private final InnReachLocationExternalService innReachLocationExternalService;
  private final FolioLocationService folioLocationService;

  private final ItemContributionOptionsConfigurationService itemContributionOptionsConfigurationService;

  private final CirculationClient circulationClient;

  @Override
  public boolean isEligibleForContribution(UUID centralServerId, Instance instance) {
    log.debug("isEligibleForContribution:: parameters centralServerId: {},instance: {}", centralServerId, instance);
    if (!isMARCRecord(instance)) {
      log.info("Source {} is not supported", instance.getSource());
      return false;
    }

    if (isExcludedStatisticalCode(centralServerId, instance.getStatisticalCodeIds())) {
      log.info("Instance has 'do not contribute' suppression status");
      return false;
    }

    var contributionItemsCount = emptyIfNull(instance.getItems()).stream()
      .filter(i -> isEligibleForContribution(centralServerId, i))
      .count();

    if (contributionItemsCount == 0) {
      log.info("Instance has no items eligible for contribution");
      return false;
    }

    return true;
  }

  @Override
  public boolean isEligibleForContribution(UUID centralServerId, Item item) {
    log.debug("isEligibleForContribution:: parameters centralServerId: {}, item: {}", centralServerId, item);
    var statisticalCodeIds = item.getStatisticalCodeIds();
    var holdingStatisticalCodeIds = fetchHoldingStatisticalCodes(item);

    if (isExcludedStatisticalCode(centralServerId, statisticalCodeIds) ||
      isExcludedStatisticalCode(centralServerId, holdingStatisticalCodeIds)) {
      log.info("Item has 'do not contribute' suppression status");
      return false;
    }

    if (isExcludedLocation(centralServerId, item)) {
      log.info("Item {} with location is excluded from contribution", item.getHrid());
      return false;
    }

    if (!isItemHasAssociatedLibrary(centralServerId, item)) {
      log.info("Item's location is not associated with INN-Reach local agencies");
      return false;
    }

    return true;
  }

  private boolean isExcludedStatisticalCode(UUID centralServerId, Set<UUID> statisticalCodeIds) {
    log.debug("isExcludedStatisticalCode:: parameters centralServerId: {}, statisticalCodeIds: {}", centralServerId, statisticalCodeIds);
    if (CollectionUtils.isEmpty(statisticalCodeIds)) {
      return false;
    } else if (statisticalCodeIds.size() > 1) {
      log.info("More than one statistical code defined");
      return true;
    }

    var suppressionCode = getSuppressionStatus(centralServerId, statisticalCodeIds);

    return DO_NOT_CONTRIBUTE_CODE.equals(suppressionCode);
  }

  //If item's effective location is matched with contribution criteria excluded locations
  private boolean isExcludedLocation(UUID centralServerId, Item item) {

    log.debug("isExcludedLocation:: parameters centralServerId: {}, item: {}", centralServerId, item);

    List<UUID> excludedLocationIds = Objects.
            requireNonNull(getContributionConfigService(centralServerId)).getLocationIds();
    return excludedLocationIds.contains(item.getEffectiveLocationId());
  }

  @Override
  public ContributionItemCirculationStatus getItemCirculationStatus(UUID centralServerId, Item item) {
    log.debug("getItemCirculationStatus:: parameters centralServerId: {}, item: {}", centralServerId, item);
    var itemContributionConfig = itemContributionOptionsConfigurationService
      .getItmContribOptConf(centralServerId);

    if (isItemNonLendable(item, itemContributionConfig)) {
      return ContributionItemCirculationStatus.NON_LENDABLE;
    }

    if (item.getStatus().getName() == CHECKED_OUT) {
      return ContributionItemCirculationStatus.ON_LOAN;
    }

    if (isItemAvailableForContribution(item, itemContributionConfig)) {
      return ContributionItemCirculationStatus.AVAILABLE;
    }

    return ContributionItemCirculationStatus.NOT_AVAILABLE;
  }

  @Override
  public Character getSuppressionStatus(UUID centralServerId, Set<UUID> statisticalCodeIds) {
    log.debug("getSuppressionStatus:: parameters centralServerId: {}, statisticalCodeIds: {}", centralServerId, statisticalCodeIds);
    if (CollectionUtils.isEmpty(statisticalCodeIds)) {
      return null;
    }

    Assert.isTrue(statisticalCodeIds.size() == 1, "Multiple statistical codes defined");

    var config = getContributionConfigService(centralServerId);
    if (config == null) {
      log.warn("Contribution criteria is not set, skipping suppression status check");
      return null;
    }

    var statisticalCodeId = statisticalCodeIds.iterator().next();

    var excludedCodeId = config.getDoNotContributeId();
    if (Objects.equals(statisticalCodeId, excludedCodeId)) {
      return 'n';
    }

    var suppressId = config.getContributeButSuppressId();
    if (Objects.equals(statisticalCodeId, suppressId)) {
      return 'y';
    }

    var systemOwnedId = config.getContributeAsSystemOwnedId();
    if (Objects.equals(statisticalCodeId, systemOwnedId)) {
      return 'g';
    }

    return null;
  }

  @Override
  public MappingValidationStatusDTO getItemTypeMappingStatus(UUID centralServerId) {
    log.debug("getItemTypeMappingStatus:: parameters centralServerId: {}", centralServerId);
    try {
      List<UUID> typeIds = getMaterialTypeIds();

      long mappedTypesCounter = typeMappingService.countByTypeIds(centralServerId, typeIds);

      log.info("getItemTypeMappingStatus:: result: {}", mappedTypesCounter == typeIds.size() ? VALID : INVALID);
      return mappedTypesCounter == typeIds.size() ? VALID : INVALID;
    } catch (Exception e) {
      log.warn("Can't validate material type mappings", e);
      return INVALID;
    }
  }

  @Override
  public MappingValidationStatusDTO getLocationMappingStatus(UUID centralServerId) {
    log.debug("getLocationMappingStatus:: parameters centralServerId: {}", centralServerId);
    try {
      List<LibraryMappingDTO> libraryMappings = getLibraryMappings(centralServerId);

      var libraryMappingStatus = validateLibraryMappings(centralServerId, libraryMappings);
      if (libraryMappingStatus != VALID) {
        log.info("getLocationMappingStatus:: result when status not valid: {}", libraryMappingStatus);
        return libraryMappingStatus;
      }

      return validateInnReachLocations(centralServerId, libraryMappings);
    } catch (Exception e) {
      log.warn("Can't validate location mappings", e);
      return INVALID;
    }
  }

  private Set<UUID> fetchHoldingStatisticalCodes(Item item) {
    log.debug("fetchHoldingStatisticalCodes:: parameters item: {}", item);
    return holdingsService.find(item.getHoldingsRecordId())
      .map(Holding::getStatisticalCodeIds)
      .orElse(emptySet());
  }

  private boolean isItemNonLendable(Item inventoryItem,
                                    ItemContributionOptionsConfigurationDTO itemContributionConfig) {
    log.debug("isItemNonLendable:: parameters inventoryItem: {}, itemContributionConfig: {}", inventoryItem, itemContributionConfig);
    return isItemNonLendableByLoanTypes(inventoryItem, itemContributionConfig) ||
      isItemNonLendableByLocations(inventoryItem, itemContributionConfig) ||
      isItemNonLendableByMaterialTypes(inventoryItem, itemContributionConfig);
  }

  private boolean isItemNonLendableByLoanTypes(Item inventoryItem,
                                               ItemContributionOptionsConfigurationDTO itemContributionConfig) {
    log.debug("isItemNonLendableByLoanTypes:: parameters inventoryItem: {}, itemContributionConfig: {}", inventoryItem, itemContributionConfig);
    var nonLendableLoanTypes = emptyIfNull(itemContributionConfig.getNonLendableLoanTypes());
    return nonLendableLoanTypes.contains(inventoryItem.getPermanentLoanTypeId()) ||
      nonLendableLoanTypes.contains(inventoryItem.getTemporaryLoanTypeId());
  }

  private boolean isItemNonLendableByLocations(Item inventoryItem,
                                               ItemContributionOptionsConfigurationDTO itemContributionConfig) {
    log.debug("isItemNonLendableByLocations:: parameters inventoryItem: {}, itemContributionConfig: {}", inventoryItem, itemContributionConfig);
    var nonLendableLocations = emptyIfNull(itemContributionConfig.getNonLendableLocations());
    return nonLendableLocations.contains(inventoryItem.getEffectiveLocationId());
  }

  private boolean isItemNonLendableByMaterialTypes(Item inventoryItem,
                                                   ItemContributionOptionsConfigurationDTO itemContributionConfig) {
    log.debug("isItemNonLendableByMaterialTypes:: parameters inventoryItem: {}, itemContributionConfig: {}", inventoryItem, itemContributionConfig);
    var nonLendableMaterialTypes = emptyIfNull(itemContributionConfig.getNonLendableMaterialTypes());
    return nonLendableMaterialTypes.contains(inventoryItem.getMaterialTypeId());
  }

  private boolean isItemAvailableForContribution(Item inventoryItem,
                                                 ItemContributionOptionsConfigurationDTO itemContributionConfig) {
    log.debug("isItemAvailableForContribution:: parameters inventoryItem: {}, itemContributionConfig: {}", inventoryItem, itemContributionConfig);
    var itemStatus = inventoryItem.getStatus();

    if (itemStatus.getName() == IN_TRANSIT && isItemRequested(inventoryItem)) {
      return false;
    }

    return itemStatus.getName() == AVAILABLE || !itemContributionConfig.getNotAvailableItemStatuses().contains(itemStatus.getName().getValue());
  }

  private boolean isItemRequested(Item inventoryItem) {

    log.debug("isItemRequested:: parameters inventoryItem: {}", inventoryItem);

    var itemRequests = circulationClient.queryRequestsByItemIdAndStatus(inventoryItem.getId(),1);
    return itemRequests.getTotalRecords() != 0;
  }

  private List<UUID> getMaterialTypeIds() {
    log.debug("getMaterialTypeIds:: no parameter");
    return mapItems(materialTypesClient.getMaterialTypes(MATERIAL_TYPES_CQL, LIMIT).getResult(), MaterialTypeDTO::getId);
  }

  private MappingValidationStatusDTO validateLibraryMappings(UUID centralServerId, List<LibraryMappingDTO> libraryMappings) {
    List<UUID> centralServerFolioLibraryIds = getFolioLibraryIds(centralServerId);

    var mappedLibraryIds = mapItems(libraryMappings, LibraryMappingDTO::getLibraryId);

    return mappedLibraryIds.containsAll(centralServerFolioLibraryIds) ? VALID : INVALID;
  }

  private MappingValidationStatusDTO validateInnReachLocations(UUID centralServerId, List<LibraryMappingDTO> libraryMappings) {
    List<String> irLocationCodes = getAllInnReachLocationCodes(centralServerId);

    List<String> mappedIrLocationCodes = getMappedInnReachLocationCodes(libraryMappings);

    return irLocationCodes.containsAll(mappedIrLocationCodes) ? VALID : INVALID;
  }

  private List<String> getAllInnReachLocationCodes(UUID centralServerId) {
    log.debug("getAllInnReachLocationCodes:: parameters centralServerId: {}", centralServerId);
    var centralServerConnectionDetails = centralServerService.getCentralServerConnectionDetails(centralServerId);

    return mapItems(innReachLocationExternalService.getAllLocations(centralServerConnectionDetails),
      org.folio.innreach.external.dto.InnReachLocationDTO::getCode);
  }

  private List<UUID> getFolioLibraryIds(UUID centralServerId) {
    log.debug("getFolioLibraryIds:: parameters centralServerId: {}", centralServerId);
    return centralServerService.getCentralServer(centralServerId).getLocalAgencies()
      .stream()
      .flatMap(agency -> agency.getFolioLibraryIds().stream())
      .distinct()
      .toList();
  }

  private List<LibraryMappingDTO> getLibraryMappings(UUID centralServerId) {
    log.debug("getLibraryMappings:: parameters centralServerId: {}", centralServerId);
    return libraryMappingService.getAllMappings(centralServerId, 0, LIMIT).getLibraryMappings();
  }

  private List<String> getMappedInnReachLocationCodes(List<LibraryMappingDTO> libraryMappings) {
    log.debug("getMappedInnReachLocationCodes:: parameters libraryMappings: {}", libraryMappings);
    var ids = mapItems(libraryMappings, LibraryMappingDTO::getInnReachLocationId);

    return mapItems(innReachLocationService.getInnReachLocations(ids).getLocations(), InnReachLocationDTO::getCode);
  }

  private ContributionCriteriaDTO getContributionConfigService(UUID centralServerId) {
    log.debug("getContributionConfigService:: parameters centralServerId: {}", centralServerId);
    ContributionCriteriaDTO config;
    try {
      config = contributionConfigService.getCriteria(centralServerId);
    } catch (Exception e) {
      log.warn("Unable to load contribution config for central server = {}", centralServerId, e);
      return null;
    }
    log.info("getContributionConfigService:: result: {}", config);
    return config;
  }

  private boolean isItemHasAssociatedLibrary(UUID centralServerId, Item item) {
    log.debug("isItemHasAssociatedLibrary:: parameters centralServerId: {}, item: {}", centralServerId, item);
    var localAgencyLibraryIds = getFolioLibraryIds(centralServerId);
    var locationLibraryMappings = folioLocationService.getLocationLibraryMappings();
    var itemLibraryId = locationLibraryMappings.get(item.getEffectiveLocationId());

    return localAgencyLibraryIds.contains(itemLibraryId);
  }

}
