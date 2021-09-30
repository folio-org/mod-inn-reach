package org.folio.innreach.domain.service;

import java.util.UUID;

import org.folio.innreach.dto.Instance;
import org.folio.innreach.dto.TransformedMARCRecordDTO;

public interface MARCRecordTransformationService {

  TransformedMARCRecordDTO transformRecord(UUID centralServerId, UUID inventoryId);

  TransformedMARCRecordDTO transformRecord(UUID centralServerId, Instance inventoryInstance);

}
