package org.folio.innreach.domain.service;

import org.folio.innreach.domain.dto.folio.inventory.SourceRecordDTO;

import java.util.UUID;

public interface MARCTransformationService {

  SourceRecordDTO transformRecord(UUID centralServerId, UUID inventoryId);
}
