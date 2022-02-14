package org.folio.innreach.domain.service.impl;

import org.folio.innreach.domain.dto.folio.inventorystorage.LocationDTO;

import java.util.Map;
import java.util.UUID;

public interface FolioLocationService {

  Map<UUID, UUID> getLocationLibraryMappings();

  LocationDTO getLocationById(UUID locationId);
}
