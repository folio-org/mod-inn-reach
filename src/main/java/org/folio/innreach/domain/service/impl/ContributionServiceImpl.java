package org.folio.innreach.domain.service.impl;

import static org.folio.innreach.dto.MappingValidationStatusDTO.INVALID;
import static org.folio.innreach.dto.MappingValidationStatusDTO.VALID;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import org.folio.innreach.client.InventoryClient;
import org.folio.innreach.domain.dto.folio.inventory.MaterialTypeDTO;
import org.folio.innreach.domain.entity.Contribution;
import org.folio.innreach.domain.service.CentralServerService;
import org.folio.innreach.domain.service.ContributionService;
import org.folio.innreach.domain.service.InnReachLocationService;
import org.folio.innreach.domain.service.LibraryMappingService;
import org.folio.innreach.domain.service.MaterialTypeMappingService;
import org.folio.innreach.dto.ContributionDTO;
import org.folio.innreach.dto.ContributionsDTO;
import org.folio.innreach.dto.InnReachLocationDTO;
import org.folio.innreach.dto.LibraryMappingDTO;
import org.folio.innreach.dto.MappingValidationStatusDTO;
import org.folio.innreach.external.service.InnReachLocationExternalService;
import org.folio.innreach.mapper.ContributionMapper;
import org.folio.innreach.repository.ContributionRepository;

@Log4j2
@AllArgsConstructor
@Service
public class ContributionServiceImpl implements ContributionService {

  private static final String MATERIAL_TYPES_CQL = "cql.allRecords=1";
  private static final int LIMIT = 2000;

  private final ContributionRepository repository;
  private final ContributionMapper mapper;

  private final InventoryClient inventoryClient;
  private final MaterialTypeMappingService typeMappingService;

  private final LibraryMappingService libraryMappingService;
  private final CentralServerService centralServerService;
  private final InnReachLocationService innReachLocationService;
  private final InnReachLocationExternalService innReachLocationExternalService;

  @Override
  public ContributionDTO getCurrent(UUID centralServerId) {
    var entity = repository.fetchCurrentByCentralServerId(centralServerId)
      .orElseGet(Contribution::new);

    var contribution = mapper.toDTO(entity);

    contribution.setItemTypeMappingStatus(validateTypeMappings(centralServerId));
    contribution.setLocationsMappingStatus(validateLocationMappings(centralServerId));

    return contribution;
  }

  @Override
  public ContributionsDTO getHistory(UUID centralServerId, int offset, int limit) {
    var page = repository.fetchHistoryByCentralServerId(centralServerId, PageRequest.of(offset, limit));
    return mapper.toDTOCollection(page);
  }

  private MappingValidationStatusDTO validateTypeMappings(UUID centralServerId) {
    try {
      List<UUID> typeIds = getMaterialTypeIds();

      long mappedTypesCounter = typeMappingService.countByTypeIds(centralServerId, typeIds);

      return mappedTypesCounter == typeIds.size() ? VALID : INVALID;
    } catch (Exception e) {
      log.warn("Can't validate material type mappings", e);
      return INVALID;
    }
  }

  private List<UUID> getMaterialTypeIds() {
    return inventoryClient.getMaterialTypes(MATERIAL_TYPES_CQL, LIMIT).getResult()
      .stream()
      .map(MaterialTypeDTO::getId)
      .collect(Collectors.toList());
  }

  private MappingValidationStatusDTO validateLocationMappings(UUID centralServerId) {
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

  private MappingValidationStatusDTO validateLibraryMappings(UUID centralServerId, List<LibraryMappingDTO> libraryMappings) {
    List<UUID> centralServerFolioLibraryIds = getFolioLibraryIds(centralServerId);

    var mappedLibraryIds = libraryMappings.stream()
      .map(LibraryMappingDTO::getLibraryId)
      .collect(Collectors.toList());

    return mappedLibraryIds.containsAll(centralServerFolioLibraryIds) ? VALID : INVALID;
  }

  private MappingValidationStatusDTO validateInnReachLocations(UUID centralServerId, List<LibraryMappingDTO> libraryMappings) {
    List<String> irLocationCodes = getAllInnReachLocationCodes(centralServerId);

    List<String> mappedIrLocationCodes = getMappedInnReachLocationCodes(libraryMappings);

    return irLocationCodes.containsAll(mappedIrLocationCodes) ? VALID : INVALID;
  }

  private List<String> getAllInnReachLocationCodes(UUID centralServerId) {
    var centralServerConnectionDetails = centralServerService.getCentralServerConnectionDetails(centralServerId);

    return innReachLocationExternalService.getAllLocations(centralServerConnectionDetails)
      .stream()
      .map(org.folio.innreach.external.dto.InnReachLocationDTO::getCode)
      .collect(Collectors.toList());
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
    var ids = libraryMappings.stream().map(LibraryMappingDTO::getInnReachLocationId).collect(Collectors.toList());

    return innReachLocationService.getInnReachLocations(ids).getLocations()
      .stream()
      .map(InnReachLocationDTO::getCode)
      .collect(Collectors.toList());
  }

}
