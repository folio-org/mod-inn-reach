package org.folio.innreach.client;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import java.util.UUID;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import org.folio.innreach.config.FolioFeignClientConfig;
import org.folio.innreach.domain.dto.folio.ResultList;
import org.folio.innreach.domain.dto.folio.inventorystorage.InstanceIterationRequest;
import org.folio.innreach.domain.dto.folio.inventorystorage.JobResponse;
import org.folio.innreach.dto.Instance;

@FeignClient(name = "instance-storage", configuration = FolioFeignClientConfig.class)
public interface InstanceStorageClient {

  @GetMapping("/instances/{instanceId}")
  Instance getInstanceById(@PathVariable("instanceId") UUID instanceId);

  @PostMapping("/instances")
  Instance createInstance(@RequestBody Instance instance);

  @GetMapping("/instances?query=(hrid=={hrid})")
  ResultList<Instance> queryInstanceByHrid(@PathVariable("hrid") String hrid);

  @PostMapping(value = "/instances/iteration", consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
  JobResponse startInitialContribution(@RequestBody InstanceIterationRequest request);

}
