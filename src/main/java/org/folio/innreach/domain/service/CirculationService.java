package org.folio.innreach.domain.service;

import org.folio.innreach.dto.CirculationRequestDTO;
import org.folio.innreach.dto.InnReachResponseDTO;
import org.folio.innreach.dto.PatronHoldDTO;
import org.folio.innreach.dto.TransferRequestDTO;

public interface CirculationService {

  InnReachResponseDTO processCirculationRequest(String trackingId, String centralCode, String circulationOperationName, CirculationRequestDTO circulationRequest);

  InnReachResponseDTO initiatePatronHold(String trackingId, String centralCode, PatronHoldDTO patronHold);

  InnReachResponseDTO transferItem(String trackingId, String centralCode, TransferRequestDTO transferRequest);
}
