package org.folio.innreach.client;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import org.folio.innreach.config.FolioFeignClientConfig;
import org.folio.innreach.domain.dto.folio.ResultList;

@FeignClient(value = "instance-types", configuration = FolioFeignClientConfig.class)
public interface InstanceTypeClient {

  @GetMapping(value = "?query=(name=={name})", produces = APPLICATION_JSON_VALUE)
  ResultList<InstanceType> queryInstanceTypeByName(@PathVariable("name") String name);

  @PostMapping(consumes = APPLICATION_JSON_VALUE)
  void createInstanceType(InstanceType type);

  @Builder
  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  class InstanceType {
    private UUID id;
    private String name;
    private String code;
    private String source;
  }
}
