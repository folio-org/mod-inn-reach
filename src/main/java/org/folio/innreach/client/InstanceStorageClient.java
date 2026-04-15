package org.folio.innreach.client;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import java.util.UUID;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.DeleteExchange;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

import org.folio.innreach.domain.dto.folio.inventorystorage.InstanceIterationRequest;
import org.folio.innreach.domain.dto.folio.inventorystorage.JobResponse;
import org.folio.innreach.dto.Instance;

@HttpExchange("instance-storage")
public interface InstanceStorageClient {

  @GetExchange("/instances/{instanceId}")
  Instance getInstanceById(@PathVariable("instanceId") UUID instanceId);

  @GetExchange("/instances/iteration/{id}")
  JobResponse getJobById(@PathVariable("id") UUID id);

  @PostExchange(value = "/instances/iteration", contentType = APPLICATION_JSON_VALUE, accept = APPLICATION_JSON_VALUE)
  JobResponse startInstanceIteration(@RequestBody InstanceIterationRequest request);

  @DeleteExchange(value = "/instances/iteration/{jobId}")
  void cancelInstanceIteration(@PathVariable("jobId") UUID jobId);

}
