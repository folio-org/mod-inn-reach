package org.folio.innreach.domain.service.impl;

import java.util.List;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.innreach.client.InventoryClient;
import org.folio.innreach.domain.exception.EntityNotFoundException;
import org.folio.innreach.repository.MaterialTypeMappingRepository;
import org.springframework.stereotype.Service;

import org.folio.innreach.domain.entity.InnReachTransaction;
import org.folio.innreach.domain.entity.InnReachTransaction.TransactionType;
import org.folio.innreach.domain.service.CentralServerService;
import org.folio.innreach.domain.service.InnReachTransactionService;
import org.folio.innreach.dto.InnReachResponseDTO;
import org.folio.innreach.dto.TransactionItemHoldDTO;
import org.folio.innreach.mapper.InnReachTransactionMapper;
import org.folio.innreach.repository.InnReachTransactionRepository;
import org.folio.innreach.repository.TransactionHoldRepository;

@Log4j2
@RequiredArgsConstructor
@Service
public class InnReachTransactionServiceImpl implements InnReachTransactionService {

  private final InnReachTransactionRepository repository;
  private final TransactionHoldRepository holdRepository;
  private final InnReachTransactionMapper mapper;
  private final CentralServerService centralServerService;

  private final InventoryClient inventoryClient;
  private final MaterialTypeMappingRepository materialRepository;

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
      var centralServer = centralServerService.getCentralServerByCentralCode(centralCode);
      var transaction = createTransactionWithItemHold(trackingId, centralCode);
      var itemHold = mapper.toItemHold(dto);
      var item = inventoryClient.getItemByHrId(itemHold.getItemId());
      var materialType = materialRepository.findOneByCentralServerIdAndMaterialTypeId(
        centralServer.getId(), item.getMaterialType().getId()).orElseThrow(
        () -> new EntityNotFoundException("Material type mapping for central server id = " + centralServer.getId()
          + " and material type id = " + item.getMaterialType().getId() + " not found")
      );
      itemHold.setCentralItemType(materialType.getCentralItemType());
      transaction.setHold(itemHold);
      repository.save(transaction);
    } catch (Exception e) {
      log.warn(e.getMessage());
      response.setStatus("failed");
      response.setReason(e.getMessage());
    }
    return response;
  }

  @Override
  public Integer countInnReachLoans(String patronId, List<UUID> loanIds) {
    return holdRepository.countByPatronIdAndFolioLoanIdIn(patronId, loanIds);
  }
}
