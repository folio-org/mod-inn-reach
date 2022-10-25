package org.folio.innreach.domain.service;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public interface VirtualRecordService {

  @Async
  void deleteVirtualRecords(UUID folioItemId,UUID folioHoldingId,UUID folioInstanceId,UUID folioLoanId);
}
