package org.folio.innreach.client;

import org.folio.innreach.domain.dto.folio.inventory.InventoryInstanceDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

@FeignClient("inventory")
public interface InventoryClient {

  @GetMapping("/inventory/instances/{id}")
  InventoryInstanceDTO getInstanceById(@PathVariable("id") UUID instanceId);
}
