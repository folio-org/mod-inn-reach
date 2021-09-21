package org.folio.innreach.domain.service.impl;

import lombok.RequiredArgsConstructor;
import org.folio.innreach.domain.dto.InnReachTransactionItemHoldDTO;
import org.folio.innreach.domain.entity.InnReachTransaction;
import org.folio.innreach.domain.entity.InnReachTransaction.TransactionType;
import org.folio.innreach.domain.service.InnReachTransactionService;
import org.folio.innreach.dto.TransactionItemHoldDTO;
import org.folio.innreach.mapper.InnReachTransactionMapper;
import org.folio.innreach.repository.InnReachTransactionRepository;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class InnReachTransactionServiceImpl implements InnReachTransactionService {

  private final InnReachTransactionRepository repository;
  private final InnReachTransactionMapper mapper;

  @Override
  public InnReachTransactionItemHoldDTO createInnReachTransactionItemHold(String trackingId, String centralCode, TransactionItemHoldDTO dto) {
    InnReachTransaction transaction = new InnReachTransaction();
    transaction.setTrackingId(trackingId);
    transaction.setCentralServerCode(centralCode);
    transaction.setType(TransactionType.ITEM);
    transaction.setState(InnReachTransaction.TransactionState.ITEM_HOLD);
    var itemHold = mapper.toItemHold(dto);
    transaction.setHold(itemHold);
    var createdTransaction = repository.save(transaction);
    return mapper.toInnReachTransactionItemHoldDTO(createdTransaction);
  }
}
