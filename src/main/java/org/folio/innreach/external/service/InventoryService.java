package org.folio.innreach.external.service;

import java.util.UUID;

import org.folio.innreach.domain.dto.folio.inventory.InventoryItemDTO;

public interface InventoryService {
  InventoryItemDTO getItemByHrId(String hrid);

  InventoryItemDTO getItemByBarcode(String barcode);

  InventoryItemDTO getItemById(UUID id);

  void updateItem(UUID itemId, InventoryItemDTO inventoryItem);
}
