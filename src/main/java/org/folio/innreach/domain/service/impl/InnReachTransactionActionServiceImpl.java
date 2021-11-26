package org.folio.innreach.domain.service.impl;

import static org.folio.innreach.domain.entity.InnReachTransaction.TransactionState.ITEM_HOLD;
import static org.folio.innreach.domain.entity.InnReachTransaction.TransactionState.ITEM_RECEIVED;
import static org.folio.innreach.domain.entity.InnReachTransaction.TransactionState.ITEM_SHIPPED;
import static org.folio.innreach.domain.entity.InnReachTransaction.TransactionState.TRANSFER;

import java.util.HashMap;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import org.folio.innreach.domain.entity.InnReachTransaction;
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

    reportItemReceived(transaction);

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

    reportItemShipped(transaction.getTrackingId(), transaction.getCentralServerCode(), itemBarcode, callNumber);

    return new ItemHoldCheckOutResponseDTO()
      .transaction(transactionMapper.toDTO(transaction))
      .folioCheckOut(checkOutResponse);
  }

  private void reportItemReceived(InnReachTransaction transaction) {
    var centralCode = transaction.getCentralServerCode();
    var requestPath = resolveItemReceivedPath(transaction.getTrackingId(), centralCode);

    try {
      innReachExternalService.postInnReachApi(centralCode, requestPath);
    } catch (InnReachException e) {
      log.warn("Unexpected D2IR response: {}", e.getMessage(), e);
    }
  }

  private void reportItemShipped(String trackingId, String centralCode, String itemBarcode, String callNumber) {
    var requestPath = resolveItemShippedPath(trackingId, centralCode);

    try {
      var payload = new HashMap<>();
      payload.put("itemBarcode", itemBarcode);
      payload.put("callNumber", callNumber);

      innReachExternalService.postInnReachApi(centralCode, requestPath, payload);
    } catch (InnReachException e) {
      log.warn("Unexpected D2IR response: {}", e.getMessage(), e);
    }
  }

  private String resolveItemReceivedPath(String trackingId, String centralServerCode) {
    return String.format("/circ/itemreceived/%s/%s", trackingId, centralServerCode);
  }

  private String resolveItemShippedPath(String trackingId, String centralServerCode) {
    return String.format("/circ/itemshipped/%s/%s", trackingId, centralServerCode);
  }

}
