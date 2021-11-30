package org.folio.innreach.domain.service.impl;

import static org.folio.innreach.domain.entity.InnReachTransaction.TransactionState.ITEM_HOLD;
import static org.folio.innreach.domain.entity.InnReachTransaction.TransactionState.ITEM_RECEIVED;
import static org.folio.innreach.domain.entity.InnReachTransaction.TransactionState.ITEM_SHIPPED;
import static org.folio.innreach.domain.entity.InnReachTransaction.TransactionState.TRANSFER;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import org.folio.innreach.domain.entity.TransactionItemHold;
import org.folio.innreach.domain.entity.TransactionPatronHold;
import org.folio.innreach.domain.exception.EntityNotFoundException;
import org.folio.innreach.domain.service.InnReachTransactionActionService;
import org.folio.innreach.domain.service.RequestService;
import org.folio.innreach.dto.ItemHoldCheckOutResponseDTO;
import org.folio.innreach.dto.PatronHoldCheckInResponseDTO;
import org.folio.innreach.external.exception.InnReachException;
import org.folio.innreach.external.service.InnReachExternalService;
import org.folio.innreach.mapper.InnReachTransactionMapper;
import org.folio.innreach.repository.InnReachTransactionRepository;

@Log4j2
@RequiredArgsConstructor
@Service
public class InnReachTransactionActionServiceImpl implements InnReachTransactionActionService {

  private static final String D2IR_ITEM_RECEIVED_OPERATION = "itemreceived";
  private static final String D2IR_ITEM_SHIPPED_OPERATION = "itemshipped";

  private final InnReachTransactionRepository transactionRepository;
  private final InnReachTransactionMapper transactionMapper;
  private final InnReachExternalService innReachExternalService;
  private final RequestService requestService;

  @Transactional
  @Override
  public PatronHoldCheckInResponseDTO checkInPatronHoldItem(UUID transactionId, UUID servicePointId) {
    var transaction = transactionRepository.fetchOneById(transactionId)
      .orElseThrow(() -> new EntityNotFoundException(String.format("InnReach transaction with id [%s] not found!", transactionId)));

    var hold = (TransactionPatronHold) transaction.getHold();
    var state = transaction.getState();
    var shippedItemBarcode = hold.getShippedItemBarcode();
    var folioItemBarcode = hold.getFolioItemBarcode();

    Assert.isTrue(state == ITEM_SHIPPED, "Unexpected transaction state: " + state);
    Assert.isTrue(shippedItemBarcode != null, "shippedItemBarcode is not set");
    Assert.isTrue(folioItemBarcode != null, "folioItemBarcode is not set");

    var checkInResponse = requestService.checkInItem(transaction, servicePointId);

    transaction.setState(ITEM_RECEIVED);

    reportItemReceived(transaction.getCentralServerCode(), transaction.getTrackingId());

    return new PatronHoldCheckInResponseDTO()
      .transaction(transactionMapper.toDTO(transaction))
      .folioCheckIn(checkInResponse)
      .barcodeAugmented(!shippedItemBarcode.equals(folioItemBarcode));
  }

  @Transactional
  @Override
  public ItemHoldCheckOutResponseDTO checkOutItemHoldItem(String itemBarcode, UUID servicePointId) {
    var transaction = transactionRepository.fetchOneByFolioItemBarcode(itemBarcode)
      .orElseThrow(() -> new EntityNotFoundException("INN-Reach transaction is not found by itemBarcode: " + itemBarcode));

    var hold = (TransactionItemHold) transaction.getHold();
    var state = transaction.getState();
    var folioPatronBarcode = hold.getFolioPatronBarcode();

    Assert.isTrue(state == ITEM_HOLD || state == TRANSFER, "Unexpected transaction state: " + state);
    Assert.isTrue(folioPatronBarcode != null, "folioPatronBarcode is not set");

    var checkOutResponse = requestService.checkOutItem(transaction, servicePointId);
    var callNumber = checkOutResponse.getItem().getCallNumber();

    hold.setFolioLoanId(checkOutResponse.getId());

    transaction.setState(ITEM_SHIPPED);

    reportItemShipped(transaction.getCentralServerCode(), transaction.getTrackingId(), itemBarcode, callNumber);

    return new ItemHoldCheckOutResponseDTO()
      .transaction(transactionMapper.toDTO(transaction))
      .folioCheckOut(checkOutResponse);
  }

  private void reportItemReceived(String centralCode, String trackingId) {
    callD2irCircOperation(D2IR_ITEM_RECEIVED_OPERATION, centralCode, trackingId, null);
  }

  private void reportItemShipped(String centralCode, String trackingId, String itemBarcode, String callNumber) {
    var payload = new HashMap<>();
    payload.put("itemBarcode", itemBarcode);
    payload.put("callNumber", callNumber);

    callD2irCircOperation(D2IR_ITEM_SHIPPED_OPERATION, centralCode, trackingId, payload);
  }

  private void callD2irCircOperation(String operation, String centralCode, String trackingId, Map<Object, Object> payload) {
    var requestPath = resolveD2irCircPath(operation, trackingId, centralCode);
    try {
      if (payload == null) {
        innReachExternalService.postInnReachApi(centralCode, requestPath);
      } else {
        innReachExternalService.postInnReachApi(centralCode, requestPath, payload);
      }
    } catch (InnReachException e) {
      //TODO: the suppression of error is temporal, see https://issues.folio.org/browse/MODINREACH-192 for more details.
      log.warn("Unexpected D2IR response: {}", e.getMessage(), e);
    }
  }

  private String resolveD2irCircPath(String operation, String trackingId, String centralCode) {
    return String.format("/circ/%s/%s/%s", operation, trackingId, centralCode);
  }

}
