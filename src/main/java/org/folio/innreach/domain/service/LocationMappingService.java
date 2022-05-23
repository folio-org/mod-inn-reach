package org.folio.innreach.domain.service;

import java.util.List;
import java.util.UUID;

import org.folio.innreach.dto.LocationMappingDTO;
import org.folio.innreach.dto.LocationMappingsDTO;

public interface LocationMappingService {

  LocationMappingsDTO getMappingsByLibraryId(UUID centralServerId, UUID libraryId, int offset, int limit);

  List<LocationMappingDTO> getAllMappings(UUID centralServerId);

  LocationMappingsDTO updateAllMappings(UUID centralServerId, UUID libraryId, LocationMappingsDTO locationMappingsDTO);

}
