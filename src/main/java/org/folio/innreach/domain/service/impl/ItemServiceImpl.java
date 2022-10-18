package org.folio.innreach.domain.service.impl;

import static org.folio.innreach.util.CqlHelper.matchAny;
import static org.folio.innreach.util.ListUtils.getFirstItem;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import org.folio.innreach.client.InventoryClient;
import org.folio.innreach.domain.dto.folio.inventory.InventoryItemDTO;
import org.folio.innreach.domain.service.ItemService;
@Log4j2
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
  public void delete(UUID itemId) {
      inventoryClient.findItem(itemId).
        ifPresentOrElse(p -> inventoryClient.deleteItem(itemId),
          () -> log.info("No item found with itemId:" + itemId));
  }

  @Override
  public InventoryItemDTO getItemByHrId(String hrid) {
    return getFirstItem(inventoryClient.getItemsByHrId(hrid))
      .orElseThrow(() -> new IllegalArgumentException("Item with hrid = " + hrid + " not found."));
  }

  @Override
  public Optional<InventoryItemDTO> findItemByBarcode(String barcode) {
    return findItem(barcode);
  }

  @Override
  public List<InventoryItemDTO> findItemsByIdsAndLocations(Set<UUID> itemIds, Set<UUID> locationIds, int limit) {
    var itemIdKey = matchAny(itemIds);
    var locationIdKey = matchAny(locationIds);

    return inventoryClient.queryItemsByIdsAndLocations(itemIdKey, locationIdKey, limit).getResult();
  }

  private Optional<InventoryItemDTO> findItem(String barcode) {
    return inventoryClient.getItemByBarcode(barcode).getResult()
      .stream()
      .findFirst();
  }

}
