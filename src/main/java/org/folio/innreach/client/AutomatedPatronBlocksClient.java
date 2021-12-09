package org.folio.innreach.client;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import org.folio.innreach.client.config.FolioFeignClientConfig;
import org.folio.innreach.domain.dto.folio.ResultList;

@FeignClient(name = "automated-patron-blocks", configuration = FolioFeignClientConfig.class)
public interface AutomatedPatronBlocksClient {

  @GetMapping("/{userId}")
  ResultList<AutomatedPatronBlock> getPatronBlocks(@PathVariable("userId") UUID userId);

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  class AutomatedPatronBlock {
    private UUID patronBlockConditionId;
    private Boolean blockBorrowing;
    private Boolean blockRenewals;
    private Boolean blockRequests;
    private String message;
  }

}
