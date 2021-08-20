package org.folio.innreach.domain.service.impl;

import static org.folio.innreach.domain.service.impl.ServiceUtils.DEFAULT_SORT;
import static org.folio.innreach.domain.service.impl.ServiceUtils.centralServerRef;
import static org.folio.innreach.domain.service.impl.ServiceUtils.equalIds;
import static org.folio.innreach.domain.service.impl.ServiceUtils.initId;
import static org.folio.innreach.domain.service.impl.ServiceUtils.mergeAndSave;

import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.folio.innreach.domain.entity.CentralServer;
import org.folio.innreach.domain.entity.InnReachLocation;
import org.folio.innreach.domain.entity.LocationMapping;
import org.folio.innreach.domain.service.CentralServerService;
import org.folio.innreach.domain.service.LocationMappingService;
import org.folio.innreach.dto.LocationMappingsDTO;
import org.folio.innreach.external.dto.InnReachLocationDTO;
import org.folio.innreach.external.service.InnReachLocationExternalService;
import org.folio.innreach.mapper.LocationMappingMapper;
import org.folio.innreach.repository.InnReachLocationRepository;
import org.folio.innreach.repository.LocationMappingRepository;

@RequiredArgsConstructor
@Service
@Transactional
public class LocationMappingServiceImpl implements LocationMappingService {

  private final LocationMappingRepository repository;
  private final InnReachLocationRepository innReachLocationRepository;
  private final LocationMappingMapper mapper;
  private final CentralServerService centralServerService;
  private final InnReachLocationExternalService innReachLocationExternalService;

  @Override
  @Transactional(readOnly = true)
  public LocationMappingsDTO getAllMappings(UUID centralServerId, UUID libraryId, int offset, int limit) {
    var example = mappingExampleWithServerIdAndLibraryId(centralServerId, libraryId);

    Page<LocationMapping> mappings = repository.findAll(example, PageRequest.of(offset, limit, DEFAULT_SORT));

    return mapper.toDTOCollection(mappings);
  }

  @Override
  public LocationMappingsDTO updateAllMappings(UUID centralServerId, UUID libraryId,
      LocationMappingsDTO locationMappingsDTO) {
    var stored = repository.findAll(mappingExampleWithServerIdAndLibraryId(centralServerId, libraryId));

    var incoming = mapper.toEntities(locationMappingsDTO.getLocationMappings());
    var csRef = centralServerRef(centralServerId);
    incoming.forEach(setCentralServerRef(csRef)
        .andThen(setLibraryId(libraryId))
        .andThen(initId()));

    var saved = mergeAndSave(incoming, stored, repository, this::copyData);

    var centralServerMappedLocations = getCentralServerMappedLocations(saved);
    var centralServerConnectionDetails = centralServerService.getCentralServerConnectionDetails(centralServerId);

    innReachLocationExternalService.submitMappedLocationsToInnReach(centralServerConnectionDetails, centralServerMappedLocations);

    return mapper.toDTOCollection(saved);
  }

  private void copyData(LocationMapping from, LocationMapping to) {
    to.setLocationId(from.getLocationId());
    to.setLibraryId(from.getLibraryId());

    if (!equalIds(to.getInnReachLocation(), from.getInnReachLocation())) {
      to.setInnReachLocation(from.getInnReachLocation());
    }
  }

  private static Consumer<LocationMapping> setLibraryId(UUID libraryId) {
    return mapping -> mapping.setLibraryId(libraryId);
  }

  private static Consumer<LocationMapping> setCentralServerRef(CentralServer centralServer) {
    return mapping -> mapping.setCentralServer(centralServer);
  }

  private static Example<LocationMapping> mappingExampleWithServerIdAndLibraryId(UUID centralServerId, UUID libraryId) {
    var toFind = new LocationMapping();
    toFind.setCentralServer(centralServerRef(centralServerId));
    toFind.setLibraryId(libraryId);

    return Example.of(toFind);
  }

  private List<InnReachLocationDTO> getCentralServerMappedLocations(List<LocationMapping> actualLibraryMappings) {
    var locationsIds = getCentralServerMappedLocationsIds(actualLibraryMappings);

    return innReachLocationRepository.findAllById(locationsIds)
      .stream()
      .map(innReachLocation -> new InnReachLocationDTO(innReachLocation.getCode(), innReachLocation.getDescription()))
      .collect(Collectors.toList());
  }

  private List<UUID> getCentralServerMappedLocationsIds(List<LocationMapping> actualLibraryMappings) {
    return actualLibraryMappings.stream()
      .map(LocationMapping::getInnReachLocation)
      .map(InnReachLocation::getId)
      .distinct()
      .collect(Collectors.toList());
  }

}
