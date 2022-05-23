package org.folio.innreach.domain.service;

import java.util.Optional;
import java.util.UUID;

import org.folio.innreach.domain.dto.folio.inventory.InventoryItemDTO;

public interface ItemService extends BasicService<UUID, InventoryItemDTO> {

  InventoryItemDTO getItemByHrId(String hrid);

  Optional<InventoryItemDTO> findItemByBarcode(String barcode);

}
