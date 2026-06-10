package org.folio.innreach.domain.service.impl;

import static org.folio.innreach.util.ListUtils.getFirstItem;

import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import org.folio.innreach.client.CirculationClient;
import org.folio.innreach.domain.dto.folio.circulation.RenewByIdDTO;
import org.folio.innreach.domain.entity.InnReachTransaction;
import org.folio.innreach.domain.service.LoanService;
import org.folio.innreach.dto.CheckInRequestDTO;
import org.folio.innreach.dto.CheckInResponseDTO;
import org.folio.innreach.dto.CheckOutRequestDTO;
import org.folio.innreach.dto.ClaimItemReturnedRequestDTO;
import org.folio.innreach.dto.LoanDTO;

@Log4j2
@Service
@RequiredArgsConstructor
public class LoanServiceImpl implements LoanService {

  private static final String DUE_DATE_CHANGED_ACTION = "dueDateChanged";
  private static final String OPEN_STATUS = "open";

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
  public void delete(UUID loanId) {
    circulationClient.findLoan(loanId)
      .ifPresentOrElse(loanDTO -> circulationClient.deleteLoan(loanId),
        () -> log.warn("Loan not found with loanId: {} for a delete", loanId));
  }

  @Override
  public Optional<LoanDTO> findByItemId(UUID itemId) {
    return getFirstItem(circulationClient.queryLoansByItemId(itemId));
  }

  @Override
  public LoanDTO getById(UUID loanId) {
    return find(loanId).orElseThrow(() -> new IllegalArgumentException("Loan is not found: id = " + loanId));
  }

  @Override
  public LoanDTO renew(RenewByIdDTO renewLoan) {
    return circulationClient.renewLoan(renewLoan);
  }

  @Override
  public LoanDTO changeDueDate(LoanDTO loan, Instant dueDate) {
    loan.setDueDate(Date.from(dueDate));
    loan.setAction(DUE_DATE_CHANGED_ACTION);

    return update(loan);
  }

  @Override
  public CheckInResponseDTO checkInItem(InnReachTransaction transaction, UUID servicePointId) {
    log.info("Processing item check-in for transaction {} and service point {}", transaction.getId(), servicePointId);

    var checkIn = new CheckInRequestDTO()
      .servicePointId(servicePointId)
      .itemBarcode(transaction.getHold().getFolioItemBarcode())
      .checkInDate(Date.from(Instant.now()));

    return circulationClient.checkInByBarcode(checkIn);
  }

  @Override
  public LoanDTO checkOutItem(InnReachTransaction transaction, UUID servicePointId) {
    log.info("Processing item check-out for transaction {} and service point {}", transaction.getTrackingId(), servicePointId);

    var hold = transaction.getHold();

    var checkOut = new CheckOutRequestDTO()
      .servicePointId(servicePointId)
      .userBarcode(hold.getFolioPatronBarcode())
      .itemBarcode(hold.getFolioItemBarcode());

    return circulationClient.checkOutByBarcode(checkOut);
  }

  @Override
  public void claimItemReturned(UUID loanId, Instant itemClaimedReturnedDate) {
    var request = new ClaimItemReturnedRequestDTO()
      .itemClaimedReturnedDateTime(Date.from(itemClaimedReturnedDate));

    circulationClient.claimItemReturned(loanId, request);
  }

  @Override
  public boolean isOpen(LoanDTO loanDTO) {
    return OPEN_STATUS.equalsIgnoreCase(loanDTO.getStatus().getName());
  }

}
