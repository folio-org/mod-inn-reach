package org.folio.innreach.client;

import org.folio.innreach.domain.dto.folio.configuration.ConfigurationDTO;
import org.folio.innreach.domain.dto.folio.ResultList;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

@HttpExchange("configurations")
public interface ConfigurationClient {

  @GetExchange("/entries?query=(module={module} and configName=other_settings)")
  ResultList<ConfigurationDTO> queryRequestByModule(@PathVariable("module") String module);

}
