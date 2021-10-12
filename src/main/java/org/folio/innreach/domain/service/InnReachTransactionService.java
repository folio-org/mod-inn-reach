package org.folio.innreach.domain.service;

import java.util.List;
import java.util.UUID;

import org.folio.innreach.dto.InnReachResponseDTO;
import org.folio.innreach.dto.TransactionItemHoldDTO;

public interface InnReachTransactionService {
  InnReachResponseDTO createInnReachTransactionItemHold(String trackingId, String centralCode, TransactionItemHoldDTO dto);

  Integer countInnReachLoans(String patronId, List<UUID> loanIds);

}
