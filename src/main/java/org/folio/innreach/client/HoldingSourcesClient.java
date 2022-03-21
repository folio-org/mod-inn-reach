package org.folio.innreach.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import org.folio.innreach.client.config.FolioFeignClientConfig;
import org.folio.innreach.domain.dto.folio.ResultList;
import org.folio.innreach.dto.HoldingSourceDTO;

@FeignClient(name = "holdings-sources", configuration = FolioFeignClientConfig.class)
public interface HoldingSourcesClient {

  @GetMapping("?query=name=={sourceName}&limit=1")
  ResultList<HoldingSourceDTO> querySourceByName(@PathVariable("sourceName") String sourceName);

}
