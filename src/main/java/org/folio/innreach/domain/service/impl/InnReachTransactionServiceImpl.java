package org.folio.innreach.domain.service.impl;

import lombok.RequiredArgsConstructor;
import org.folio.innreach.domain.entity.InnReachTransaction;
import org.folio.innreach.domain.entity.InnReachTransaction.TransactionType;
import org.folio.innreach.domain.service.InnReachTransactionService;
import org.folio.innreach.dto.D2irResponseDTO;
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
  public D2irResponseDTO createInnReachTransactionItemHold(String trackingId, String centralCode, TransactionItemHoldDTO dto) {
    var response = new D2irResponseDTO();
    response.setStatus("ok");
    try {
      InnReachTransaction transaction = new InnReachTransaction();
      transaction.setTrackingId(trackingId);
      transaction.setCentralServerCode(centralCode);
      transaction.setType(TransactionType.ITEM);
      transaction.setState(InnReachTransaction.TransactionState.ITEM_HOLD);
      var itemHold = mapper.toItemHold(dto);
      transaction.setHold(itemHold);
      repository.save(transaction);
    } catch (Exception e) {
      response.setStatus("failed");
      response.setReason(e.getMessage());
    }
    return response;
  }
}
