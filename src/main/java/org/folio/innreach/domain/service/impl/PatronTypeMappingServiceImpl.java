package org.folio.innreach.domain.service.impl;

import static org.folio.innreach.domain.service.impl.ServiceUtils.centralServerRef;
import static org.folio.innreach.domain.service.impl.ServiceUtils.initId;
import static org.folio.innreach.domain.service.impl.ServiceUtils.mergeAndSave;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.folio.innreach.domain.entity.CentralPatronTypeMapping;
import org.folio.innreach.domain.entity.CentralServer;
import org.folio.innreach.domain.entity.PatronTypeMapping;
import org.folio.innreach.domain.service.PatronTypeMappingService;
import org.folio.innreach.dto.PatronTypeMappingsDTO;
import org.folio.innreach.mapper.PatronTypeMappingMapper;
import org.folio.innreach.repository.PatronTypeMappingRepository;

@RequiredArgsConstructor
@Service
@Transactional
public class PatronTypeMappingServiceImpl implements PatronTypeMappingService {

  private final PatronTypeMappingRepository repository;
  private final PatronTypeMappingMapper mapper;

  @Override
  public PatronTypeMappingsDTO getAllMappings(UUID centralServerId, int offset, int limit) {
    var example = mappingExampleWithServerId(centralServerId);

    Page<PatronTypeMapping> mappings = repository.findAll(example, PageRequest.of(offset, limit));

    return mapper.toDTOCollection(mappings);
  }

  @Override
  public PatronTypeMappingsDTO updateAllMappings(UUID centralServerId, PatronTypeMappingsDTO patronTypeMappingsDTO) {
    var stored = repository.findAll(mappingExampleWithServerId(centralServerId));

    var incoming = mapper.toEntities(patronTypeMappingsDTO.getPatronTypeMappings());
    var csRef = centralServerRef(centralServerId);
    incoming.forEach(setCentralServerRef(csRef).andThen(initId()));

    var saved = mergeAndSave(incoming, stored, repository, this::copyData);

    return mapper.toDTOCollection(saved);
  }

  @Override
  public Optional<Integer> getCentralPatronType(UUID centralServerId, UUID patronGroupId) {
    return repository.findOneByCentralServerIdAndPatronGroupId(centralServerId, patronGroupId)
      .map(PatronTypeMapping::getPatronType);
  }

  private static Example<PatronTypeMapping> mappingExampleWithServerId(UUID centralServerId) {
    var toFind = new PatronTypeMapping();
    toFind.setCentralServer(centralServerRef(centralServerId));

    return Example.of(toFind);
  }

  private void copyData(PatronTypeMapping from, PatronTypeMapping to) {
    to.setPatronType(from.getPatronType());
    to.setPatronGroupId(from.getPatronGroupId());
  }

  private static Consumer<PatronTypeMapping> setCentralServerRef(CentralServer centralServer) {
    return mapping -> mapping.setCentralServer(centralServer);
  }
}
