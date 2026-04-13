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

@HttpExchange("manualblocks")
public interface ManualPatronBlocksClient {

  @GetExchange("?query=(userId=={userId})")
  ResultList<ManualPatronBlock> getPatronBlocks(@PathVariable("userId") UUID userId);

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  class ManualPatronBlock {
    private String userId;
    private String desc;
    private String patronMessage;
    private Boolean borrowing;
    private Boolean renewals;
    private Boolean requests;
  }
}
