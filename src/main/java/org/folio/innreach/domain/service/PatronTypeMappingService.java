package org.folio.innreach.domain.service;

import org.folio.innreach.dto.PatronTypeMappingDTO;
import org.folio.innreach.dto.PatronTypeMappingsDTO;

import java.util.UUID;

public interface PatronTypeMappingService {
  PatronTypeMappingsDTO getAll(UUID centralServerId, int offset, int limit);

  PatronTypeMappingDTO get(UUID centralServerId, UUID id);

  PatronTypeMappingDTO create(UUID centralServerId, PatronTypeMappingDTO patronTypeMappingDTO);

  PatronTypeMappingDTO update(UUID centralServerId, UUID id, PatronTypeMappingDTO patronTypeMappingDTO);

  void delete(UUID centralServerId, UUID id);

  PatronTypeMappingsDTO updateAll(UUID centralServerId, PatronTypeMappingsDTO patronTypeMappingsDTO);
}
