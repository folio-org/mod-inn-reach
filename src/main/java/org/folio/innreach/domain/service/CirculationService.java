package org.folio.innreach.domain.service;

import org.folio.innreach.dto.CancelRequestDTO;
import org.folio.innreach.dto.InnReachResponseDTO;
import org.folio.innreach.dto.ItemShippedDTO;
import org.folio.innreach.dto.PatronHoldDTO;
import org.folio.innreach.dto.TransferRequestDTO;

public interface CirculationService {

  InnReachResponseDTO initiatePatronHold(String trackingId, String centralCode, PatronHoldDTO patronHold);

  InnReachResponseDTO trackShippedItem(String trackingId, String centralCode, ItemShippedDTO itemShipped);

  InnReachResponseDTO cancelTransaction(String trackingId, String centralCode, CancelRequestDTO cancelRequest);

  InnReachResponseDTO transferItem(String trackingId, String centralCode, TransferRequestDTO transferRequest);
}
