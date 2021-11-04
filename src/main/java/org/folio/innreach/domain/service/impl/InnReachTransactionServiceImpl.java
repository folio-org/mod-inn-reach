package org.folio.innreach.domain.service.impl;

import java.util.List;
import java.util.UUID;

import javax.persistence.EntityExistsException;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.innreach.domain.service.MaterialTypeMappingService;
import org.folio.innreach.dto.InnReachTransactionFilterParametersDTO;
import org.folio.innreach.dto.InnReachTransactionsDTO;
import org.folio.innreach.external.service.InventoryService;
import org.folio.innreach.mapper.InnReachErrorMapper;
import org.folio.innreach.mapper.InnReachTransactionFilterParametersMapper;
import org.folio.innreach.specification.InnReachTransactionSpecification;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.folio.innreach.domain.entity.InnReachTransaction;
import org.folio.innreach.domain.entity.InnReachTransaction.TransactionType;
import org.folio.innreach.domain.exception.EntityNotFoundException;
import org.folio.innreach.domain.service.CentralServerService;
import org.folio.innreach.domain.service.InnReachTransactionService;
import org.folio.innreach.dto.InnReachResponseDTO;
import org.folio.innreach.dto.InnReachTransactionDTO;
import org.folio.innreach.dto.TransactionHoldDTO;
import org.folio.innreach.mapper.InnReachTransactionHoldMapper;
import org.folio.innreach.mapper.InnReachTransactionMapper;
import org.folio.innreach.repository.InnReachTransactionRepository;
import org.folio.innreach.repository.TransactionHoldRepository;

@Log4j2
@RequiredArgsConstructor
@Service
public class InnReachTransactionServiceImpl implements InnReachTransactionService {

  private final InnReachTransactionRepository repository;
  private final TransactionHoldRepository holdRepository;

  private final InnReachTransactionMapper transactionMapper;
  private final InnReachTransactionHoldMapper transactionHoldMapper;
  private final InnReachErrorMapper errorMapper;
  private final InnReachTransactionFilterParametersMapper parametersMapper;

  private final CentralServerService centralServerService;
  private final MaterialTypeMappingService materialService;
  private final InventoryService inventoryService;

  private final InnReachTransactionSpecification specification;

  private InnReachTransaction createTransactionWithItemHold(String trackingId, String centralCode) {
    var transaction = new InnReachTransaction();
    transaction.setTrackingId(trackingId);
    transaction.setCentralServerCode(centralCode);
    transaction.setType(TransactionType.ITEM);
    transaction.setState(InnReachTransaction.TransactionState.ITEM_HOLD);
    return transaction;
  }

  @Override
  public InnReachResponseDTO createInnReachTransactionItemHold(String trackingId, String centralCode, TransactionHoldDTO dto) {
    var response = new InnReachResponseDTO();
    response.setStatus("ok");
    try {
      repository.fetchOneByTrackingId(trackingId).ifPresent(m -> {
        throw new EntityExistsException("INN-Reach Transaction with tracking ID = " + trackingId
          + " already exists.");
      });
      var centralServer = centralServerService.getCentralServerByCentralCode(centralCode);
      var centralServerId = centralServer.getId();
      var transaction = createTransactionWithItemHold(trackingId, centralCode);
      var itemHold = transactionHoldMapper.toItemHold(dto);
      var item = inventoryService.getItemByHrId(itemHold.getItemId());
      var materialTypeId = item.getMaterialType().getId();
      var materialType = materialService.findByCentralServerAndMaterialType(centralServerId, materialTypeId);
      itemHold.setCentralItemType(materialType.getCentralItemType());
      transaction.setHold(itemHold);
      repository.save(transaction);
    } catch (Exception e) {
      log.warn("An error occurred during creation of INN-Reach Transaction.", e);
      response.setStatus("failed");
      response.setReason("An error occurred during creation of INN-Reach Transaction.");
      var innReachError = errorMapper.toInnReachError(e);
      response.setErrors(List.of(innReachError));
    }
    return response;
  }

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
}
