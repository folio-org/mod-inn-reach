package org.folio.innreach.domain.service.impl;

import java.util.List;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.folio.innreach.domain.exception.EntityNotFoundException;
import org.folio.innreach.domain.service.InnReachTransactionService;
import org.folio.innreach.dto.InnReachTransactionDTO;
import org.folio.innreach.dto.InnReachTransactionFilterParametersDTO;
import org.folio.innreach.dto.InnReachTransactionsDTO;
import org.folio.innreach.mapper.InnReachTransactionFilterParametersMapper;
import org.folio.innreach.mapper.InnReachTransactionMapper;
import org.folio.innreach.repository.InnReachTransactionRepository;
import org.folio.innreach.repository.TransactionHoldRepository;
import org.folio.innreach.specification.InnReachTransactionSpecification;

@Log4j2
@RequiredArgsConstructor
@Service
public class InnReachTransactionServiceImpl implements InnReachTransactionService {

  private final InnReachTransactionRepository repository;
  private final TransactionHoldRepository holdRepository;

  private final InnReachTransactionMapper transactionMapper;
  private final InnReachTransactionFilterParametersMapper parametersMapper;

  private final InnReachTransactionSpecification specification;

  @Override
  public Integer countInnReachLoans(String patronId, List<UUID> loanIds) {
    return holdRepository.countByPatronIdAndFolioLoanIdIn(patronId, loanIds);
  }

  @Override
  public InnReachTransactionDTO getInnReachTransaction(UUID transactionId) {
    return repository.fetchOneById(transactionId)
      .map(transactionMapper::toDTO)
      .orElseThrow(() -> new EntityNotFoundException(String.format("InnReach transaction with id [%s] not found!", transactionId)));
  }

  @Override
  @Transactional(readOnly = true)
  public InnReachTransactionsDTO getAllTransactions(Integer offset, Integer limit,
                                                    InnReachTransactionFilterParametersDTO parametersDTO) {
    var parameters = parametersMapper.toEntity(parametersDTO);
    var transactions = repository.findAll(specification.filterByParameters(parameters), PageRequest.of(offset, limit));
    return transactionMapper.toDTOCollection(transactions);
  }

  @Override
  @Transactional
  public void updateInnReachTransaction(UUID transactionId, InnReachTransactionDTO transaction) {
    var oldTransaction = repository.getById(transactionId);
    var updatedTransaction = transactionMapper.toEntity(transaction);

    oldTransaction.setState(updatedTransaction.getState());
    oldTransaction.setHold(updatedTransaction.getHold());

    repository.save(oldTransaction);
  }

}
