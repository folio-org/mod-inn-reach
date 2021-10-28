package org.folio.innreach.domain.service;

import java.util.List;
import java.util.UUID;

import org.folio.innreach.dto.InnReachResponseDTO;
import org.folio.innreach.dto.InnReachTransactionDTO;
import org.folio.innreach.dto.TransactionHoldDTO;
import org.folio.innreach.dto.InnReachTransactionsDTO;

public interface InnReachTransactionService {
  InnReachResponseDTO createInnReachTransactionItemHold(String trackingId, String centralCode, TransactionHoldDTO dto);

  Integer countInnReachLoans(String patronId, List<UUID> loanIds);

  InnReachTransactionDTO getInnReachTransaction(UUID transactionId);

  InnReachTransactionsDTO getAllTransactions(Integer offset, Integer limit);
}
