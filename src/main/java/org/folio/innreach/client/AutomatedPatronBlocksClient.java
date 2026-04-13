package org.folio.innreach.client;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.folio.innreach.domain.dto.folio.ResultList;

@HttpExchange("automated-patron-blocks")
public interface AutomatedPatronBlocksClient {

  @GetExchange("/{userId}")
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
