package org.folio.innreach.domain.service.impl;

import java.util.Optional;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import org.folio.innreach.client.CirculationClient;
import org.folio.innreach.domain.dto.folio.circulation.RenewByIdDTO;
import org.folio.innreach.domain.service.LoanService;
import org.folio.innreach.dto.LoanDTO;

@Service
@RequiredArgsConstructor
public class LoanServiceImpl implements LoanService {

  private final CirculationClient circulationClient;


  @Override
  public LoanDTO create(LoanDTO loan) {
    return circulationClient.createLoan(loan);
  }

  @Override
  public LoanDTO update(LoanDTO loan) {
    circulationClient.updateLoan(loan.getId(), loan);
    return loan;
  }

  @Override
  public Optional<LoanDTO> find(UUID loanId) {
    return circulationClient.findLoan(loanId);
  }

  @Override
  public LoanDTO getById(UUID loanId) {
    return find(loanId).orElseThrow(() -> new IllegalArgumentException("Loan is not found: id = " + loanId));
  }

  @Override
  public LoanDTO renew(RenewByIdDTO renewLoan) {
    return circulationClient.renewLoan(renewLoan);
  }

}
