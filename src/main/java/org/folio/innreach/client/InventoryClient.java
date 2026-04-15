package org.folio.innreach.client;

import java.util.Optional;
import java.util.UUID;

import jakarta.validation.Valid;
import org.folio.innreach.domain.dto.CQLQueryRequestDto;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.DeleteExchange;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;
import org.springframework.web.service.annotation.PutExchange;

import org.folio.innreach.domain.dto.folio.ResultList;
import org.folio.innreach.domain.dto.folio.inventory.InventoryInstanceDTO;
import org.folio.innreach.domain.dto.folio.inventory.InventoryItemDTO;

@HttpExchange("inventory")
public interface InventoryClient {

  @GetExchange("/items?query=hrid=={hrId}")
  ResultList<InventoryItemDTO> getItemsByHrId(@PathVariable("hrId") String hrId);

  @PostExchange("/instances")
  void createInstance(@RequestBody InventoryInstanceDTO instance);

  @GetExchange("/instances?query=(hrid=={hrid})")
  ResultList<InventoryInstanceDTO> queryInstanceByHrid(@PathVariable("hrid") String hrid);

  @PostExchange("/items")
  InventoryItemDTO createItem(@RequestBody InventoryItemDTO item);

  @PutExchange("/items/{itemId}")
  void updateItem(@PathVariable("itemId") UUID itemId, @RequestBody InventoryItemDTO item);

  @DeleteExchange("/items/{itemId}")
  void deleteItem(@PathVariable("itemId") UUID itemId);

  @GetExchange("/items/{itemId}")
  Optional<InventoryItemDTO> findItem(@PathVariable("itemId") UUID itemId);

  @GetExchange("/instances/{instanceId}")
  Optional<InventoryInstanceDTO> findInstance(@PathVariable("instanceId") UUID instanceId);

  @DeleteExchange("/instances/{instanceId}")
  void deleteInstance(@PathVariable("instanceId") UUID instanceId);

  @GetExchange("/items?query=barcode=={barcode}")
  ResultList<InventoryItemDTO> getItemByBarcode(@PathVariable("barcode") String barcode);

  @GetExchange("/items?query=id=({itemIds}) and effectiveLocationId=({locationIds})")
  ResultList<InventoryItemDTO> queryItemsByIdsAndLocations(@PathVariable("itemIds") String itemIdKey,
                                                           @PathVariable("locationIds") String locationIdKey,
                                                           @RequestParam("limit") int limit);
  @PostExchange("/items/retrieve")
  ResultList<InventoryItemDTO> retrieveItemsByCQLBody(@Valid @RequestBody CQLQueryRequestDto cqlQueryRequestDto);

  @GetExchange("/instances?query=id=({instanceIds})")
  ResultList<InventoryInstanceDTO> queryInstancesByIds(@PathVariable("instanceIds") String instanceIdKey, @RequestParam("limit") int limit);
}
