package org.folio.innreach.client;

import java.util.UUID;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

import org.folio.innreach.domain.dto.folio.ResultList;
import org.folio.innreach.domain.dto.folio.inventorystorage.LocationDTO;

@HttpExchange("locations")
public interface LocationsClient {

  @GetExchange
  ResultList<LocationDTO> getLocations(@RequestParam("limit") int limit);

  @GetExchange("?query=primaryServicePoint=={servicePointId}")
  ResultList<LocationDTO> queryLocationsByServicePoint(@PathVariable("servicePointId") UUID servicePointId, @RequestParam("limit") int limit);

}
