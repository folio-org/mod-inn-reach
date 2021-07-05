package org.folio.innreach.domain.service.impl;

import static org.folio.innreach.domain.service.impl.ServiceUtils.DEFAULT_SORT;
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
import org.folio.innreach.domain.entity.LibraryMapping;
import org.folio.innreach.domain.service.LibraryMappingService;
import org.folio.innreach.dto.LibraryMappingsDTO;
import org.folio.innreach.mapper.LibraryMappingMapper;
import org.folio.innreach.repository.LibraryMappingRepository;

@RequiredArgsConstructor
@Service
@Transactional
public class LibraryMappingServiceImpl implements LibraryMappingService {

  private final LibraryMappingRepository repository;
  private final LibraryMappingMapper mapper;


  @Override
  @Transactional(readOnly = true)
  public LibraryMappingsDTO getAllMappings(UUID centralServerId, int offset, int limit) {
    var example = mappingExampleWithServerId(centralServerId);

    Page<LibraryMapping> mappings = repository.findAll(example, PageRequest.of(offset, limit, DEFAULT_SORT));

    return mapper.toDTOCollection(mappings);
  }

  @Override
  public LibraryMappingsDTO updateAllMappings(UUID centralServerId, LibraryMappingsDTO libraryMappingsDTO) {
    var stored = repository.findAll(mappingExampleWithServerId(centralServerId));

    var incoming = mapper.toEntities(libraryMappingsDTO.getLibraryMappings());
    var csRef = centralServerRef(centralServerId);
    incoming.forEach(setCentralServerRef(csRef).andThen(initId()));

    var saved = merge(incoming, stored, repository, this::copyData);

    return mapper.toDTOCollection(saved);
  }

  private void copyData(LibraryMapping from, LibraryMapping to) {
    to.setLibraryId(from.getLibraryId());
    
    if (!equalIds(to.getInnReachLocation(), from.getInnReachLocation())) {
      to.setInnReachLocation(from.getInnReachLocation());
    }
  }

  private static Consumer<LibraryMapping> setCentralServerRef(CentralServer centralServer) {
    return mapping -> mapping.setCentralServer(centralServer);
  }

  private static Example<LibraryMapping> mappingExampleWithServerId(UUID centralServerId) {
    var toFind = new LibraryMapping();
    toFind.setCentralServer(centralServerRef(centralServerId));

    return Example.of(toFind);
  }

}
