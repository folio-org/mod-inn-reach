package org.folio.innreach.domain.service;

import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public interface VirtualRecordService {

  void deleteVirtualRecords(UUID folioItemId,UUID folioHoldingId,UUID folioInstanceId,UUID folioLoanId);
}
