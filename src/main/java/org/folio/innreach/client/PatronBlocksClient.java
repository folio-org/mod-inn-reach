package org.folio.innreach.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import org.folio.innreach.config.FolioFeignClientConfig;
import org.folio.innreach.domain.dto.folio.ResultList;
import org.folio.innreach.domain.dto.folio.patron.PatronBlock;

@FeignClient(name = "automated-patron-blocks", configuration = FolioFeignClientConfig.class)
public interface PatronBlocksClient {

  @GetMapping("/{userId}")
  ResultList<PatronBlock> getPatronBlocks(@PathVariable("userId") String userId);

}
