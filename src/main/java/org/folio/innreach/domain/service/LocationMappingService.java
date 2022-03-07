package org.folio.innreach.domain.service;

import java.util.List;
import java.util.UUID;

import org.folio.innreach.dto.LocationMappingForAllLibrariesDTO;
import org.folio.innreach.dto.LocationMappingsForOneLibraryDTO;

public interface LocationMappingService {

  LocationMappingsForOneLibraryDTO getMappingsByLibraryId(UUID centralServerId, UUID libraryId, int offset, int limit);

  List<LocationMappingForAllLibrariesDTO> getAllMappings(UUID centralServerId);

  LocationMappingsForOneLibraryDTO updateAllMappings(UUID centralServerId, UUID libraryId, LocationMappingsForOneLibraryDTO locationMappingsDTO);

}
