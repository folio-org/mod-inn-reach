package org.folio.innreach.domain.processor;

import org.folio.innreach.dto.InnReachResponseDTO;
import org.folio.innreach.dto.TransactionHoldDTO;

public interface InnReachCirculationProcessor {

  boolean canProcess(String circulationOperationName);

  InnReachResponseDTO process(String trackingId, String centralCode, TransactionHoldDTO transactionHold);

}
