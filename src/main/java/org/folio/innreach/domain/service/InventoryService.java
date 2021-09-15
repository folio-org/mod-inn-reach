package org.folio.innreach.domain.service;

import java.util.List;
import java.util.UUID;

import org.folio.innreach.dto.Instance;
import org.folio.innreach.dto.Item;

public interface InventoryService {

  Instance getInstance(UUID id);

  List<Item> getItemsByInstanceId(UUID instanceId);

}
