package org.folio.innreach.domain.service.impl;

import static java.util.Objects.nonNull;
import static org.apache.commons.lang3.StringUtils.capitalize;

import static org.folio.innreach.domain.entity.InnReachTransaction.TransactionState.CANCEL_REQUEST;
import static org.folio.innreach.domain.entity.InnReachTransaction.TransactionState.TRANSFER;

import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import org.folio.innreach.domain.dto.folio.inventory.InventoryItemDTO;
import org.folio.innreach.domain.entity.InnReachTransaction;
import org.folio.innreach.domain.entity.TransactionPatronHold;
import org.folio.innreach.domain.exception.EntityNotFoundException;
import org.folio.innreach.domain.service.CirculationService;
import org.folio.innreach.domain.service.InventoryService;
import org.folio.innreach.domain.service.PatronHoldService;
import org.folio.innreach.domain.service.RequestService;
import org.folio.innreach.dto.CancelRequestDTO;
import org.folio.innreach.dto.Holding;
import org.folio.innreach.dto.InnReachResponseDTO;
import org.folio.innreach.dto.InnReachTransactionReceiveItemDTO;
import org.folio.innreach.dto.ItemShippedDTO;
import org.folio.innreach.dto.PatronHoldDTO;
import org.folio.innreach.dto.TransactionHoldDTO;
import org.folio.innreach.dto.TransferRequestDTO;
import org.folio.innreach.external.exception.InnReachException;
import org.folio.innreach.external.service.InnReachExternalService;
import org.folio.innreach.mapper.InnReachTransactionHoldMapper;
import org.folio.innreach.mapper.InnReachTransactionMapper;
import org.folio.innreach.mapper.InnReachTransactionPickupLocationMapper;
import org.folio.innreach.repository.InnReachTransactionRepository;

@Log4j2
@Service
@Transactional
@RequiredArgsConstructor
public class CirculationServiceImpl implements CirculationService {

  private final InnReachTransactionRepository transactionRepository;
  private final InnReachTransactionMapper transactionMapper;
  private final InnReachTransactionHoldMapper transactionHoldMapper;
  private final InnReachTransactionPickupLocationMapper pickupLocationMapper;
  private final PatronHoldService patronHoldService;
  private final RequestService requestService;
  private final InventoryService inventoryService;
  private final InnReachExternalService innReachExternalService;

  @Override
  public InnReachResponseDTO initiatePatronHold(String trackingId, String centralCode, PatronHoldDTO patronHold) {
    var innReachTransaction = transactionRepository.findByTrackingIdAndCentralServerCode(trackingId, centralCode);
    var transactionHold = transactionHoldMapper.mapRequest(patronHold);

    if (innReachTransaction.isPresent()) {
      log.info("Transaction patron hold with trackingId [{}] and centralCode [{}] exists, start to update...",
          trackingId, centralCode);

      updateTransactionPatronHold((TransactionPatronHold) innReachTransaction.get().getHold(), transactionHold);

      patronHoldService.updateVirtualItems(innReachTransaction.get());
    } else {
      log.info("Transaction patron hold with trackingId [{}] and centralCode [{}] doesn't exist, create a new one...",
          trackingId, centralCode);

      InnReachTransaction newTransactionWithPatronHold = createTransactionWithPatronHold(trackingId, centralCode,
          transactionHold);
      var transaction = transactionRepository.save(newTransactionWithPatronHold);

      patronHoldService.createVirtualItems(transaction);
    }

    return success();
  }

  @Override
  public InnReachResponseDTO trackShippedItem(String trackingId, String centralCode, ItemShippedDTO itemShipped) {
    var innReachTransaction = getTransaction(trackingId, centralCode);

    var itemBarcode = itemShipped.getItemBarcode();
    var callNumber = itemShipped.getCallNumber();

    var transactionPatronHold = (TransactionPatronHold) innReachTransaction.getHold();

    if (nonNull(itemBarcode)) {
      var itemByBarcode = inventoryService.findItemByBarcode(itemBarcode);

      String folioItemBarcode = itemBarcode;
      if (itemByBarcode.isPresent()) {
        folioItemBarcode += transactionPatronHold.getItemAgencyCode();
      }

      transactionPatronHold.setShippedItemBarcode(itemBarcode);

      transactionPatronHold.setFolioItemBarcode(folioItemBarcode);
      updateFolioAssociatedItem(transactionPatronHold.getFolioItemId(), folioItemBarcode);
    }

    if (nonNull(callNumber)) {
      transactionPatronHold.setCallNumber(callNumber);
    }

    innReachTransaction.setState(InnReachTransaction.TransactionState.ITEM_SHIPPED);

    return success();
  }

  @Override
  public InnReachResponseDTO cancelTransaction(String trackingId, String centralCode, CancelRequestDTO cancelRequest) {
    log.info("Cancelling request for transaction: {}", trackingId);

    var transaction = getTransaction(trackingId, centralCode);

    transaction.setState(CANCEL_REQUEST);

    var itemId = transaction.getHold().getFolioItemId();

    requestService.cancelRequest(transaction, cancelRequest.getReason());

    inventoryService.findItem(itemId)
        .map(removeItemTransactionInfo())
        .map(inventoryService::updateItem)
        .flatMap(item -> inventoryService.findHolding(item.getHoldingsRecordId()))
        .map(removeHoldingTransactionInfo())
        .ifPresent(inventoryService::updateHolding);

    log.info("Item request successfully cancelled");

    return success();
  }

  @Override
  public InnReachResponseDTO transferItem(String trackingId, String centralCode, TransferRequestDTO request) {
    var transaction = getTransaction(trackingId, centralCode);

    validateEquals(request::getItemId, () -> transaction.getHold().getItemId(), "item id");
    validateEquals(request::getItemAgencyCode, () -> transaction.getHold().getItemAgencyCode(), "item agency code");

    transaction.getHold().setItemId(request.getNewItemId());
    transaction.setState(TRANSFER);

    return success();
  }

  @Override
  public InnReachTransactionReceiveItemDTO receivePatronHoldItem(UUID transactionId, UUID servicePointId) {
    var transaction = transactionRepository.fetchOneById(transactionId)
      .orElseThrow(() -> new EntityNotFoundException(String.format("InnReach transaction with id [%s] not found!", transactionId)));

    Assert.isTrue(transaction.getState() == InnReachTransaction.TransactionState.ITEM_SHIPPED,
      "Unexpected transaction state: " + transaction.getState());

    var hold = (TransactionPatronHold) transaction.getHold();

    var shippedItemBarcode = hold.getShippedItemBarcode();
    var folioItemBarcode = hold.getFolioItemBarcode();

    Assert.isTrue(shippedItemBarcode != null, "shippedItemBarcode is not set");
    Assert.isTrue(folioItemBarcode != null, "folioItemBarcode is not set");

    var checkInResponse = requestService.checkInItem(transaction, servicePointId);

    transaction.setState(InnReachTransaction.TransactionState.ITEM_RECEIVED);

    transactionRepository.save(transaction);

    reportItemReceived(transaction);

    return new InnReachTransactionReceiveItemDTO()
      .transaction(transactionMapper.toDTO(transaction))
      .folioCheckIn(checkInResponse)
      .barcodeAugmented(!shippedItemBarcode.equals(folioItemBarcode));
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

  private String resolveItemReceivedPath(String trackingId, String centralServerCode) {
    return String.format("/circ/itemreceived/%s/%s", trackingId, centralServerCode);
  }

  private InnReachResponseDTO success() {
    return new InnReachResponseDTO().status("ok").reason("success");
  }

  private void updateTransactionPatronHold(TransactionPatronHold existingTransactionPatronHold,
      TransactionHoldDTO transactionHold) {
    // update transaction patron hold
    BeanUtils.copyProperties(transactionHold, existingTransactionPatronHold,
        "pickupLocation", "id", "createdBy", "updatedBy", "createdDate", "updatedDate",
        "folioPatronId", "folioInstanceId", "folioHoldingId", "folioItemId",
        "folioRequestId", "folioLoanId", "folioPatronBarcode", "folioItemBarcode");

    // update pickupLocation
    var pickupLocation = pickupLocationMapper.fromString(transactionHold.getPickupLocation());
    BeanUtils.copyProperties(pickupLocation, existingTransactionPatronHold.getPickupLocation(),
        "id", "createdBy", "updatedBy", "createdDate", "updatedDate");
  }

  private InnReachTransaction createTransactionWithPatronHold(String trackingId, String centralCode,
      TransactionHoldDTO transactionHold) {
    var newInnReachTransaction = new InnReachTransaction();
    newInnReachTransaction.setHold(transactionHoldMapper.toPatronHold(transactionHold));
    newInnReachTransaction.setCentralServerCode(centralCode);
    newInnReachTransaction.setTrackingId(trackingId);
    newInnReachTransaction.setState(InnReachTransaction.TransactionState.PATRON_HOLD);
    newInnReachTransaction.setType(InnReachTransaction.TransactionType.PATRON);
    return newInnReachTransaction;
  }

  private void updateFolioAssociatedItem(UUID folioItemId, String itemBarcode) {
    var folioAssociatedItem = inventoryService.findItem(folioItemId);
    folioAssociatedItem.ifPresent(item -> {
      item.setBarcode(itemBarcode);
      inventoryService.updateItem(item);
    });
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

  private <T> void validateEquals(Supplier<T> requestField, Supplier<T> trxField, String fieldName) {
    T reqValue = requestField.get();
    T trxValue = trxField.get();

    Assert.isTrue(Objects.equals(reqValue, trxValue),
        String.format("%s [%s] from the request doesn't match with %s [%s] in the stored transaction",
            capitalize(fieldName), reqValue, fieldName.toLowerCase(), trxValue));
  }

  private InnReachTransaction getTransaction(String trackingId, String centralCode) {
    return transactionRepository.findByTrackingIdAndCentralServerCode(trackingId, centralCode)
        .orElseThrow(() -> new EntityNotFoundException(String.format(
            "InnReach transaction with tracking id [%s] and central code [%s] not found", trackingId, centralCode)));
  }

}
