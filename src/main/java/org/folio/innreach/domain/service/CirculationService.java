package org.folio.innreach.domain.service;

import org.folio.innreach.dto.InnReachResponseDTO;
import org.folio.innreach.dto.TransactionHoldDTO;
import org.folio.innreach.dto.TransferRequestDTO;

public interface CirculationService {

  InnReachResponseDTO processCirculationRequest(String trackingId, String centralCode, String circulationOperationName, TransactionHoldDTO transactionHold);

  InnReachResponseDTO transferItem(String trackingId, String centralCode, TransferRequestDTO transferRequest);

}
