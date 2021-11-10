package org.folio.innreach.domain.service.impl;

import static org.apache.commons.collections4.ListUtils.emptyIfNull;

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
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import org.folio.innreach.client.CirculationClient;
import org.folio.innreach.client.MaterialTypesClient;
import org.folio.innreach.domain.dto.folio.ContributionItemCirculationStatus;
import org.folio.innreach.domain.dto.folio.inventorystorage.MaterialTypeDTO;
import org.folio.innreach.domain.service.CentralServerService;
import org.folio.innreach.domain.service.ContributionCriteriaConfigurationService;
import org.folio.innreach.domain.service.ContributionValidationService;
import org.folio.innreach.domain.service.InnReachLocationService;
import org.folio.innreach.domain.service.ItemContributionOptionsConfigurationService;
import org.folio.innreach.domain.service.LibraryMappingService;
import org.folio.innreach.domain.service.MaterialTypeMappingService;
import org.folio.innreach.dto.ContributionCriteriaDTO;
import org.folio.innreach.dto.InnReachLocationDTO;
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

  private final MaterialTypesClient materialTypesClient;
  private final MaterialTypeMappingService typeMappingService;

  private final LibraryMappingService libraryMappingService;
  private final CentralServerService centralServerService;
  private final ContributionCriteriaConfigurationService contributionConfigService;
  private final InnReachLocationService innReachLocationService;
  private final InnReachLocationExternalService innReachLocationExternalService;

  private final ItemContributionOptionsConfigurationService itemContributionOptionsConfigurationService;

  private final CirculationClient circulationClient;

  @Override
  public ContributionItemCirculationStatus getItemCirculationStatus(UUID centralServerId, Item item) {
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
  public Character getSuppressionStatus(UUID centralServerId, List<UUID> statisticalCodeIds) {
    var config = getContributionConfigService(centralServerId);
    if (config == null || CollectionUtils.isEmpty(statisticalCodeIds)) {
      return null;
    }

    Assert.isTrue(statisticalCodeIds.size() == 1, "Multiple statistical codes defined");

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
    return nonLendableLocations.contains(inventoryItem.getPermanentLocationId());
  }

  private boolean isItemNonLendableByMaterialTypes(Item inventoryItem,
                                                   ItemContributionOptionsConfigurationDTO itemContributionConfig) {
    var nonLendableMaterialTypes = emptyIfNull(itemContributionConfig.getNonLendableMaterialTypes());
    return nonLendableMaterialTypes.contains(inventoryItem.getMaterialTypeId());
  }

  private boolean isItemAvailableForContribution(Item inventoryItem,
                                                 ItemContributionOptionsConfigurationDTO itemContributionConfig) {
    var itemStatus = inventoryItem.getStatus();

    if (itemStatus.getName() == IN_TRANSIT && isItemRequested(inventoryItem)) {
      return false;
    }

    return itemStatus.getName() == AVAILABLE || !itemContributionConfig.getNotAvailableItemStatuses().contains(itemStatus.getName().getValue());
  }

  private boolean isItemRequested(Item inventoryItem) {
    var itemRequests = circulationClient.findRequests(inventoryItem.getId());
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

}
