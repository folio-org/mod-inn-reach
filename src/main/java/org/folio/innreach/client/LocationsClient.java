package org.folio.innreach.client;

import java.util.UUID;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import org.folio.innreach.client.config.FolioFeignClientConfig;
import org.folio.innreach.domain.dto.folio.ResultList;
import org.folio.innreach.domain.dto.folio.inventorystorage.LocationDTO;

@FeignClient(name = "locations", configuration = FolioFeignClientConfig.class)
public interface LocationsClient {

  @GetMapping
  ResultList<LocationDTO> getLocations(@RequestParam("limit") int limit);

  @GetMapping("?query=primaryServicePoint=={servicePointId}")
  ResultList<LocationDTO> queryLocationsByServicePoint(@PathVariable("servicePointId") UUID servicePointId, @RequestParam("limit") int limit);

}
