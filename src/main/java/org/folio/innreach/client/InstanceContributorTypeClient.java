package org.folio.innreach.client;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

import org.folio.innreach.domain.dto.folio.ResultList;

@HttpExchange("contributor-name-types")
public interface InstanceContributorTypeClient {

  @GetExchange(accept = APPLICATION_JSON_VALUE)
  ResultList<NameType> findByQuery(@RequestParam("query") String query);

  @PostExchange(contentType = APPLICATION_JSON_VALUE)
  void createContributorType(NameType nameType);

  @Builder
  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  class NameType {
    private UUID id;
    private String name;
    private Integer ordering;
  }

}
