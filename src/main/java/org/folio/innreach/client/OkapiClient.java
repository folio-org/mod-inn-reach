package org.folio.innreach.client;

import org.folio.innreach.client.config.FolioFeignClientConfig;
import org.folio.innreach.domain.dto.folio.ResultList;
import org.folio.innreach.domain.dto.folio.Tenant;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

@FeignClient(name = "_/proxy/tenants", configuration = FolioFeignClientConfig.class)
public interface OkapiClient {
  @GetMapping
  ResultList<Tenant> getTenantList();

}
