package org.folio.innreach.domain.service;

import java.util.List;
import java.util.UUID;

import org.folio.innreach.dto.InnReachTransactionDTO;
import org.folio.innreach.dto.InnReachTransactionFilterParametersDTO;
import org.folio.innreach.dto.InnReachTransactionSearchRequestDTO;
import org.folio.innreach.dto.InnReachTransactionsDTO;

public interface InnReachTransactionService {

  Integer countInnReachLoans(String patronId, List<UUID> loanIds);

  InnReachTransactionDTO getInnReachTransaction(UUID transactionId);

  InnReachTransactionsDTO getAllTransactions(Integer offset, Integer limit, InnReachTransactionFilterParametersDTO parameters);

  InnReachTransactionsDTO searchTransactions(Integer offset, Integer limit, InnReachTransactionSearchRequestDTO searchRequest);
}
