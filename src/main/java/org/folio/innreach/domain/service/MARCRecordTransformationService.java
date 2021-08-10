package org.folio.innreach.domain.service;

import java.util.UUID;

import org.folio.innreach.domain.dto.folio.inventory.SourceRecordDTO;

public interface MARCRecordTransformationService {

  SourceRecordDTO transformRecord(UUID centralServerId, UUID inventoryId);
}
