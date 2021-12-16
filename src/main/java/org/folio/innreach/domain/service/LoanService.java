package org.folio.innreach.domain.service;

import java.util.UUID;

import org.folio.innreach.domain.dto.folio.circulation.RenewByIdDTO;
import org.folio.innreach.dto.LoanDTO;

public interface LoanService extends BasicService<UUID, LoanDTO> {

  LoanDTO getById(UUID loanId);

  LoanDTO renew(RenewByIdDTO renewLoan);

}
