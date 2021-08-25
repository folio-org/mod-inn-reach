package org.folio.innreach.client;

import java.util.UUID;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import org.folio.innreach.domain.dto.folio.inventory.InventoryInstanceDTO;
import org.folio.innreach.domain.dto.folio.inventory.InventoryItemDTO;

@FeignClient("inventory")
public interface InventoryClient {

  @GetMapping("/instances/{instanceId}")
  InventoryInstanceDTO getInstanceById(@PathVariable("instanceId") UUID instanceId);

  @GetMapping("/items/{itemId}")
  InventoryItemDTO getItemById(@PathVariable("itemId") UUID itemId);
}
