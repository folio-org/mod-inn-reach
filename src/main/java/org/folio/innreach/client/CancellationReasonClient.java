package org.folio.innreach.client;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;
import org.folio.innreach.domain.dto.folio.ResultList;

@HttpExchange("cancellation-reason-storage")
public interface CancellationReasonClient {

  @GetExchange(url = "/cancellation-reasons?query=(name=={name})", accept = APPLICATION_JSON_VALUE)
  ResultList<CancellationReason> queryReasonByName(@PathVariable("name") String name);

  @PostExchange(url = "/cancellation-reasons", contentType = APPLICATION_JSON_VALUE)
  void createReason(CancellationReason reason);

  @Builder
  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  class CancellationReason {
    private UUID id;
    private String name;
    private String description;
    private String publicDescription;
    private Boolean requiresAdditionalInformation;
  }

}
