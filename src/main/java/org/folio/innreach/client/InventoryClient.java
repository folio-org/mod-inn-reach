package org.folio.innreach.client;

import java.util.UUID;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import org.folio.innreach.config.FolioFeignClientConfig;
import org.folio.innreach.domain.dto.folio.ResultList;
import org.folio.innreach.domain.dto.folio.inventory.InventoryItemDTO;
import org.folio.innreach.dto.Instance;
import org.folio.innreach.dto.Item;

@FeignClient(name = "inventory", configuration = FolioFeignClientConfig.class)
public interface InventoryClient {

  @GetMapping("/instances/{instanceId}")
  Instance getInstanceById(@PathVariable("instanceId") UUID instanceId);

  @GetMapping("/instances?query=(hrid=={hrid})")
  ResultList<Instance> queryInstanceByHrid(@PathVariable("hrid") String hrid);

  @GetMapping("/items/{itemId}")
  InventoryItemDTO getItemById(@PathVariable("itemId") UUID itemId);

  @GetMapping("/items?query=hrid=={hrId}")
  ResultList<InventoryItemDTO> getItemsByHrId(@PathVariable("hrId") String hrId);

  @GetMapping("/items/query=(hrid==\"{hrId}\")")
  InventoryItemDTO getItemByHrId(@PathVariable("hrId") String hrId);

  @PostMapping("/instances")
  Instance createInstance(@RequestBody Instance instance);

  @PostMapping("/items")
  Item createItem(@RequestBody Item item);

}
