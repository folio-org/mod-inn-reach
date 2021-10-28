package org.folio.innreach.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import org.folio.innreach.config.FolioFeignClientConfig;
import org.folio.innreach.dto.Holding;

@FeignClient(name = "holdings-storage", configuration = FolioFeignClientConfig.class)
public interface HoldingsStorageClient {

  @PostMapping("/holdings")
  Holding createHolding(@RequestBody Holding holding);

}
