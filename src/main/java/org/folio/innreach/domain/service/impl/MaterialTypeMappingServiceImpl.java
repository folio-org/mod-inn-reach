package org.folio.innreach.domain.service.impl;

import static org.folio.innreach.domain.service.impl.ServiceUtils.centralServerRef;
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
import org.folio.innreach.domain.entity.MaterialTypeMapping;
import org.folio.innreach.domain.exception.EntityNotFoundException;
import org.folio.innreach.domain.service.MaterialTypeMappingService;
import org.folio.innreach.dto.MaterialTypeMappingDTO;
import org.folio.innreach.dto.MaterialTypeMappingsDTO;
import org.folio.innreach.mapper.MaterialTypeMappingMapper;
import org.folio.innreach.repository.MaterialTypeMappingRepository;
import org.folio.spring.data.OffsetRequest;

@Log4j2
@RequiredArgsConstructor
@Service
@Transactional
public class MaterialTypeMappingServiceImpl implements MaterialTypeMappingService {

  private final MaterialTypeMappingRepository repository;
  private final MaterialTypeMappingMapper mapper;


  @Override
  @Transactional(readOnly = true)
  public MaterialTypeMappingsDTO getAllMappings(UUID centralServerId, int offset, int limit) {
    log.debug("getAllMappings:: parameters centralServerId: {}, offset: {}, limit: {}", centralServerId, offset, limit);
    var example = mappingExampleWithServerId(centralServerId);

    Page<MaterialTypeMapping> mappings = repository.findAll(example, new OffsetRequest(offset, limit));

    log.info("getAllMappings:: result: {}", mapper.toDTOCollection(mappings));
    return mapper.toDTOCollection(mappings);
  }

  @Override
  @Transactional(readOnly = true)
  public MaterialTypeMappingDTO getMapping(UUID centralServerId, UUID id) {
    log.debug("getMapping:: parameters centralServerId: {}, id: {}", centralServerId, id);
    var mapping = findMapping(centralServerId, id);

    log.info("getMapping:: result: {}", mapper.toDTO(mapping));
    return mapper.toDTO(mapping);
  }

  @Override
  public MaterialTypeMappingDTO createMapping(UUID centralServerId, MaterialTypeMappingDTO dto) {
    log.debug("createMapping:: parameters centralServerId: {}, dto: {}", centralServerId, dto);
    var entity = mapper.toEntity(dto);
    entity.setCentralServer(centralServerRef(centralServerId));

    var saved = repository.save(entity);

    log.info("createMapping:: result: {}", mapper.toDTO(saved));
    return mapper.toDTO(saved);
  }

  @Override
  public MaterialTypeMappingDTO updateMapping(UUID centralServerId, UUID id,
      MaterialTypeMappingDTO dto) {
    log.debug("updateMapping:: parameters centralServerId: {}, id: {}", centralServerId, id);
    var mapping = findMapping(centralServerId, id);

    mapping.setMaterialTypeId(dto.getMaterialTypeId());
    mapping.setCentralItemType(dto.getCentralItemType());

    log.info("updateMapping:: result: {}", mapper.toDTO(mapping));
    return mapper.toDTO(mapping);
  }

  @Override
  public MaterialTypeMappingsDTO updateAllMappings(UUID centralServerId,
      MaterialTypeMappingsDTO materialTypeMappingsDTO) {
    log.debug("updateAllMappings:: parameters centralServerId: {}, materialTypeMappingsDTO: {}", centralServerId, materialTypeMappingsDTO);
    var stored = repository.findAll(mappingExampleWithServerId(centralServerId));

    var incoming = mapper.toEntities(materialTypeMappingsDTO.getMaterialTypeMappings());
    var csRef = centralServerRef(centralServerId);
    incoming.forEach(setCentralServerRef(csRef).andThen(initId()));

    var saved = mergeAndSave(incoming, stored, repository, this::copyData);

    log.info("updateAllMappings:: result: {}", mapper.toDTOCollection(saved));
    return mapper.toDTOCollection(saved);
  }

  @Override
  public void deleteMapping(UUID centralServerId, UUID id) {
    MaterialTypeMapping mapping = findMapping(centralServerId, id);
    repository.delete(mapping);
  }

  @Override
  public long countByTypeIds(UUID centralServerId, List<UUID> typeIds) {
    return repository.countByCentralServerIdAndMaterialTypeIdIn(centralServerId, typeIds);
  }

  @Override
  public MaterialTypeMappingDTO findByCentralServerAndMaterialType(UUID centralServerId, UUID materialTypeId){
    log.debug("findByCentralServerAndMaterialType:: parameters centralServerId: {}, materialTypeId: {}", centralServerId, materialTypeId);
    var materialType = repository.findOneByCentralServerIdAndMaterialTypeId(
      centralServerId, materialTypeId).orElseThrow(
      () -> new EntityNotFoundException("Material type mapping for central server id = " + centralServerId
        + " and material type id = " + materialTypeId + " not found")
    );
    log.info("findByCentralServerAndMaterialType:: result: {}", mapper.toDTO(materialType));
    return mapper.toDTO(materialType);
  }

  private MaterialTypeMapping findMapping(UUID centralServerId, UUID id) {
    return repository.findOne(mappingExampleWithServerIdAndId(centralServerId, id))
        .orElseThrow(() -> new EntityNotFoundException("Material type mapping not found: id = " + id +
            ", centralServerId = " + centralServerId));
  }

  private void copyData(MaterialTypeMapping from, MaterialTypeMapping to) {
    to.setMaterialTypeId(from.getMaterialTypeId());
    to.setCentralItemType(from.getCentralItemType());
  }

  private static Consumer<MaterialTypeMapping> setCentralServerRef(CentralServer centralServer) {
    return mapping -> mapping.setCentralServer(centralServer);
  }

  private static Example<MaterialTypeMapping> mappingExampleWithServerId(UUID centralServerId) {
    var toFind = new MaterialTypeMapping();
    toFind.setCentralServer(centralServerRef(centralServerId));

    return Example.of(toFind);
  }

  private static Example<MaterialTypeMapping> mappingExampleWithServerIdAndId(UUID centralServerId, UUID id) {
    var toFind = new MaterialTypeMapping();
    toFind.setId(id);
    toFind.setCentralServer(centralServerRef(centralServerId));

    return Example.of(toFind);
  }

}
