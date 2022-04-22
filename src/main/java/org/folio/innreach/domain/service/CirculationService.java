package org.folio.innreach.domain.service;

import org.folio.innreach.dto.BaseCircRequestDTO;
import org.folio.innreach.dto.CancelRequestDTO;
import org.folio.innreach.dto.ClaimsItemReturnedDTO;
import org.folio.innreach.dto.InnReachResponseDTO;
import org.folio.innreach.dto.ItemReceivedDTO;
import org.folio.innreach.dto.ItemShippedDTO;
import org.folio.innreach.dto.LocalHoldDTO;
import org.folio.innreach.dto.PatronHoldDTO;
import org.folio.innreach.dto.RecallDTO;
import org.folio.innreach.dto.RenewLoanDTO;
import org.folio.innreach.dto.ReturnUncirculatedDTO;
import org.folio.innreach.dto.TransactionHoldDTO;
import org.folio.innreach.dto.TransferRequestDTO;

public interface CirculationService {
  InnReachResponseDTO createInnReachTransactionItemHold(String trackingId, String centralCode, TransactionHoldDTO dto);

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

  InnReachResponseDTO borrowerRenewLoan(String trackingId, String centralCode, RenewLoanDTO renewLoan);

  InnReachResponseDTO ownerRenewLoan(String trackingId, String centralCode, RenewLoanDTO renewLoan);

  InnReachResponseDTO finalCheckIn(String trackingId, String centralCode, BaseCircRequestDTO finalCheckIn);

  InnReachResponseDTO claimsReturned(String trackingId, String centralCode, ClaimsItemReturnedDTO claimsItemReturned);

}
