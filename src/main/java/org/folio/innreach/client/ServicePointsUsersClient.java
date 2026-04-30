package org.folio.innreach.client;

import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

import org.folio.innreach.domain.dto.folio.ResultList;
import org.folio.innreach.domain.dto.folio.inventorystorage.ServicePointUserDTO;

@HttpExchange("service-points-users")
public interface ServicePointsUsersClient {

  @GetExchange
  ResultList<ServicePointUserDTO> findServicePointsUsersByQuery(@RequestParam("query") String query);
}
