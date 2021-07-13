package org.folio.innreach.domain.service.impl;

import static org.folio.innreach.domain.service.impl.ServiceUtils.DEFAULT_SORT;
import static org.folio.innreach.domain.service.impl.ServiceUtils.centralServerRef;
import static org.folio.innreach.domain.service.impl.ServiceUtils.equalIds;
import static org.folio.innreach.domain.service.impl.ServiceUtils.initId;
import static org.folio.innreach.domain.service.impl.ServiceUtils.mergeAndSave;

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
import org.folio.innreach.domain.entity.LocationMapping;
import org.folio.innreach.domain.service.CentralServerService;
import org.folio.innreach.domain.service.LocationMappingService;
import org.folio.innreach.dto.LocationMappingsDTO;
import org.folio.innreach.external.dto.InnReachLocationDTO;
import org.folio.innreach.external.service.InnReachLocationExternalService;
import org.folio.innreach.mapper.LocationMappingMapper;
import org.folio.innreach.repository.LocationMappingRepository;

@RequiredArgsConstructor
@Service
@Transactional
public class LocationMappingServiceImpl implements LocationMappingService {

  private final LocationMappingRepository repository;
  private final LocationMappingMapper mapper;
  private final CentralServerService centralServerService;
  private final InnReachLocationExternalService innReachLocationExternalService;

  @Override
  @Transactional(readOnly = true)
  public LocationMappingsDTO getAllMappings(UUID centralServerId, UUID libraryId, int offset, int limit) {
    var example = mappingExampleWithServerId(centralServerId);

    Page<LocationMapping> mappings = repository.findAll(example, PageRequest.of(offset, limit, DEFAULT_SORT));

    return mapper.toDTOCollection(mappings);
  }

  @Override
  public LocationMappingsDTO updateAllMappings(UUID centralServerId, UUID libraryId,
      LocationMappingsDTO locationMappingsDTO) {
    var stored = repository.fetchAllByCentralServer(centralServerId);

    var incoming = mapper.toEntities(locationMappingsDTO.getLocationMappings());
    var csRef = centralServerRef(centralServerId);
    incoming.forEach(setCentralServerRef(csRef)
        .andThen(setLibraryId(libraryId))
        .andThen(initId()));

    var saved = mergeAndSave(incoming, stored, repository, this::copyData);

    var innReachLocationDTOS = saved.stream()
      .map(LocationMapping::getInnReachLocation)
      .filter(location -> location.getCode() != null && location.getDescription() != null)
      .map(location -> new InnReachLocationDTO(location.getCode(), location.getDescription()))
      .collect(Collectors.toList());

    var connectionDetailsDTO = centralServerService.getCentralServerConnectionDetails(centralServerId);

    innReachLocationExternalService.updateAllLocations(connectionDetailsDTO, innReachLocationDTOS);

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

  private static Example<LocationMapping> mappingExampleWithServerId(UUID centralServerId) {
    var toFind = new LocationMapping();
    toFind.setCentralServer(centralServerRef(centralServerId));

    return Example.of(toFind);
  }

}
