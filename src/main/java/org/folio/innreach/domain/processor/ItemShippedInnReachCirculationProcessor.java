package org.folio.innreach.domain.processor;

import java.util.Objects;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.folio.innreach.client.InventoryClient;
import org.folio.innreach.domain.CirculationOperation;
import org.folio.innreach.domain.entity.InnReachTransaction;
import org.folio.innreach.domain.entity.TransactionPatronHold;
import org.folio.innreach.domain.exception.EntityNotFoundException;
import org.folio.innreach.dto.InnReachResponseDTO;
import org.folio.innreach.dto.TransactionHoldDTO;
import org.folio.innreach.repository.InnReachTransactionRepository;

@Log4j2
@Service
@RequiredArgsConstructor
public class ItemShippedInnReachCirculationProcessor implements InnReachCirculationProcessor {

  private final InnReachTransactionRepository transactionRepository;
  private final InventoryClient inventoryClient;

  @Override
  public boolean canProcess(String circulationOperationName) {
    return CirculationOperation.ITEM_SHIPPED.getOperationName().equals(circulationOperationName);
  }

  @Override
  @Transactional
  public InnReachResponseDTO process(String trackingId, String centralCode, TransactionHoldDTO transactionHold) {
    var innReachTransaction = transactionRepository.findByTrackingIdAndCentralServerCode(trackingId, centralCode)
        .orElseThrow(() -> new EntityNotFoundException(String.format(
            "InnReach transaction with trackingId [%s] and centralCode [%s] not found", trackingId, centralCode)));

    var itemBarcode = transactionHold.getShippedItemBarcode();

    var transactionPatronHold = (TransactionPatronHold) innReachTransaction.getHold();
    transactionPatronHold.setShippedItemBarcode(itemBarcode);

    if (Objects.nonNull(itemBarcode)) {
      var itemByBarcode =  inventoryClient.getItemByBarcode(itemBarcode);
      if (Objects.nonNull(itemByBarcode)) {
        transactionPatronHold.setShippedItemBarcode(itemBarcode + transactionPatronHold.getItemAgencyCode());
      }
    }

    updateFolioAssociatedItem(transactionPatronHold.getFolioItemId(), itemBarcode);

    innReachTransaction.setState(InnReachTransaction.TransactionState.ITEM_SHIPPED);

    return new InnReachResponseDTO().status("ok").reason("success");
  }

  private void updateFolioAssociatedItem(UUID folioItemId, String itemBarcode) {
    var folioAssociatedItem = inventoryClient.getItemById(folioItemId);
    if (Objects.nonNull(folioAssociatedItem)) {
      folioAssociatedItem.setBarcode(itemBarcode);
      inventoryClient.updateItem(folioAssociatedItem.getId(), folioAssociatedItem);
    }
  }

}
