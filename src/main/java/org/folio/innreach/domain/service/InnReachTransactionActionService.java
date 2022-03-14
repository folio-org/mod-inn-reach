package org.folio.innreach.domain.service;

import java.util.UUID;

import org.folio.innreach.domain.dto.folio.circulation.RequestDTO;
import org.folio.innreach.dto.CheckInDTO;
import org.folio.innreach.dto.TransactionCheckOutResponseDTO;
import org.folio.innreach.dto.PatronHoldCheckInResponseDTO;
import org.folio.innreach.dto.StorageLoanDTO;

public interface InnReachTransactionActionService {

  PatronHoldCheckInResponseDTO checkInPatronHoldItem(UUID transactionId, UUID servicePointId);

  PatronHoldCheckInResponseDTO checkInPatronHoldUnshippedItem(UUID transactionId, UUID servicePointId, String itemBarcode);

  TransactionCheckOutResponseDTO checkOutItemHoldItem(String itemBarcode, UUID servicePointId);

  TransactionCheckOutResponseDTO checkOutPatronHoldItem(UUID transactionId, UUID servicePointId);

  void associateNewLoanWithTransaction(StorageLoanDTO loan);

  void handleLoanUpdate(StorageLoanDTO loan);

  void handleRequestUpdate(RequestDTO requestDTO);

  void handleCheckInCreation(CheckInDTO checkIn);

}
