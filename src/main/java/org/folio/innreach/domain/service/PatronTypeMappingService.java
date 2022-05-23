package org.folio.innreach.domain.service;

import java.util.Optional;
import java.util.UUID;

import org.folio.innreach.dto.PatronTypeMappingsDTO;

public interface PatronTypeMappingService {
  PatronTypeMappingsDTO getAllMappings(UUID centralServerId, int offset, int limit);

  PatronTypeMappingsDTO updateAllMappings(UUID centralServerId, PatronTypeMappingsDTO patronTypeMappingsDTO);

  Optional<Integer> getCentralPatronType(UUID centralServerId, UUID patronGroupId);

}
