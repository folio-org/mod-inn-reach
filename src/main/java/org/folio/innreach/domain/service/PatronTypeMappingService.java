package org.folio.innreach.domain.service;

import org.folio.innreach.dto.PatronTypeMappingsDTO;

import java.util.UUID;

public interface PatronTypeMappingService {
  PatronTypeMappingsDTO getAll(UUID centralServerId, int offset, int limit);

  PatronTypeMappingsDTO updateAll(UUID centralServerId, PatronTypeMappingsDTO patronTypeMappingsDTO);
}
