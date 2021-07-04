package org.folio.innreach.domain.service;

import java.util.UUID;

import org.folio.innreach.dto.MaterialTypeMappingDTO;
import org.folio.innreach.dto.MaterialTypeMappingsDTO;

public interface MaterialTypeMappingService {

  MaterialTypeMappingsDTO getAllMappings(UUID centralServerId, int offset, int limit);

  MaterialTypeMappingDTO getMapping(UUID centralServerId, UUID id);

  MaterialTypeMappingDTO createMapping(UUID centralServerId, MaterialTypeMappingDTO materialTypeMappingDTO);

  MaterialTypeMappingDTO updateMapping(UUID centralServerId, UUID id, MaterialTypeMappingDTO materialTypeMappingDTO);

  void deleteMapping(UUID centralServerId, UUID id);
}
