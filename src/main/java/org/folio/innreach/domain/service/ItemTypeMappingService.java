package org.folio.innreach.domain.service;

import org.folio.innreach.dto.ItemTypeMappingsDTO;

import java.util.UUID;

public interface ItemTypeMappingService {
  ItemTypeMappingsDTO getAllMappings(UUID centralServerId, Integer offset, Integer limit);

  ItemTypeMappingsDTO updateAllMappings(UUID centralServerId, ItemTypeMappingsDTO itemTypeMappingsDTO);
}
