package org.folio.innreach.domain.service;

import java.util.UUID;

import org.folio.innreach.dto.Instance;

public interface InventoryService {

  Instance getInstance(UUID id);

}
