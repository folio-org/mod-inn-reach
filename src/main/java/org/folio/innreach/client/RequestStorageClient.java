package org.folio.innreach.client;

import java.util.UUID;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import org.folio.innreach.domain.dto.folio.requeststorage.RequestsDTO;

@FeignClient("request-storage")
public interface RequestStorageClient {

  @GetMapping("/requests?query=(itemId==\"{itemId}\")")
  RequestsDTO findRequests(@PathVariable("itemId") UUID itemId);
}
