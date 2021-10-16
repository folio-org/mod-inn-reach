package org.folio.innreach.client;

import org.folio.innreach.config.FolioFeignClientConfig;
import org.folio.innreach.domain.dto.folio.ResultList;
import org.folio.innreach.domain.dto.folio.inventorystorage.ServicePointUserDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

@FeignClient(name = "inventory-storage", configuration = FolioFeignClientConfig.class)
public interface InventoryStorageClient {
  @GetMapping("/service-points-users?query=(userId=={userId})")
  ResultList<ServicePointUserDTO> findServicePointsUsers(@PathVariable("userId") UUID userId);
}
