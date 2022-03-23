package org.folio.innreach.domain.service;

import java.util.Date;
import java.util.Optional;
import java.util.UUID;

import org.folio.innreach.domain.dto.folio.circulation.RenewByIdDTO;
import org.folio.innreach.domain.entity.InnReachTransaction;
import org.folio.innreach.dto.CheckInResponseDTO;
import org.folio.innreach.dto.LoanDTO;

public interface LoanService extends BasicService<UUID, LoanDTO> {

  Optional<LoanDTO> findByItemId(UUID itemId);

  LoanDTO getById(UUID loanId);

  LoanDTO renew(RenewByIdDTO renewLoan);

  LoanDTO changeDueDate(LoanDTO loan, Date dueDate);

  LoanDTO checkOutItem(InnReachTransaction transaction, UUID servicePointId);

  CheckInResponseDTO checkInItem(InnReachTransaction transaction, UUID servicePointId);

  void claimItemReturned(UUID loanId, Date itemClaimedReturnedDateTime);
}
