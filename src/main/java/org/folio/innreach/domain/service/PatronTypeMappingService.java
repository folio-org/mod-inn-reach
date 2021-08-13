package org.folio.innreach.domain.service;

import java.util.UUID;

import org.folio.innreach.dto.PatronTypeMappingsDTO;

public interface PatronTypeMappingService {
  PatronTypeMappingsDTO getAllMappings(UUID centralServerId, int offset, int limit);

  PatronTypeMappingsDTO updateAllMappings(UUID centralServerId, PatronTypeMappingsDTO patronTypeMappingsDTO);
}
