package org.folio.innreach.external.client.feign;

import org.folio.innreach.domain.dto.folio.inventoryStorage.InstanceIterationRequest;
import org.folio.innreach.external.client.feign.config.FolioFeignClientConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "inventory-storage", configuration = FolioFeignClientConfig.class)
public interface InventoryStorageClient {

  @PostMapping("/instance-storage/instances/iteration")
  void startInitialContribution(@RequestBody InstanceIterationRequest request);
}
