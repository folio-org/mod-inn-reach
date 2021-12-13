package org.folio.innreach.domain.service.impl;

import static org.folio.innreach.util.ListUtils.getFirstItem;

import java.util.Optional;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import org.folio.innreach.client.InventoryClient;
import org.folio.innreach.domain.dto.folio.inventory.InventoryItemDTO;
import org.folio.innreach.domain.service.ItemService;

@Service
@RequiredArgsConstructor
public class ItemServiceImpl implements ItemService {

  private final InventoryClient inventoryClient;

  @Override
  public InventoryItemDTO create(InventoryItemDTO item) {
    return inventoryClient.createItem(item);
  }

  @Override
  public InventoryItemDTO update(InventoryItemDTO item) {
    inventoryClient.updateItem(item.getId(), item);
    return item;
  }

  @Override
  public Optional<InventoryItemDTO> find(UUID itemId) {
    return inventoryClient.findItem(itemId);
  }

  @Override
  public InventoryItemDTO getItemByHrId(String hrid) {
    return getFirstItem(inventoryClient.getItemsByHrId(hrid))
        .orElseThrow(() -> new IllegalArgumentException("Item with hrid = " + hrid + " not found."));
  }

  @Override
  public InventoryItemDTO getItemByBarcode(String barcode) {
    return inventoryClient.getItemByBarcode(barcode).getResult()
        .stream()
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("Item with barcode = " + barcode + " not found"));
  }

  @Override
  public Optional<InventoryItemDTO> findItemByBarcode(String barcode) {
    return findItem(barcode);
  }

  private Optional<InventoryItemDTO> findItem(String barcode) {
    return inventoryClient.getItemByBarcode(barcode).getResult()
      .stream()
      .findFirst();
  }

}
