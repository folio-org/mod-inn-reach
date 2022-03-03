package org.folio.innreach.domain.service;

import java.util.UUID;

import org.folio.innreach.dto.LocationMappingsDTO;

public interface LocationMappingService {

  LocationMappingsDTO getMappingsByLibraryId(UUID centralServerId, UUID libraryId, int offset, int limit);

  LocationMappingsDTO getAllMappings(UUID centralServerId, int offset, int limit);

  LocationMappingsDTO updateAllMappings(UUID centralServerId, UUID libraryId, LocationMappingsDTO locationMappingsDTO);

}
