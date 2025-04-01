package org.folio.innreach.client;

import java.util.Optional;
import java.util.UUID;

import jakarta.validation.Valid;
import org.folio.innreach.domain.dto.CQLQueryRequestDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import org.folio.innreach.client.config.InventoryFeignClientConfig;
import org.folio.innreach.domain.dto.folio.ResultList;
import org.folio.innreach.domain.dto.folio.inventory.InventoryInstanceDTO;
import org.folio.innreach.domain.dto.folio.inventory.InventoryItemDTO;

@FeignClient(name = "inventory", configuration = InventoryFeignClientConfig.class, dismiss404 = true)
public interface InventoryClient {

  @GetMapping("/items?query=hrid=={hrId}")
  ResultList<InventoryItemDTO> getItemsByHrId(@PathVariable("hrId") String hrId);

  @PostMapping("/instances")
  void createInstance(@RequestBody InventoryInstanceDTO instance);

  @GetMapping("/instances?query=(hrid=={hrid})")
  ResultList<InventoryInstanceDTO> queryInstanceByHrid(@PathVariable("hrid") String hrid);

  @PostMapping("/items")
  InventoryItemDTO createItem(@RequestBody InventoryItemDTO item);

  @PutMapping("/items/{itemId}")
  void updateItem(@PathVariable("itemId") UUID itemId, @RequestBody InventoryItemDTO item);

  @DeleteMapping("/items/{itemId}")
  void deleteItem(@PathVariable("itemId") UUID itemId);

  @GetMapping("/items/{itemId}")
  Optional<InventoryItemDTO> findItem(@PathVariable("itemId") UUID itemId);

  @GetMapping("/instances/{instanceId}")
  Optional<InventoryInstanceDTO> findInstance(@PathVariable("instanceId") UUID instanceId);

  @DeleteMapping("/instances/{instanceId}")
  void deleteInstance(@PathVariable("instanceId") UUID instanceId);

  @GetMapping("/items?query=barcode=={barcode}")
  ResultList<InventoryItemDTO> getItemByBarcode(@PathVariable("barcode") String barcode);

  @GetMapping("/items?query=id=({itemIds}) and effectiveLocationId=({locationIds})")
  ResultList<InventoryItemDTO> queryItemsByIdsAndLocations(@PathVariable("itemIds") String itemIdKey,
                                                           @PathVariable("locationIds") String locationIdKey,
                                                           @RequestParam("limit") int limit);
  @PostMapping("/items/retrieve")
  ResultList<InventoryItemDTO> retrieveItemsByCQLBody(@Valid @RequestBody CQLQueryRequestDto cqlQueryRequestDto);

  @GetMapping("/instances?query=id=({instanceIds})")
  ResultList<InventoryInstanceDTO> queryInstancesByIds(@PathVariable("instanceIds") String instanceIdKey, @RequestParam("limit") int limit);

}
