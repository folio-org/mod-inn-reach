package org.folio.innreach.client;

import java.util.UUID;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import org.folio.innreach.client.config.FolioFeignClientConfig;
import org.folio.innreach.domain.dto.folio.ResultList;
import org.folio.innreach.domain.dto.folio.requestpreference.RequestPreferenceDTO;

@FeignClient(name = "request-preference-storage", configuration = FolioFeignClientConfig.class)
public interface RequestPreferenceStorageClient {
    @GetMapping("/request-preference?query=userId=={userId}")
    ResultList<RequestPreferenceDTO> getUserRequestPreference(@PathVariable("userId") UUID userId);
}
