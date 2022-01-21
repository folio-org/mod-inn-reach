package org.folio.innreach.client;

import org.folio.innreach.client.config.FolioFeignClientConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@FeignClient(name = "innreach/v2/circ", configuration = FolioFeignClientConfig.class)
public interface BorrowingCentralServer {

  @PostMapping(value = "/borrowerrenew/{trackingId}/{centralCode}", consumes = APPLICATION_JSON_VALUE)
  void borrowingToCentralServer(@PathVariable("trackingId") String name, @PathVariable("centralCode") String centralCode, @RequestBody Integer dueDateTime);
}
