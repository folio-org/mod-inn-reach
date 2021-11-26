package org.folio.innreach.client;

import java.util.UUID;

import lombok.Data;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import org.folio.innreach.client.config.FolioFeignClientConfig;
import org.folio.innreach.domain.dto.folio.ResultList;

@FeignClient(name = "service-points", configuration = FolioFeignClientConfig.class)
public interface ServicePointsClient {

  @GetMapping("?query=code=={code}")
  ResultList<ServicePoint> queryServicePointByCode(@PathVariable("code") String pickupLocationCode);

  @Data
  class ServicePoint {
    private UUID id;
    private String code;
  }

}
