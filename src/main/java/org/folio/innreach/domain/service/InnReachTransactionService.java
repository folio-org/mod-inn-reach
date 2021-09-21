package org.folio.innreach.domain.service;

import org.folio.innreach.domain.dto.InnReachTransactionItemHoldDTO;
import org.folio.innreach.dto.TransactionItemHoldDTO;

public interface InnReachTransactionService {
  InnReachTransactionItemHoldDTO createInnReachTransactionItemHold(String trackingId, String centralCode, TransactionItemHoldDTO dto);
}
