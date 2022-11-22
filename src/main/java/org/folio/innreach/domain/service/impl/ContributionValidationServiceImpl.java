package org.folio.innreach.domain.service.impl;

import static java.util.Collections.emptyList;
import static org.apache.commons.collections4.ListUtils.emptyIfNull;

import static org.folio.innreach.domain.service.impl.MARCRecordTransformationServiceImpl.isMARCRecord;
import static org.folio.innreach.dto.ItemStatus.NameEnum.AVAILABLE;
import static org.folio.innreach.dto.ItemStatus.NameEnum.CHECKED_OUT;
import static org.folio.innreach.dto.ItemStatus.NameEnum.IN_TRANSIT;
import static org.folio.innreach.dto.MappingValidationStatusDTO.INVALID;
import static org.folio.innreach.dto.MappingValidationStatusDTO.VALID;
import static org.folio.innreach.util.ListUtils.mapItems;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.folio.innreach.dto.LocalAgencyDTO;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

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

  private boolean isExcludedStatisticalCode(UUID centralServerId, List<UUID> statisticalCodeIds) {
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
    List<UUID> excludedLocationIds = Objects.
            requireNonNull(getContributionConfigService(centralServerId)).getLocationIds();
    return excludedLocationIds.contains(item.getEffectiveLocationId());
  }

  @Override
  public ContributionItemCirculationStatus getItemCirculationStatus(UUID centralServerId, Item item) {
    var itemContributionConfig = itemContributionOptionsConfigurationService
      .getItmContribOptConf(centralServerId);

    log.info("getItemCirculationStatus: itemContributionConfig {}", itemContributionConfig);

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
  public Character getSuppressionStatus(UUID centralServerId, List<UUID> statisticalCodeIds) {
    if (CollectionUtils.isEmpty(statisticalCodeIds)) {
      return null;
    }

    Assert.isTrue(statisticalCodeIds.size() == 1, "Multiple statistical codes defined");

    var config = getContributionConfigService(centralServerId);
    if (config == null) {
      log.warn("Contribution criteria is not set, skipping suppression status check");
      return null;
    }

    var statisticalCodeId = statisticalCodeIds.get(0);

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
    try {
      List<UUID> typeIds = getMaterialTypeIds();

      long mappedTypesCounter = typeMappingService.countByTypeIds(centralServerId, typeIds);

      return mappedTypesCounter == typeIds.size() ? VALID : INVALID;
    } catch (Exception e) {
      log.warn("Can't validate material type mappings", e);
      return INVALID;
    }
  }

  @Override
  public MappingValidationStatusDTO getLocationMappingStatus(UUID centralServerId) {
    try {
      List<LibraryMappingDTO> libraryMappings = getLibraryMappings(centralServerId);

      var libraryMappingStatus = validateLibraryMappings(centralServerId, libraryMappings);
      if (libraryMappingStatus != VALID) {
        return libraryMappingStatus;
      }

      return validateInnReachLocations(centralServerId, libraryMappings);
    } catch (Exception e) {
      log.warn("Can't validate location mappings", e);
      return INVALID;
    }
  }

  private List<UUID> fetchHoldingStatisticalCodes(Item item) {
    return holdingsService.find(item.getHoldingsRecordId())
      .map(Holding::getStatisticalCodeIds)
      .orElse(emptyList());
  }

  private boolean isItemNonLendable(Item inventoryItem,
                                    ItemContributionOptionsConfigurationDTO itemContributionConfig) {
    return isItemNonLendableByLoanTypes(inventoryItem, itemContributionConfig) ||
      isItemNonLendableByLocations(inventoryItem, itemContributionConfig) ||
      isItemNonLendableByMaterialTypes(inventoryItem, itemContributionConfig);
  }

  private boolean isItemNonLendableByLoanTypes(Item inventoryItem,
                                               ItemContributionOptionsConfigurationDTO itemContributionConfig) {
    var nonLendableLoanTypes = emptyIfNull(itemContributionConfig.getNonLendableLoanTypes());
    return nonLendableLoanTypes.contains(inventoryItem.getPermanentLoanTypeId()) ||
      nonLendableLoanTypes.contains(inventoryItem.getTemporaryLoanTypeId());
  }

  private boolean isItemNonLendableByLocations(Item inventoryItem,
                                               ItemContributionOptionsConfigurationDTO itemContributionConfig) {
    var nonLendableLocations = emptyIfNull(itemContributionConfig.getNonLendableLocations());
    return nonLendableLocations.contains(inventoryItem.getEffectiveLocationId());
  }

  private boolean isItemNonLendableByMaterialTypes(Item inventoryItem,
                                                   ItemContributionOptionsConfigurationDTO itemContributionConfig) {
    var nonLendableMaterialTypes = emptyIfNull(itemContributionConfig.getNonLendableMaterialTypes());
    return nonLendableMaterialTypes.contains(inventoryItem.getMaterialTypeId());
  }

  private boolean isItemAvailableForContribution(Item inventoryItem,
                                                 ItemContributionOptionsConfigurationDTO itemContributionConfig) {
    var itemStatus = inventoryItem.getStatus();
    log.info("isItemAvailableForContribution : itemStatus : {}",itemStatus );
    if (itemStatus.getName() == IN_TRANSIT && isItemRequested(inventoryItem)) {
      return false;
    }
    log.info("isItemAvailableForContribution : itemContributionConfig : {}",itemContributionConfig );
    return itemStatus.getName() == AVAILABLE || !itemContributionConfig.getNotAvailableItemStatuses().contains(itemStatus.getName().getValue());
  }

  private boolean isItemRequested(Item inventoryItem) {
    log.info("isItemRequested : {} with status : {}", inventoryItem, inventoryItem.getStatus());
    var itemRequests = circulationClient.queryRequestsByItemId(inventoryItem.getId());
    log.info("itemRequests.getTotalRecords() : {}", itemRequests.getTotalRecords());
    return itemRequests.getTotalRecords() != 0;
  }

  private List<UUID> getMaterialTypeIds() {
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
    var centralServerConnectionDetails = centralServerService.getCentralServerConnectionDetails(centralServerId);

    return mapItems(innReachLocationExternalService.getAllLocations(centralServerConnectionDetails),
      org.folio.innreach.external.dto.InnReachLocationDTO::getCode);
  }

  private List<UUID> getFolioLibraryIds(UUID centralServerId) {
    return centralServerService.getCentralServer(centralServerId).getLocalAgencies()
      .stream()
      .flatMap(agency -> agency.getFolioLibraryIds().stream())
      .distinct()
      .collect(Collectors.toList());
  }

  private List<LibraryMappingDTO> getLibraryMappings(UUID centralServerId) {
    return libraryMappingService.getAllMappings(centralServerId, 0, LIMIT).getLibraryMappings();
  }

  private List<String> getMappedInnReachLocationCodes(List<LibraryMappingDTO> libraryMappings) {
    var ids = mapItems(libraryMappings, LibraryMappingDTO::getInnReachLocationId);

    return mapItems(innReachLocationService.getInnReachLocations(ids).getLocations(), InnReachLocationDTO::getCode);
  }

  private ContributionCriteriaDTO getContributionConfigService(UUID centralServerId) {
    ContributionCriteriaDTO config;
    try {
      config = contributionConfigService.getCriteria(centralServerId);
    } catch (Exception e) {
      log.warn("Unable to load contribution config for central server = {}", centralServerId, e);
      return null;
    }
    return config;
  }

  private boolean isItemHasAssociatedLibrary(UUID centralServerId, Item item) {
    var localAgencyLibraryIds = getFolioLibraryIds(centralServerId);
    var locationLibraryMappings = folioLocationService.getLocationLibraryMappings();
    var itemLibraryId = locationLibraryMappings.get(item.getEffectiveLocationId());

    return localAgencyLibraryIds.contains(itemLibraryId);
  }

}
