package org.folio.innreach.client;

import java.util.UUID;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

import org.folio.innreach.domain.dto.folio.ResultList;
import org.folio.innreach.domain.dto.folio.inventorystorage.ServicePointUserDTO;

@HttpExchange("service-points-users")
public interface ServicePointsUsersClient {

  @GetExchange("?query=userId=={userId}")
  ResultList<ServicePointUserDTO> findServicePointsUsers(@PathVariable("userId") UUID userId);
}
