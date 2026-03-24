package org.folio.innreach.client;

import org.folio.innreach.client.config.FolioFeignClientConfig;
import org.folio.innreach.domain.dto.folio.configuration.ConfigurationDTO;
import org.folio.innreach.domain.dto.folio.ResultList;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "configurations", configuration = FolioFeignClientConfig.class, dismiss404 = true)
public interface ConfigurationClient {

  @GetMapping("/entries?query=(module={module} and configName=other_settings)")
  ResultList<ConfigurationDTO> queryRequestByModule(@PathVariable("module") String module);

}
