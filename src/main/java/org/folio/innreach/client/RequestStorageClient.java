package org.folio.innreach.client;

import java.util.UUID;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import org.folio.innreach.config.FolioFeignClientConfig;
import org.folio.innreach.domain.dto.folio.ResultList;
import org.folio.innreach.domain.dto.folio.requeststorage.RequestDTO;

@FeignClient(name = "request-storage", configuration = FolioFeignClientConfig.class)
public interface RequestStorageClient {

  @GetMapping("/requests?query=(itemId=={itemId})")
  ResultList<RequestDTO> findRequests(@PathVariable("itemId") UUID itemId);

  @PostMapping("/requests")
  RequestDTO sendRequest(@RequestBody RequestDTO requestDTO);
}
