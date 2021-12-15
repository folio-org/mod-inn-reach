package org.folio.innreach.client;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import org.folio.innreach.client.config.FolioFeignClientConfig;
import org.folio.innreach.domain.dto.folio.ResultList;

@FeignClient(value = "cancellation-reason-storage", configuration = FolioFeignClientConfig.class)
public interface CancellationReasonClient {

  @GetMapping(value = "/cancellation-reasons?query=(name=={name})", produces = APPLICATION_JSON_VALUE)
  ResultList<CancellationReason> queryReasonByName(@PathVariable("name") String name);

  @PostMapping(value = "/cancellation-reasons", consumes = APPLICATION_JSON_VALUE)
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
