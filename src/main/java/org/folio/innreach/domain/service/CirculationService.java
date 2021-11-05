package org.folio.innreach.domain.service;

import org.folio.innreach.dto.CirculationRequestDTO;
import org.folio.innreach.dto.InnReachResponseDTO;
import org.folio.innreach.dto.TransactionHoldDTO;

public interface CirculationService {

  InnReachResponseDTO processCirculationRequest(String trackingId, String centralCode, String circulationOperationName, CirculationRequestDTO request);
}
