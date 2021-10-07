package org.folio.innreach.client;

import java.util.UUID;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import org.folio.innreach.config.FolioFeignClientConfig;
import org.folio.innreach.domain.dto.folio.inventory.InventoryItemDTO;
import org.folio.innreach.dto.Instance;

@FeignClient(name = "inventory", configuration = FolioFeignClientConfig.class)
public interface InventoryClient {

  @GetMapping("/instances/{instanceId}")
  Instance getInstanceById(@PathVariable("instanceId") UUID instanceId);

  @GetMapping("/items/{itemId}")
  InventoryItemDTO getItemById(@PathVariable("itemId") UUID itemId);

  @GetMapping("/items/query==(hrid=\"{hrId}\")")
  InventoryItemDTO getItemByHrId(@PathVariable("hrId") String hrId);
}
