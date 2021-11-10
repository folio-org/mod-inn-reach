package org.folio.innreach.client;

import java.util.Optional;
import java.util.UUID;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

import org.folio.innreach.config.FolioFeignClientConfig;
import org.folio.innreach.domain.dto.folio.ResultList;
import org.folio.innreach.domain.dto.folio.inventory.InventoryInstanceDTO;
import org.folio.innreach.domain.dto.folio.inventory.InventoryItemDTO;

@FeignClient(name = "inventory", configuration = FolioFeignClientConfig.class, decode404 = true)
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
  InventoryItemDTO updateItem(@PathVariable("itemId") UUID itemId, @RequestBody InventoryItemDTO item);

  @GetMapping("/items/{itemId}")
  Optional<InventoryItemDTO> findItem(@PathVariable("itemId") UUID itemId);

  @GetMapping("/instances/{instanceId}")
  Optional<InventoryInstanceDTO> findInstance(@PathVariable("instanceId") UUID instanceId);

}
