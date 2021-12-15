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

@FeignClient(name = "manualblocks", configuration = FolioFeignClientConfig.class)
public interface ManualPatronBlocksClient {

  @GetMapping("?query=(userId=={userId})")
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
