package org.folio.innreach.domain.service;

import org.folio.innreach.dto.PatronTypeMappingsDTO;

import java.util.UUID;

public interface PatronTypeMappingService {
  PatronTypeMappingsDTO getAllMappings(UUID centralServerId, int offset, int limit);

  PatronTypeMappingsDTO updateAllMappings(UUID centralServerId, PatronTypeMappingsDTO patronTypeMappingsDTO);
}
