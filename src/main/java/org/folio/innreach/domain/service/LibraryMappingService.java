package org.folio.innreach.domain.service;

import java.util.UUID;

import org.folio.innreach.dto.LibraryMappingsDTO;

public interface LibraryMappingService {

  LibraryMappingsDTO getAllMappings(UUID centralServerId, int offset, int limit);

  LibraryMappingsDTO updateAllMappings(UUID centralServerId, LibraryMappingsDTO libraryMappingsDTO);
}
