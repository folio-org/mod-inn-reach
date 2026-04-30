package org.folio.innreach.client;

import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

import org.folio.innreach.domain.dto.folio.ResultList;
import org.folio.innreach.domain.dto.folio.inventorystorage.LocationDTO;

@HttpExchange("locations")
public interface LocationsClient {

  @GetExchange
  ResultList<LocationDTO> getLocations(@RequestParam("limit") int limit);

  @GetExchange
  ResultList<LocationDTO> queryLocationsByServicePoint(@RequestParam("query") String query, @RequestParam("limit") int limit);

}
