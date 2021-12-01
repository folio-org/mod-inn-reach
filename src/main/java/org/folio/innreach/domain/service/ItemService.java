package org.folio.innreach.domain.service;

import java.util.UUID;

import org.folio.innreach.domain.dto.folio.inventory.InventoryItemDTO;

public interface ItemService extends RetryableUpdateTemplate<UUID, InventoryItemDTO>,
                                      BasicService<UUID, InventoryItemDTO> {

  InventoryItemDTO getItemByHrId(String hrid);

  InventoryItemDTO getItemByBarcode(String barcode);

}
