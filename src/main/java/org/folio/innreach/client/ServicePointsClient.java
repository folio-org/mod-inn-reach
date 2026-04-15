package org.folio.innreach.client;

import java.util.UUID;

import lombok.Data;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

import org.folio.innreach.domain.dto.folio.ResultList;

@HttpExchange("service-points")
public interface ServicePointsClient {

  @GetExchange()
  ResultList<ServicePoint> findByQuery(@RequestParam("query") String query);

  @Data
  class ServicePoint {
    private UUID id;
    private String code;
  }

}
