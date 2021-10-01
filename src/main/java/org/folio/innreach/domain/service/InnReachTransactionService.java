package org.folio.innreach.domain.service;

import org.folio.innreach.dto.InnReachResponseDTO;
import org.folio.innreach.dto.TransactionItemHoldDTO;

public interface InnReachTransactionService {
  InnReachResponseDTO createInnReachTransactionItemHold(String trackingId, String centralCode, TransactionItemHoldDTO dto);
  void createItemRequest(String transactionTrackingId);
}
