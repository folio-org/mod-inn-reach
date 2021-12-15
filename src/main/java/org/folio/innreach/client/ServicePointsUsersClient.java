package org.folio.innreach.client;

import java.util.UUID;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import org.folio.innreach.client.config.FolioFeignClientConfig;
import org.folio.innreach.domain.dto.folio.ResultList;
import org.folio.innreach.domain.dto.folio.inventorystorage.ServicePointUserDTO;

@FeignClient(name = "service-points-users", configuration = FolioFeignClientConfig.class)
public interface ServicePointsUsersClient {
  @GetMapping("?query=userId=={userId}")
  ResultList<ServicePointUserDTO> findServicePointsUsers(@PathVariable("userId") UUID userId);
}
