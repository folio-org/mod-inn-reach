package org.folio.innreach.domain.service.impl;

import static org.folio.innreach.domain.service.impl.ServiceUtils.centralServerRef;
import static org.folio.innreach.domain.service.impl.ServiceUtils.initId;
import static org.folio.innreach.domain.service.impl.ServiceUtils.mergeAndSave;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.folio.innreach.domain.entity.CentralPatronTypeMapping;
import org.folio.innreach.domain.entity.CentralServer;
import org.folio.innreach.domain.service.CentralPatronTypeMappingService;
import org.folio.innreach.dto.CentralPatronTypeMappingsDTO;
import org.folio.innreach.mapper.CentralPatronTypeMappingMapper;
import org.folio.innreach.repository.CentralPatronTypeMappingRepository;

@Service
@RequiredArgsConstructor
@Transactional
public class CentralPatronTypeMappingServiceImpl implements CentralPatronTypeMappingService {

  private final CentralPatronTypeMappingRepository repository;
  private final CentralPatronTypeMappingMapper mapper;

  @Override
  @Transactional(readOnly = true)
  public CentralPatronTypeMappingsDTO getCentralPatronTypeMappings(UUID centralServerId, Integer offset, Integer limit) {
    var centralPatronTypeMappingExample = mappingExampleWithServerId(centralServerId);

    var centralPatronTypeMappings = repository.findAll(centralPatronTypeMappingExample, PageRequest.of(offset, limit));

    return mapper.toDTOCollection(centralPatronTypeMappings);
  }

  @Transactional(readOnly = true)
  @Override
  public Optional<Integer> getCentralPatronType(UUID centralServerId, String barcode) {
    return repository.findOneByCentralServerIdAndBarcode(centralServerId, barcode)
      .map(CentralPatronTypeMapping::getCentralPatronType);
  }

  @Override
  public CentralPatronTypeMappingsDTO updateCentralPatronTypeMappings(UUID centralServerId, CentralPatronTypeMappingsDTO centralPatronTypeMappingsDTO) {
    var stored = repository.findAll(mappingExampleWithServerId(centralServerId));

    var incoming = mapper.toEntities(centralPatronTypeMappingsDTO.getCentralPatronTypeMappings());
    var csRef = centralServerRef(centralServerId);
    incoming.forEach(setCentralServerRef(csRef).andThen(initId()));

    var saved = mergeAndSave(incoming, stored, repository, this::copyData);

    return mapper.toDTOCollection(saved);
  }

  private static Example<CentralPatronTypeMapping> mappingExampleWithServerId(UUID centralServerId) {
    var toFind = new CentralPatronTypeMapping();
    toFind.setCentralServer(centralServerRef(centralServerId));

    return Example.of(toFind);
  }

  private void copyData(CentralPatronTypeMapping from, CentralPatronTypeMapping to) {
    to.setCentralPatronType(from.getCentralPatronType());
    to.setBarcode(from.getBarcode());
  }

  private static Consumer<CentralPatronTypeMapping> setCentralServerRef(CentralServer centralServer) {
    return mapping -> mapping.setCentralServer(centralServer);
  }
}
