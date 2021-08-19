package org.folio.innreach.client;

import org.folio.innreach.config.FolioFeignClientConfig;
import org.folio.innreach.domain.dto.folio.inventoryStorage.InstanceIterationRequest;
import org.folio.innreach.domain.dto.folio.inventoryStorage.JobResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@FeignClient(name = "instance-storage", configuration = FolioFeignClientConfig.class)
public interface InstanceStorageClient {

  @PostMapping(value = "/instances/iteration", consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
  JobResponse startInitialContribution(@RequestBody InstanceIterationRequest request);
}
