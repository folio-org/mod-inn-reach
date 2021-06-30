package org.folio.innreach.domain.service.impl;

import static org.folio.innreach.domain.service.impl.ServiceUtils.centralServerRef;
import static org.folio.innreach.domain.service.impl.ServiceUtils.equalIds;
import static org.folio.innreach.domain.service.impl.ServiceUtils.initId;
import static org.folio.innreach.domain.service.impl.ServiceUtils.merge;

import java.util.UUID;
import java.util.function.Consumer;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.folio.innreach.domain.entity.CentralServer;
import org.folio.innreach.domain.entity.LocationMapping;
import org.folio.innreach.domain.service.LocationMappingService;
import org.folio.innreach.dto.LocationMappingsDTO;
import org.folio.innreach.mapper.LocationMappingMapper;
import org.folio.innreach.repository.LocationMappingRepository;

@RequiredArgsConstructor
@Service
@Transactional
public class LocationMappingServiceImpl implements LocationMappingService {

  private final LocationMappingRepository repository;
  private final LocationMappingMapper mapper;


  @Override
  @Transactional(readOnly = true)
  public LocationMappingsDTO getAllMappings(UUID centralServerId, UUID libraryId, int offset, int limit) {
    var example = mappingExampleWithServerId(centralServerId);

    Page<LocationMapping> mappings = repository.findAll(example, PageRequest.of(offset, limit));

    return mapper.toDTOCollection(mappings);
  }

  @Override
  public LocationMappingsDTO updateAllMappings(UUID centralServerId, UUID libraryId,
      LocationMappingsDTO locationMappingsDTO) {
    var stored = repository.findAll(mappingExampleWithServerId(centralServerId));

    var incoming = mapper.toEntities(locationMappingsDTO.getLocationMappings());
    var csRef = centralServerRef(centralServerId);
    incoming.forEach(setCentralServerRef(csRef)
        .andThen(setLibraryId(libraryId))
        .andThen(initId()));

    var saved = merge(incoming, stored, repository, this::copyData);

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
