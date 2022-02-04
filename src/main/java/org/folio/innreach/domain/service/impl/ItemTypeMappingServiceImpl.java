package org.folio.innreach.domain.service.impl;

import static org.folio.innreach.domain.service.impl.ServiceUtils.centralServerRef;
import static org.folio.innreach.domain.service.impl.ServiceUtils.initId;
import static org.folio.innreach.domain.service.impl.ServiceUtils.mergeAndSave;

import java.util.UUID;
import java.util.function.Consumer;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.folio.innreach.domain.entity.CentralServer;
import org.folio.innreach.domain.entity.ItemTypeMapping;
import org.folio.innreach.domain.exception.EntityNotFoundException;
import org.folio.innreach.domain.service.ItemTypeMappingService;
import org.folio.innreach.dto.ItemTypeMappingDTO;
import org.folio.innreach.dto.ItemTypeMappingsDTO;
import org.folio.innreach.mapper.ItemTypeMappingMapper;
import org.folio.innreach.repository.ItemTypeMappingRepository;
import org.folio.spring.data.OffsetRequest;

@RequiredArgsConstructor
@Service
@Transactional
public class ItemTypeMappingServiceImpl implements ItemTypeMappingService {

  private final ItemTypeMappingRepository repository;
  private final ItemTypeMappingMapper mapper;

  @Override
  @Transactional(readOnly = true)
  public ItemTypeMappingsDTO getAllMappings(UUID centralServerId, Integer offset, Integer limit) {
    var example = mappingExampleWithServerId(centralServerId);

    Page<ItemTypeMapping> mappings = repository.findAll(example, new OffsetRequest(offset, limit));

    return mapper.toDTOCollection(mappings);
  }

  @Override
  public ItemTypeMappingDTO getMappingByCentralType(UUID centralServerId, Integer centralItemType) {
    return repository.findByCentralServerIdAndCentralItemType(centralServerId, centralItemType)
      .map(mapper::toDTO)
      .orElseThrow(() -> new EntityNotFoundException(
        String.format("Item type mapping for central server: %s and type: %s not found", centralServerId, centralItemType)));
  }

  @Override
  public ItemTypeMappingsDTO updateAllMappings(UUID centralServerId, ItemTypeMappingsDTO itemTypeMappingsDTO) {
    var stored = repository.findAll(mappingExampleWithServerId(centralServerId));

    var incoming = mapper.toEntities(itemTypeMappingsDTO.getItemTypeMappings());
    var csRef = centralServerRef(centralServerId);
    incoming.forEach(setCentralServerRef(csRef).andThen(initId()));

    var saved = mergeAndSave(incoming, stored, repository, this::copyData);

    return mapper.toDTOCollection(saved);
  }

  private static Example<ItemTypeMapping> mappingExampleWithServerId(UUID centralServerId) {
    var toFind = new ItemTypeMapping();
    toFind.setCentralServer(centralServerRef(centralServerId));

    return Example.of(toFind);
  }

  private void copyData(ItemTypeMapping from, ItemTypeMapping to) {
    to.setCentralItemType(from.getCentralItemType());
    to.setMaterialTypeId(from.getMaterialTypeId());
  }

  private static Consumer<ItemTypeMapping> setCentralServerRef(CentralServer centralServer) {
    return mapping -> mapping.setCentralServer(centralServer);
  }
}
