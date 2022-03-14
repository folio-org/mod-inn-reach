package org.folio.innreach.client;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import java.util.UUID;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import org.folio.innreach.client.config.FolioFeignClientConfig;
import org.folio.innreach.domain.dto.folio.CustomFieldDTO;

@FeignClient(name = "custom-fields", configuration = FolioFeignClientConfig.class)
public interface CustomFieldsClient {
  @GetMapping(value = "/{id}", produces = APPLICATION_JSON_VALUE)
  CustomFieldDTO getCustomField(@PathVariable("id") UUID customFieldId);
}
