package org.folio.innreach.client;

import java.util.Optional;
import java.util.UUID;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

import org.folio.innreach.client.config.InventoryFeignClientConfig;
import org.folio.innreach.dto.Holding;

@FeignClient(name = "holdings-storage", configuration = InventoryFeignClientConfig.class, decode404 = true)
public interface HoldingsStorageClient {

  @GetMapping("/holdings/{holdingId}")
  Optional<Holding> findHolding(@PathVariable("holdingId") UUID holdingId);

  @PostMapping("/holdings")
  Holding createHolding(@RequestBody Holding holding);

  @PutMapping("/holdings/{holdingId}")
  Holding updateHolding(@PathVariable UUID holdingId, @RequestBody Holding holding);

}
