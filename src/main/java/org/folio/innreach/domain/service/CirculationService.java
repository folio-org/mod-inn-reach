package org.folio.innreach.domain.service;

import org.folio.innreach.dto.BaseCircRequestDTO;
import org.folio.innreach.dto.CancelRequestDTO;
import org.folio.innreach.dto.InnReachResponseDTO;
import org.folio.innreach.dto.ItemReceivedDTO;
import org.folio.innreach.dto.ItemShippedDTO;
import org.folio.innreach.dto.PatronHoldDTO;
import org.folio.innreach.dto.TransferRequestDTO;

public interface CirculationService {

  InnReachResponseDTO initiatePatronHold(String trackingId, String centralCode, PatronHoldDTO patronHold);

  InnReachResponseDTO trackPatronHoldShippedItem(String trackingId, String centralCode, ItemShippedDTO itemShipped);

  InnReachResponseDTO cancelPatronHold(String trackingId, String centralCode, CancelRequestDTO cancelRequest);

  InnReachResponseDTO transferPatronHoldItem(String trackingId, String centralCode, TransferRequestDTO transferRequest);

  InnReachResponseDTO cancelItemHold(String trackingId, String centralCode, BaseCircRequestDTO cancelItemDTO);

  InnReachResponseDTO itemReceived(String trackingId, String centralCode, ItemReceivedDTO itemReceivedDTO);
}
