package org.folio.innreach.client;

import java.util.UUID;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import org.folio.innreach.config.FolioFeignClientConfig;
import org.folio.innreach.domain.dto.folio.patron.PatronDTO;

@FeignClient(name = "patron", configuration = FolioFeignClientConfig.class)
public interface PatronClient {

  @GetMapping("/account/{id}")
  PatronDTO getAccountDetails(@PathVariable("id") String userId);

}
