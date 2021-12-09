package org.folio.innreach.client;

import java.util.UUID;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import org.folio.innreach.client.config.FolioFeignClientConfig;
import org.folio.innreach.domain.dto.folio.sourcerecord.SourceRecordDTO;

/**
 * Source Record Storage (SRS) client
 */

@FeignClient(name = "source-storage", configuration = FolioFeignClientConfig.class)
public interface SourceRecordStorageClient {

  @GetMapping("/records/{id}")
  SourceRecordDTO getRecordById(@PathVariable("id") UUID id);
}
