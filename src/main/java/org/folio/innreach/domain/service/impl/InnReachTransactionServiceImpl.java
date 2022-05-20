package org.folio.innreach.domain.service.impl;

import java.util.List;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.folio.innreach.domain.entity.TransactionHold;
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
import org.folio.spring.data.OffsetRequest;

@Log4j2
@RequiredArgsConstructor
@Service
public class InnReachTransactionServiceImpl implements InnReachTransactionService {

  private final InnReachTransactionRepository repository;
  private final TransactionHoldRepository holdRepository;

  private final InnReachTransactionMapper transactionMapper;
  private final InnReachTransactionFilterParametersMapper parametersMapper;

  private final InnReachTransactionSpecification specification;

  private static final String[] TRANSACTION_HOLD_IGNORE_PROPS_ON_COPY = {
    "pickupLocation", "id", "createdBy", "updatedBy", "createdDate", "updatedDate"
  };
  private static final String[] PICKUP_LOC_IGNORE_PROPS_ON_COPY = {
    "id", "createdBy", "updatedBy", "createdDate", "updatedDate"
  };

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
    var pageRequest = new OffsetRequest(offset, limit, Sort.unsorted());
    var transactions = repository.findAll(specification.filterByParameters(parameters), pageRequest);
    return transactionMapper.toDTOCollection(transactions);
  }

  @Override
  @Transactional
  public void updateInnReachTransaction(UUID transactionId, InnReachTransactionDTO transactionDTO) {
    var oldTransaction = repository.getById(transactionId);
    var newTransaction = transactionMapper.toEntity(transactionDTO);

    oldTransaction.setState(newTransaction.getState());
    updateTransactionHold(newTransaction.getHold(), oldTransaction.getHold());

    repository.save(oldTransaction);
  }

  private void updateTransactionHold(TransactionHold newHold, TransactionHold oldHold) {
    var oldPickupLocation = oldHold.getPickupLocation();
    var newPickupLocation = newHold.getPickupLocation();

    BeanUtils.copyProperties(newHold, oldHold, TRANSACTION_HOLD_IGNORE_PROPS_ON_COPY);
    BeanUtils.copyProperties(newPickupLocation, oldPickupLocation, PICKUP_LOC_IGNORE_PROPS_ON_COPY);
  }
}
