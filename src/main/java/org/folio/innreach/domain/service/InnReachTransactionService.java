package org.folio.innreach.domain.service;

import org.folio.innreach.dto.D2irResponseDTO;
import org.folio.innreach.dto.TransactionItemHoldDTO;

public interface InnReachTransactionService {
  D2irResponseDTO createInnReachTransactionItemHold(String trackingId, String centralCode, TransactionItemHoldDTO dto);
}
