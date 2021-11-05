package org.folio.innreach.domain.processor;

import static org.folio.innreach.domain.CirculationOperation.CANCEL_PATRON_HOLD;

import java.util.function.Function;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;

import org.folio.innreach.domain.service.InventoryStorageService;
import org.folio.innreach.domain.service.RequestService;
import org.folio.innreach.dto.CirculationRequestDTO;
import org.folio.innreach.dto.Holding;
import org.folio.innreach.dto.InnReachResponseDTO;
import org.folio.innreach.dto.Item;
import org.folio.innreach.repository.InnReachTransactionRepository;

@Log4j2
@Component
@RequiredArgsConstructor
public class PatronHoldCancelRequestProcessor implements InnReachCirculationProcessor {

  private final InnReachTransactionRepository transactionRepository;
  private final RequestService requestService;
  private final InventoryStorageService inventoryStorageService;

  @Override
  public boolean canProcess(String circulationOperationName) {
    return CANCEL_PATRON_HOLD.getOperationName().equals(circulationOperationName);
  }

  @Override
  public InnReachResponseDTO process(String trackingId, String centralCode, CirculationRequestDTO request) {
    var transaction = transactionRepository.fetchOneByTrackingId(trackingId).get();
    var hold = transaction.getHold();
    var itemId = hold.getFolioItemId();

    requestService.cancelRequest(transaction, request.getReason());

    inventoryStorageService.findItem(itemId)
      .map(removeItemTransactionInfo())
      .map(inventoryStorageService::updateItem)
      .flatMap(item -> inventoryStorageService.findHolding(item.getHoldingsRecordId()))
      .map(removeHoldingTransactionInfo())
      .ifPresent(inventoryStorageService::updateHolding);

    return new InnReachResponseDTO().status("ok").reason("success");
  }

  private Function<Holding, Holding> removeHoldingTransactionInfo() {
    return holding -> {
      holding.setCallNumber(null);
      return holding;
    };
  }

  private Function<Item, Item> removeItemTransactionInfo() {
    return item -> {
      item.setEffectiveCallNumberComponents(null);
      item.setBarcode(null);
      return item;
    };
  }

}
