package org.folio.innreach.domain.service;

import java.util.UUID;

import org.folio.innreach.dto.ItemTypeMappingsDTO;

public interface ItemTypeMappingService {
  ItemTypeMappingsDTO getAllMappings(UUID centralServerId, Integer offset, Integer limit);

  ItemTypeMappingsDTO updateAllMappings(UUID centralServerId, ItemTypeMappingsDTO itemTypeMappingsDTO);
}
