package org.folio.innreach.domain.service.impl;

import java.util.Map;
import java.util.UUID;

public interface FolioLocationService {

  Map<UUID, UUID> getLocationLibraryMappings();

}
