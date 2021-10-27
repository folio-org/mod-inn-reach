package org.folio.innreach.external.service;

import org.folio.innreach.domain.dto.folio.inventory.InventoryItemDTO;

public interface InventoryService {
  InventoryItemDTO getItemByHrId(String hrid);
}
