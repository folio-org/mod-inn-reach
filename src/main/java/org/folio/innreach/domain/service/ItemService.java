package org.folio.innreach.domain.service;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.folio.innreach.domain.dto.folio.inventory.InventoryItemDTO;

public interface ItemService extends BasicService<UUID, InventoryItemDTO> {

  InventoryItemDTO getItemByHrId(String hrid);

  Optional<InventoryItemDTO> findItemByBarcode(String barcode);

  List<InventoryItemDTO> findItemsByIdsAndLocations(Set<UUID> itemIds, Set<UUID> locationIds, int limit);
}
