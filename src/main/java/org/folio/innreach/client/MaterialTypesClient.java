package org.folio.innreach.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import org.folio.innreach.client.config.FolioFeignClientConfig;
import org.folio.innreach.domain.dto.folio.ResultList;
import org.folio.innreach.domain.dto.folio.inventorystorage.MaterialTypeDTO;

@FeignClient(name = "material-types", configuration = FolioFeignClientConfig.class)
public interface MaterialTypesClient {

  @GetMapping
  ResultList<MaterialTypeDTO> getMaterialTypes(@RequestParam("query") String query, @RequestParam("limit") int limit);

}
