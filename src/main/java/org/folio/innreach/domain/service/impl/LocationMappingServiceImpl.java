package org.folio.innreach.domain.service.impl;

import static org.folio.innreach.domain.service.impl.ServiceUtils.DEFAULT_SORT;
import static org.folio.innreach.domain.service.impl.ServiceUtils.centralServerRef;
import static org.folio.innreach.domain.service.impl.ServiceUtils.equalIds;
import static org.folio.innreach.domain.service.impl.ServiceUtils.initId;
import static org.folio.innreach.domain.service.impl.ServiceUtils.mergeAndSave;
import static org.folio.innreach.util.ListUtils.mapItems;

import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.folio.innreach.domain.entity.CentralServer;
import org.folio.innreach.domain.entity.InnReachLocation;
import org.folio.innreach.domain.entity.LocationMapping;
import org.folio.innreach.domain.service.CentralServerService;
import org.folio.innreach.domain.service.LocationMappingService;
import org.folio.innreach.dto.LocationMappingForAllLibrariesDTO;
import org.folio.innreach.dto.LocationMappingsForOneLibraryDTO;
import org.folio.innreach.external.dto.InnReachLocationDTO;
import org.folio.innreach.external.service.InnReachLocationExternalService;
import org.folio.innreach.mapper.LocationMappingForAllLibrariesMapper;
import org.folio.innreach.mapper.LocationMappingMapper;
import org.folio.innreach.repository.InnReachLocationRepository;
import org.folio.innreach.repository.LocationMappingRepository;
import org.folio.spring.data.OffsetRequest;

@RequiredArgsConstructor
@Service
@Transactional
public class LocationMappingServiceImpl implements LocationMappingService {

  private final LocationMappingRepository repository;
  private final InnReachLocationRepository innReachLocationRepository;
  private final LocationMappingMapper locationMappingMapper;
  private final LocationMappingForAllLibrariesMapper locationMappingForAllLibrariesMapper;
  private final CentralServerService centralServerService;
  private final InnReachLocationExternalService innReachLocationExternalService;

  @Override
  @Transactional(readOnly = true)
  public LocationMappingsForOneLibraryDTO getMappingsByLibraryId(UUID centralServerId, UUID libraryId, int offset, int limit) {
    var example = mappingExampleWithServerIdAndLibraryId(centralServerId, libraryId);

    Page<LocationMapping> mappings = repository.findAll(example, new OffsetRequest(offset, limit, DEFAULT_SORT));

    return locationMappingMapper.toDTOCollection(mappings);
  }

  @Override
  @Transactional(readOnly = true)
  public List<LocationMappingForAllLibrariesDTO> getAllMappings(UUID centralServerId) {
    var example = mappingExampleWithServerId(centralServerId);

    List<LocationMapping> mappings = repository.findAll(example);

    return locationMappingForAllLibrariesMapper.toDTOs(mappings);
  }

  @Override
  public LocationMappingsForOneLibraryDTO updateAllMappings(UUID centralServerId, UUID libraryId,
                                               LocationMappingsForOneLibraryDTO locationMappingsDTO) {
    var stored = repository.findByCentralServerIdAndLibraryId(centralServerId, libraryId);

    var incoming = locationMappingMapper.toEntities(locationMappingsDTO.getLocationMappings());
    var csRef = centralServerRef(centralServerId);
    incoming.forEach(setCentralServerRef(csRef)
      .andThen(setLibraryId(libraryId))
      .andThen(initId()));

    var saved = mergeAndSave(incoming, stored, repository, this::copyData);

    var centralServerMappedLocations = getCentralServerMappedLocations(saved);
    var centralServerConnectionDetails = centralServerService.getCentralServerConnectionDetails(centralServerId);

    innReachLocationExternalService.submitMappedLocationsToInnReach(centralServerConnectionDetails, centralServerMappedLocations);

    return locationMappingMapper.toDTOCollection(saved);
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

  private static Example<LocationMapping> mappingExampleWithServerId(UUID centralServerId) {
    var toFind = new LocationMapping();
    toFind.setCentralServer(centralServerRef(centralServerId));

    return Example.of(toFind);
  }

  private List<InnReachLocationDTO> getCentralServerMappedLocations(List<LocationMapping> actualLibraryMappings) {
    var locationsIds = getCentralServerMappedLocationsIds(actualLibraryMappings);

    return mapItems(innReachLocationRepository.findAllById(locationsIds),
        innReachLocation -> new InnReachLocationDTO(innReachLocation.getCode(), innReachLocation.getDescription()));
  }

  private List<UUID> getCentralServerMappedLocationsIds(List<LocationMapping> actualLibraryMappings) {
    return actualLibraryMappings.stream()
      .map(LocationMapping::getInnReachLocation)
      .map(InnReachLocation::getId)
      .distinct()
      .collect(Collectors.toList());
  }

}
