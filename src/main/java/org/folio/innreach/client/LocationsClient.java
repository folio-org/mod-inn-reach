package org.folio.innreach.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import org.folio.innreach.client.config.FolioFeignClientConfig;
import org.folio.innreach.domain.dto.folio.ResultList;
import org.folio.innreach.domain.dto.folio.inventorystorage.LocationDTO;

import java.util.UUID;

@FeignClient(name = "locations", configuration = FolioFeignClientConfig.class)
public interface LocationsClient {

  @GetMapping
  ResultList<LocationDTO> getLocations(@RequestParam("limit") int limit);

  @GetMapping("/{id}")
  LocationDTO getLocationById(@RequestParam("locationId") UUID locationId);

}
