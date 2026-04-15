package org.folio.innreach.client;

import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

import org.folio.innreach.domain.dto.folio.ResultList;
import org.folio.innreach.domain.dto.folio.inventorystorage.MaterialTypeDTO;

@HttpExchange("material-types")
public interface MaterialTypesClient {

  @GetExchange
  ResultList<MaterialTypeDTO> getMaterialTypes(@RequestParam("query") String query, @RequestParam("limit") int limit);

}
