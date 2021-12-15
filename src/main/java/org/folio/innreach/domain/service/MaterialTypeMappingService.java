package org.folio.innreach.domain.service;

import java.util.List;
import java.util.UUID;

import org.folio.innreach.dto.MaterialTypeMappingDTO;
import org.folio.innreach.dto.MaterialTypeMappingsDTO;

public interface MaterialTypeMappingService {

  MaterialTypeMappingsDTO getAllMappings(UUID centralServerId, int offset, int limit);

  MaterialTypeMappingDTO getMapping(UUID centralServerId, UUID id);

  MaterialTypeMappingDTO createMapping(UUID centralServerId, MaterialTypeMappingDTO materialTypeMappingDTO);

  MaterialTypeMappingsDTO updateAllMappings(UUID centralServerId, MaterialTypeMappingsDTO materialTypeMappingsDTO);

  MaterialTypeMappingDTO updateMapping(UUID centralServerId, UUID id, MaterialTypeMappingDTO materialTypeMappingDTO);

  void deleteMapping(UUID centralServerId, UUID id);

  long countByTypeIds(UUID centralServerId, List<UUID> typeIds);

  MaterialTypeMappingDTO findByCentralServerAndMaterialType(UUID centralServerId, UUID materialTypeId);
}
