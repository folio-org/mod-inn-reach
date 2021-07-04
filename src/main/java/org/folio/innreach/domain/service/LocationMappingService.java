package org.folio.innreach.domain.service;

import java.util.UUID;

import org.folio.innreach.dto.LocationMappingsDTO;

public interface LocationMappingService {

  LocationMappingsDTO getAllMappings(UUID centralServerId, UUID libraryId, int offset, int limit);

  LocationMappingsDTO updateAllMappings(UUID centralServerId, UUID libraryId, LocationMappingsDTO locationMappingsDTO);

}
