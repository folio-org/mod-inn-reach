package org.folio.innreach.client;

import java.util.UUID;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

import org.folio.innreach.domain.dto.folio.sourcerecord.SourceRecordDTO;

/**
 * Source Record Storage (SRS) client
 */

@HttpExchange("source-storage")
public interface SourceRecordStorageClient {

  @GetExchange("/records/{instanceId}/formatted?idType=INSTANCE")
  SourceRecordDTO getRecordByInstanceId(@PathVariable("instanceId") UUID instanceId);
}
