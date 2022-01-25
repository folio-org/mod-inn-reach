package org.folio.innreach.domain.service;

import java.util.UUID;

import org.folio.innreach.dto.ItemHoldCheckOutResponseDTO;
import org.folio.innreach.dto.LoanDTO;
import org.folio.innreach.dto.PatronHoldCheckInResponseDTO;

public interface InnReachTransactionActionService {

  PatronHoldCheckInResponseDTO checkInPatronHoldItem(UUID transactionId, UUID servicePointId);

  PatronHoldCheckInResponseDTO checkInPatronHoldUnshippedItem(UUID transactionId, UUID servicePointId, String itemBarcode);

  ItemHoldCheckOutResponseDTO checkOutItemHoldItem(String itemBarcode, UUID servicePointId);

  void associateNewLoanWithTransaction(LoanDTO loan);
}
