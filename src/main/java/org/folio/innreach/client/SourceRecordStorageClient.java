package org.folio.innreach.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import org.folio.innreach.domain.dto.folio.inventory.SourceRecordDTO;

import java.util.UUID;

/**
 * Source Record Storage (SRS) client
 */

@FeignClient("SRS")
public interface SourceRecordStorageClient {

  @GetMapping("/source-storage/records/{id}")
  SourceRecordDTO getRecordById(@PathVariable("id") UUID id);
}
