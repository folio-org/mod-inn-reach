package org.folio.innreach.client;

import org.folio.innreach.client.config.FolioFeignClientConfig;
import org.folio.innreach.domain.dto.folio.ResultList;
import org.folio.innreach.domain.dto.folio.Tenant;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@FeignClient(name = "okapi", configuration = FolioFeignClientConfig.class)
public interface OkapiClient {
  @GetMapping(value = "/proxy/tenants", produces = APPLICATION_JSON_VALUE)
  ResultList<Tenant> getTenantList();

}
