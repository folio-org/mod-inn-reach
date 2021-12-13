package org.folio.innreach.domain.service;

import java.util.UUID;

import org.folio.innreach.dto.RenewLoanRequestDTO;
import org.springframework.scheduling.annotation.Async;

import org.folio.innreach.domain.dto.folio.User;
import org.folio.innreach.domain.dto.folio.circulation.RequestDTO;
import org.folio.innreach.domain.dto.folio.circulation.RequestDTO.RequestType;
import org.folio.innreach.domain.dto.folio.inventory.InventoryItemDTO;
import org.folio.innreach.domain.entity.InnReachTransaction;
import org.folio.innreach.dto.CheckInResponseDTO;
import org.folio.innreach.dto.CheckOutResponseDTO;
import org.folio.innreach.dto.Holding;

public interface RequestService {
  @Async
  void createItemHoldRequest(String transactionTrackingId);

  @Async
  void createLocalHoldRequest(String transactionTrackingId);

  void createItemRequest(InnReachTransaction transaction, Holding holding, InventoryItemDTO item,
                         User patron, UUID servicePointId, RequestType requestType);

  void moveItemRequest(InnReachTransaction transaction, Holding holding, InventoryItemDTO item);

  void cancelRequest(InnReachTransaction transaction, String reason);

  CheckInResponseDTO checkInItem(InnReachTransaction transaction, UUID servicePointId);

  CheckOutResponseDTO checkOutItem(InnReachTransaction transaction, UUID servicePointId);

  void createRecallRequest(UUID userId, UUID itemId);

  RequestDTO findRequest(UUID requestId);

  CheckOutResponseDTO getLoan(UUID loanId);

  CheckOutResponseDTO renewLoan(RenewLoanRequestDTO renewLoan);
}
