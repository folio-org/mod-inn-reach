package org.folio.innreach.client;

import java.util.UUID;

import lombok.Builder;
import lombok.Data;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import org.folio.innreach.config.FolioFeignClientConfig;
import org.folio.innreach.domain.dto.folio.requeststorage.RequestDTO;

@FeignClient(value = "circulation", configuration = FolioFeignClientConfig.class)
public interface CirculationClient {

  @PostMapping("/requests/{requestId}/move")
  RequestDTO moveRequest(@PathVariable("requestId") UUID requestId, @RequestBody MoveRequest payload);

  @Builder
  @Data
  class MoveRequest {
    private UUID destinationItemId;
    private String requestType;
  }
}
