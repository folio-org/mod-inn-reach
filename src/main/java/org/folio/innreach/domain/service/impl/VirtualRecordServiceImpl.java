package org.folio.innreach.domain.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.innreach.domain.service.*;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Log4j2
@Service
@RequiredArgsConstructor
public class VirtualRecordServiceImpl implements VirtualRecordService {

  private final ItemService itemService;
  private final HoldingsService holdingsService;
  private final InstanceService instanceService;
  private final LoanService loanService;
  @Override
  public void deleteVirtualRecords(UUID folioItemId, UUID folioHoldingId, UUID folioInstanceId, UUID folioLoanId) {
    log.info("Inside deleteVirtualRecords, loanId->>"+folioLoanId);
    Optional.ofNullable(folioItemId).ifPresent(itemService::delete);
    Optional.ofNullable(folioHoldingId).ifPresent(holdingsService::delete);
    Optional.ofNullable(folioInstanceId).ifPresent(instanceService::delete);
    Optional.ofNullable(folioLoanId).ifPresent(loanService::delete);
  }
}
