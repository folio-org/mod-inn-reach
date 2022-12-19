package org.folio.innreach.domain.service.impl;

import static org.folio.innreach.domain.service.impl.ServiceUtils.centralServerRef;
import static org.folio.innreach.domain.service.impl.ServiceUtils.initId;
import static org.folio.innreach.domain.service.impl.ServiceUtils.mergeAndSave;

import java.util.UUID;
import java.util.function.Consumer;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.domain.Example;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.folio.innreach.domain.entity.CentralPatronTypeMapping;
import org.folio.innreach.domain.entity.CentralServer;
import org.folio.innreach.domain.service.CentralPatronTypeMappingService;
import org.folio.innreach.dto.CentralPatronTypeMappingsDTO;
import org.folio.innreach.mapper.CentralPatronTypeMappingMapper;
import org.folio.innreach.repository.CentralPatronTypeMappingRepository;
import org.folio.spring.data.OffsetRequest;

@Log4j2
@Service
@RequiredArgsConstructor
@Transactional
public class CentralPatronTypeMappingServiceImpl implements CentralPatronTypeMappingService {

  private final CentralPatronTypeMappingRepository repository;
  private final CentralPatronTypeMappingMapper mapper;

  @Override
  @Transactional(readOnly = true)
  public CentralPatronTypeMappingsDTO getCentralPatronTypeMappings(UUID centralServerId, Integer offset, Integer limit) {
    log.debug("getCentralPatronTypeMappings:: parameters centralServerId: {}, offset: {}, limit: {}", centralServerId, offset, limit);
    var centralPatronTypeMappingExample = mappingExampleWithServerId(centralServerId);

    var centralPatronTypeMappings = repository.findAll(centralPatronTypeMappingExample, new OffsetRequest(offset, limit));

    log.info("getCentralPatronTypeMappings:: result: {}", mapper.toDTOCollection(centralPatronTypeMappings));
    return mapper.toDTOCollection(centralPatronTypeMappings);
  }

  @Override
  public CentralPatronTypeMappingsDTO updateCentralPatronTypeMappings(UUID centralServerId, CentralPatronTypeMappingsDTO centralPatronTypeMappingsDTO) {
    log.debug("updateCentralPatronTypeMappings:: parameters centralServerId: {}, centralPatronTypeMappingsDTO: {}", centralServerId, centralPatronTypeMappingsDTO);
    var stored = repository.findAll(mappingExampleWithServerId(centralServerId));

    var incoming = mapper.toEntities(centralPatronTypeMappingsDTO.getCentralPatronTypeMappings());
    var csRef = centralServerRef(centralServerId);
    incoming.forEach(setCentralServerRef(csRef).andThen(initId()));

    var saved = mergeAndSave(incoming, stored, repository, this::copyData);

    log.info("updateCentralPatronTypeMappings:: result: {}", mapper.toDTOCollection(saved));
    return mapper.toDTOCollection(saved);
  }

  private static Example<CentralPatronTypeMapping> mappingExampleWithServerId(UUID centralServerId) {
    log.debug("mappingExampleWithServerId:: parameters centralServerId: {}", centralServerId);
    var toFind = new CentralPatronTypeMapping();
    toFind.setCentralServer(centralServerRef(centralServerId));

    log.info("mappingExampleWithServerId:: result: {}", Example.of(toFind));
    return Example.of(toFind);
  }

  private void copyData(CentralPatronTypeMapping from, CentralPatronTypeMapping to) {
    log.debug("copyData:: parameters from: {}, to: {}", from, to);
    to.setCentralPatronType(from.getCentralPatronType());
    to.setBarcode(from.getBarcode());
  }

  private static Consumer<CentralPatronTypeMapping> setCentralServerRef(CentralServer centralServer) {
    log.debug("setCentralServerRef:: parameters centralServer: {}", centralServer);
    return mapping -> mapping.setCentralServer(centralServer);
  }
}
