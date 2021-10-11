package org.folio.innreach.client;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import org.folio.innreach.config.FolioFeignClientConfig;
import org.folio.innreach.domain.dto.folio.patron.PatronDTO;

@FeignClient(name = "patron", configuration = FolioFeignClientConfig.class)
public interface PatronClient {

  @GetMapping(value = "/account/{id}", produces = APPLICATION_JSON_VALUE)
  PatronDTO getAccountDetails(@PathVariable("id") String userId);

}
