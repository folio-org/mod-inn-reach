package org.folio.innreach.domain.service;

import org.folio.innreach.dto.BaseCircRequestDTO;
import org.folio.innreach.dto.BorrowerRenewDTO;
import org.folio.innreach.dto.CancelRequestDTO;
import org.folio.innreach.dto.InnReachResponseDTO;
import org.folio.innreach.dto.ItemReceivedDTO;
import org.folio.innreach.dto.ItemShippedDTO;
import org.folio.innreach.dto.LocalHoldDTO;
import org.folio.innreach.dto.PatronHoldDTO;
import org.folio.innreach.dto.RecallDTO;
import org.folio.innreach.dto.ReturnUncirculatedDTO;
import org.folio.innreach.dto.TransferRequestDTO;

public interface CirculationService {

  InnReachResponseDTO initiatePatronHold(String trackingId, String centralCode, PatronHoldDTO patronHold);

  InnReachResponseDTO initiateLocalHold(String trackingId, String centralCode, LocalHoldDTO localHold);

  InnReachResponseDTO trackPatronHoldShippedItem(String trackingId, String centralCode, ItemShippedDTO itemShipped);

  InnReachResponseDTO cancelPatronHold(String trackingId, String centralCode, CancelRequestDTO cancelRequest);

  InnReachResponseDTO transferPatronHoldItem(String trackingId, String centralCode, TransferRequestDTO transferRequest);

  InnReachResponseDTO cancelItemHold(String trackingId, String centralCode, BaseCircRequestDTO cancelItemDTO);

  InnReachResponseDTO receiveUnshipped(String trackingId, String centralCode, BaseCircRequestDTO receiveUnshippedRequestDTO);

  InnReachResponseDTO itemInTransit(String trackingId, String centralCode, BaseCircRequestDTO itemInTransitRequest);

  InnReachResponseDTO returnUncirculated(String trackingId, String centralCode, ReturnUncirculatedDTO returnUncirculated);

  InnReachResponseDTO itemReceived(String trackingId, String centralCode, ItemReceivedDTO itemReceivedDTO);

  InnReachResponseDTO recall(String trackingId, String centralCode, RecallDTO recallDTO);

  InnReachResponseDTO borrowerRenew(String trackingId, String centralCode, BorrowerRenewDTO borrowerRenew);

  InnReachResponseDTO finalCheckIn(String trackingId, String centralCode, BaseCircRequestDTO finalCheckIn);
}
