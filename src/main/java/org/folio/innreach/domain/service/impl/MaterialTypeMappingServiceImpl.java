package org.folio.innreach.domain.service.impl;

import static java.util.stream.Collectors.toList;

import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
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

@RequiredArgsConstructor
@Service
@Transactional
public class MaterialTypeMappingServiceImpl implements MaterialTypeMappingService {

  private final MaterialTypeMappingRepository repository;
  private final MaterialTypeMappingMapper mapper;

  
  @Override
  @Transactional(readOnly = true)
  public MaterialTypeMappingsDTO getAllMappings(UUID centralServerId, Integer offset, Integer limit) {
    var example = mappingExampleWithServerId(centralServerId);

    Page<MaterialTypeMapping> mappings = repository.findAll(example, PageRequest.of(offset, limit));

    var result = new MaterialTypeMappingsDTO();
    result.setMappings(mappings.stream()
        .map(mapper::mapToDTO)
        .collect(toList()));
    
    result.setTotalRecords((int) mappings.getTotalElements());

    return result;
  }

  @Override
  @Transactional(readOnly = true)
  public MaterialTypeMappingDTO getMapping(UUID centralServerId, UUID id) {
    var mapping = findMapping(centralServerId, id);

    return mapper.mapToDTO(mapping);
  }

  @Override
  public MaterialTypeMappingDTO createMapping(UUID centralServerId, MaterialTypeMappingDTO dto) {
    var entity = mapper.mapToEntity(dto);
    entity.setCentralServer(centralServerRef(centralServerId));

    var saved = repository.save(entity);

    return mapper.mapToDTO(saved);
  }

  @Override
  public MaterialTypeMappingDTO updateMapping(UUID centralServerId, UUID id,
      MaterialTypeMappingDTO dto) {
    var mapping = findMapping(centralServerId, id);

    mapping.setMaterialTypeId(dto.getMaterialTypeId());
    mapping.setCentralItemType(dto.getCentralItemType());

    return mapper.mapToDTO(mapping);
  }

  @Override
  public void deleteMapping(UUID centralServerId, UUID id) {
    MaterialTypeMapping mapping = findMapping(centralServerId, id);
    repository.delete(mapping);
  }

  private MaterialTypeMapping findMapping(UUID centralServerId, UUID id) {
    return repository.findOne(mappingExampleWithServerIdAndId(centralServerId, id))
        .orElseThrow(() -> new EntityNotFoundException("Material type mapping not found: id = " + id +
            ", centralServerId = " + centralServerId));
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

  private static CentralServer centralServerRef(UUID centralServerId) {
    var server = new CentralServer();
    server.setId(centralServerId);

    return server;
  }

}