package org.folio.innreach.domain.service.impl;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

import javax.annotation.PostConstruct;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import org.folio.innreach.domain.entity.InnReachTransaction;
import org.folio.innreach.domain.entity.InnReachTransaction.TransactionType;
import org.folio.innreach.domain.entity.TransactionHold;
import org.folio.innreach.domain.entity.TransactionItemHold;
import org.folio.innreach.domain.entity.TransactionLocalHold;
import org.folio.innreach.domain.entity.TransactionPatronHold;
import org.folio.innreach.domain.exception.EntityNotFoundException;
import org.folio.innreach.domain.service.CentralServerService;
import org.folio.innreach.domain.service.InnReachTransactionService;
import org.folio.innreach.dto.InnReachResponseDTO;
import org.folio.innreach.dto.InnReachTransactionDTO;
import org.folio.innreach.dto.TransactionHoldDTO;
import org.folio.innreach.dto.TransactionItemHoldDTO;
import org.folio.innreach.mapper.InnReachTransactionMapper;
import org.folio.innreach.mapper.TransactionHoldMapper;
import org.folio.innreach.repository.InnReachTransactionRepository;
import org.folio.innreach.repository.TransactionHoldRepository;

@RequiredArgsConstructor
@Service
public class InnReachTransactionServiceImpl implements InnReachTransactionService {

  private final InnReachTransactionRepository repository;
  private final TransactionHoldRepository holdRepository;
  private final InnReachTransactionMapper mapper;
  private final TransactionHoldMapper transactionHoldMapper;
  private final CentralServerService centralServerService;

  //todo - reimplement the InnReachTransactionMapper to map it to a DTO with a correct transactionHold DTO

  private Map<TransactionType, Function<TransactionHold, TransactionHoldDTO>> itemHoldMappers;

  @PostConstruct
  public void initTransactionHoldMappers() {
     this.itemHoldMappers = Map.of(
      TransactionType.ITEM, hold -> transactionHoldMapper.toItemHoldDTO((TransactionItemHold) hold),
      TransactionType.LOCAL, hold -> transactionHoldMapper.toLocalHoldDTO((TransactionLocalHold) hold),
      TransactionType.PATRON, hold -> transactionHoldMapper.toPatronHoldDTO((TransactionPatronHold) hold)
    );
  }

  private InnReachTransaction createTransactionWithItemHold(String trackingId, String centralCode) {
    var transaction = new InnReachTransaction();
    transaction.setTrackingId(trackingId);
    transaction.setCentralServerCode(centralCode);
    transaction.setType(TransactionType.ITEM);
    transaction.setState(InnReachTransaction.TransactionState.ITEM_HOLD);
    return transaction;
  }

  @Override
  public InnReachResponseDTO createInnReachTransactionItemHold(String trackingId, String centralCode, TransactionItemHoldDTO dto) {
    var response = new InnReachResponseDTO();
    response.setStatus("ok");
    try {
      centralServerService.getCentralServerByCentralCode(centralCode);
      var transaction = createTransactionWithItemHold(trackingId, centralCode);
      var itemHold = mapper.toItemHold(dto);
      transaction.setHold(itemHold);
      repository.save(transaction);
    } catch (Exception e) {
      response.setStatus("failed");
      response.setReason(e.getMessage());
    }
    return response;
  }

  @Override
  public Integer countInnReachLoans(String patronId, List<UUID> loanIds) {
    return holdRepository.countByPatronIdAndFolioLoanIdIn(patronId, loanIds);
  }

  @Override
  public InnReachTransactionDTO getInnReachTransaction(UUID transactionId) {
    var innReachTransaction = repository.fetchOneById(transactionId)
      .orElseThrow(() -> new EntityNotFoundException(String.format("InnReach transaction with id [%s] not found!", transactionId)));

    var itemHoldDTO = itemHoldMappers.get(innReachTransaction.getType()).apply(innReachTransaction.getHold());

    return null;
  }
}
