package org.folio.innreach.domain.processor;

import static org.folio.innreach.domain.CirculationOperation.CANCEL_PATRON_HOLD;
import static org.folio.innreach.domain.entity.InnReachTransaction.TransactionState.CANCEL_REQUEST;

import java.util.function.Function;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import org.folio.innreach.domain.dto.folio.inventory.InventoryItemDTO;
import org.folio.innreach.domain.entity.InnReachTransaction;
import org.folio.innreach.domain.exception.EntityNotFoundException;
import org.folio.innreach.domain.service.InventoryService;
import org.folio.innreach.domain.service.RequestService;
import org.folio.innreach.dto.CirculationRequestDTO;
import org.folio.innreach.dto.Holding;
import org.folio.innreach.dto.InnReachResponseDTO;
import org.folio.innreach.repository.InnReachTransactionRepository;

@Log4j2
@Component
@RequiredArgsConstructor
public class PatronHoldCancelRequestProcessor implements InnReachCirculationProcessor {

  private final InnReachTransactionRepository transactionRepository;
  private final RequestService requestService;
  private final InventoryService inventoryService;

  @Override
  public boolean canProcess(String circulationOperationName) {
    return CANCEL_PATRON_HOLD.getOperationName().equals(circulationOperationName);
  }

  @Override
  @Transactional
  public InnReachResponseDTO process(String trackingId, String centralCode, CirculationRequestDTO request) {
    log.info("Cancelling request for transaction: {}", trackingId);
    var transaction = getInnReachTransaction(trackingId);
    transaction.setState(CANCEL_REQUEST);

    var itemId = transaction.getHold().getFolioItemId();

    requestService.cancelRequest(transaction, request.getReason());

    inventoryService.findItem(itemId)
      .map(removeItemTransactionInfo())
      .map(inventoryService::updateItem)
      .flatMap(item -> inventoryService.findHolding(item.getHoldingsRecordId()))
      .map(removeHoldingTransactionInfo())
      .ifPresent(inventoryService::updateHolding);

    log.info("Item request successfully cancelled");
    return new InnReachResponseDTO().status("ok").reason("success");
  }

  private InnReachTransaction getInnReachTransaction(String trackingId) {
    return transactionRepository.fetchOneByTrackingId(trackingId)
      .orElseThrow(() -> new EntityNotFoundException(
        String.format("INN-Reach transaction with trackingId [%s] not found", trackingId)));
  }

  private Function<Holding, Holding> removeHoldingTransactionInfo() {
    return holding -> {
      holding.setCallNumber(null);
      return holding;
    };
  }

  private Function<InventoryItemDTO, InventoryItemDTO> removeItemTransactionInfo() {
    return item -> {
      item.setCallNumber(null);
      item.setBarcode(null);
      return item;
    };
  }

}
