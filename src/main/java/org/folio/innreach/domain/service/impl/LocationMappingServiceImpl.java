package org.folio.innreach.domain.service.impl;

import static org.folio.innreach.domain.service.impl.ServiceUtils.DEFAULT_SORT;
import static org.folio.innreach.domain.service.impl.ServiceUtils.centralServerRef;
import static org.folio.innreach.domain.service.impl.ServiceUtils.equalIds;
import static org.folio.innreach.domain.service.impl.ServiceUtils.initId;
import static org.folio.innreach.domain.service.impl.ServiceUtils.mergeAndSave;

import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.folio.innreach.domain.entity.CentralServer;
import org.folio.innreach.domain.entity.LocationMapping;
import org.folio.innreach.domain.service.InnReachLocationContributionService;
import org.folio.innreach.domain.service.LocationMappingService;
import org.folio.innreach.dto.LocationMappingDTO;
import org.folio.innreach.dto.LocationMappingsDTO;
import org.folio.innreach.mapper.LocationMappingMapper;
import org.folio.innreach.repository.LocationMappingRepository;
import org.folio.spring.data.OffsetRequest;

@Log4j2
@RequiredArgsConstructor
@Service
@Transactional
public class LocationMappingServiceImpl implements LocationMappingService {

  private final LocationMappingRepository repository;
  private final LocationMappingMapper mapper;
  private final InnReachLocationContributionService locationContributionService;

  @Override
  @Transactional(readOnly = true)
  public LocationMappingsDTO getMappingsByLibraryId(UUID centralServerId, UUID libraryId, int offset, int limit) {
    log.debug("getMappingsByLibraryId:: parameters centralServerId: {}, libraryId: {}, offset: {}, limit: {}", centralServerId, libraryId, offset, limit);
    var example = mappingExampleWithServerIdAndLibraryId(centralServerId, libraryId);

    Page<LocationMapping> mappings = repository.findAll(example, new OffsetRequest(offset, limit, DEFAULT_SORT));

    log.info("getMappingsByLibraryId:: result: {}", mapper.toDTOCollection(mappings));
    return mapper.toDTOCollection(mappings);
  }

  @Override
  @Transactional(readOnly = true)
  public List<LocationMappingDTO> getAllMappings(UUID centralServerId) {
    log.debug("getAllMappings:: parameters centralServerId: {}", centralServerId);
    var example = mappingExampleWithServerId(centralServerId);

    List<LocationMapping> mappings = repository.findAll(example);

    log.info("getAllMappings:: result: {}", mapper.toDTOs(mappings));
    return mapper.toDTOs(mappings);
  }

  @Override
  public LocationMappingsDTO updateAllMappings(UUID centralServerId, UUID libraryId,
                                               LocationMappingsDTO locationMappingsDTO) {
    log.debug("updateAllMappings:: parameters centralServerId: {}, libraryId: {}, locationMappingsDTO: {}", centralServerId, libraryId, locationMappingsDTO);
    var stored = repository.findByCentralServerIdAndLibraryId(centralServerId, libraryId);

    var incoming = mapper.toEntities(locationMappingsDTO.getLocationMappings());

    incoming.removeIf(mapping -> mapping.getInnReachLocation().getId() == null);

    var csRef = centralServerRef(centralServerId);
    incoming.forEach(setCentralServerRef(csRef)
      .andThen(setLibraryId(libraryId))
      .andThen(initId()));

    var saved = mergeAndSave(incoming, stored, repository, this::copyData);

    locationContributionService.contributeInnReachLocations(centralServerId);

    log.info("updateAllMappings:: result: {}", mapper.toDTOCollection(saved));
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

  private static Example<LocationMapping> mappingExampleWithServerId(UUID centralServerId) {
    var toFind = new LocationMapping();
    toFind.setCentralServer(centralServerRef(centralServerId));

    return Example.of(toFind);
  }

}
