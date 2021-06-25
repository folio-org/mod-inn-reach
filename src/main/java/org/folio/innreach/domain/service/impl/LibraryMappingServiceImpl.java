package org.folio.innreach.domain.service.impl;

import static org.folio.innreach.domain.service.impl.ServiceUtils.centralServerRef;
import static org.folio.innreach.domain.service.impl.ServiceUtils.evaluateEntitiesToDelete;
import static org.folio.innreach.domain.service.impl.ServiceUtils.initId;

import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    Page<LibraryMapping> mappings = repository.findAll(example, PageRequest.of(offset, limit));

    return mapper.toDTOCollection(mappings);
  }

  @Override
  public LibraryMappingsDTO updateAllMappings(UUID centralServerId, LibraryMappingsDTO libraryMappingsDTO) {
    var stored = repository.findAll(mappingExampleWithServerId(centralServerId));

    var incoming = mapper.toEntities(libraryMappingsDTO.getLibraryMappings());
    incoming.forEach(initId());

    var toDelete = evaluateEntitiesToDelete(stored, incoming);
    repository.deleteInBatch(toDelete);

    var saved = repository.saveAll(incoming);

    return mapper.toDTOCollection(saved);
  }

  private static Example<LibraryMapping> mappingExampleWithServerId(UUID centralServerId) {
    var toFind = new LibraryMapping();
    toFind.setCentralServer(centralServerRef(centralServerId));

    return Example.of(toFind);
  }

}