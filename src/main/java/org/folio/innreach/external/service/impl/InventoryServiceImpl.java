package org.folio.innreach.external.service.impl;

import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import org.folio.innreach.client.InventoryClient;
import org.folio.innreach.domain.dto.folio.inventory.InventoryItemDTO;
import org.folio.innreach.domain.exception.EntityNotFoundException;
import org.folio.innreach.external.service.InventoryService;

@RequiredArgsConstructor
@Service
public class InventoryServiceImpl implements InventoryService {

  private final InventoryClient client;

  @Override
  public InventoryItemDTO getItemByHrId(String hrid) {
    return client.getItemsByHrId(hrid).getResult()
      .stream().findFirst().orElseThrow( () -> new EntityNotFoundException("Item with hrid = " + hrid + " not found."));
  }

  @Override
  public InventoryItemDTO getItemByBarcode(String barcode) {
    return client.getItemByBarcode(barcode).getResult()
      .stream()
      .findFirst()
      .orElseThrow(() -> new EntityNotFoundException(String.format("Item with barcode [%s] not found", barcode)));
  }

  @Override
  public InventoryItemDTO getItemById(UUID id) {
    return client.getItemById(id);
  }

  @Override
  public void updateItem(UUID itemId, InventoryItemDTO inventoryItem) {
    client.updateItem(itemId, inventoryItem);
  }
}
