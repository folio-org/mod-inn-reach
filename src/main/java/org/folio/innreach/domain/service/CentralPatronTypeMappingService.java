package org.folio.innreach.domain.service;

import java.util.UUID;

import org.folio.innreach.dto.CentralPatronTypeMappingsDTO;

public interface CentralPatronTypeMappingService {

  CentralPatronTypeMappingsDTO getCentralPatronTypeMappings(UUID centralServerId, Integer offset, Integer limit);

  CentralPatronTypeMappingsDTO updateCentralPatronTypeMappings(UUID centralServerId, CentralPatronTypeMappingsDTO centralPatronTypeMappingsDTO);
}
