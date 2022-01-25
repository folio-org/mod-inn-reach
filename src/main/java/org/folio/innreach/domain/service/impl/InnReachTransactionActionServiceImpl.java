package org.folio.innreach.domain.service.impl;

import static org.folio.innreach.domain.entity.InnReachTransaction.TransactionState.ITEM_HOLD;
import static org.folio.innreach.domain.entity.InnReachTransaction.TransactionState.ITEM_IN_TRANSIT;
import static org.folio.innreach.domain.entity.InnReachTransaction.TransactionState.ITEM_RECEIVED;
import static org.folio.innreach.domain.entity.InnReachTransaction.TransactionState.ITEM_SHIPPED;
import static org.folio.innreach.domain.entity.InnReachTransaction.TransactionState.PATRON_HOLD;
import static org.folio.innreach.domain.entity.InnReachTransaction.TransactionState.RECEIVE_UNANNOUNCED;
import static org.folio.innreach.domain.entity.InnReachTransaction.TransactionState.TRANSFER;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

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
import org.folio.innreach.domain.service.PatronHoldService;
import org.folio.innreach.domain.service.RequestService;
import org.folio.innreach.dto.ItemHoldCheckOutResponseDTO;
import org.folio.innreach.dto.LoanDTO;
import org.folio.innreach.dto.PatronHoldCheckInResponseDTO;
import org.folio.innreach.external.exception.InnReachException;
import org.folio.innreach.external.service.InnReachExternalService;
import org.folio.innreach.mapper.InnReachTransactionMapper;
import org.folio.innreach.repository.InnReachTransactionRepository;

@Log4j2
@Transactional
@RequiredArgsConstructor
@Service
public class InnReachTransactionActionServiceImpl implements InnReachTransactionActionService {

  private static final String D2IR_ITEM_RECEIVED_OPERATION = "itemreceived";
  private static final String D2IR_ITEM_SHIPPED_OPERATION = "itemshipped";
  private static final String D2IR_RECEIVE_UNSHIPPED_OPERATION = "receiveunshipped";
  private static final String D2IR_IN_TRANSIT = "intransit";

  private final InnReachTransactionRepository transactionRepository;
  private final InnReachTransactionMapper transactionMapper;
  private final InnReachExternalService innReachExternalService;
  private final RequestService requestService;
  private final PatronHoldService patronHoldService;

  @Override
  public PatronHoldCheckInResponseDTO checkInPatronHoldItem(UUID transactionId, UUID servicePointId) {
    var transaction = fetchTransactionById(transactionId);
    var state = transaction.getState();

    Assert.isTrue(state == ITEM_SHIPPED, "Unexpected transaction state: " + state);

    var response = checkInItem(transaction, servicePointId);

    transaction.setState(ITEM_RECEIVED);

    reportItemReceived(transaction);

    return response;
  }

  @Override
  public PatronHoldCheckInResponseDTO checkInPatronHoldUnshippedItem(UUID transactionId, UUID servicePointId, String itemBarcode) {
    var transaction = fetchTransactionById(transactionId);
    var state = transaction.getState();
    var folioItemBarcode = transaction.getHold().getFolioItemBarcode();

    Assert.isTrue(state == PATRON_HOLD || state == TRANSFER, "Unexpected transaction state: " + state);
    Assert.isTrue(folioItemBarcode == null, "Item associated with the transaction has a barcode assigned");

    patronHoldService.addItemBarcode(transaction, itemBarcode);

    var response = checkInItem(transaction, servicePointId);

    transaction.setState(RECEIVE_UNANNOUNCED);

    reportUnshippedItemReceived(transaction);

    return response;
  }

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

    reportItemShipped(transaction, itemBarcode, callNumber);

    return new ItemHoldCheckOutResponseDTO()
      .transaction(transactionMapper.toDTO(transaction))
      .folioCheckOut(checkOutResponse);
  }

  @Override
  public void associateNewLoanWithTransaction(LoanDTO loan) {
    updateAssociatedTransaction(loan, transaction -> {
      log.info("Associating a new loan {} with transaction {}", loan.getId(), transaction.getId());
      var hold = transaction.getHold();
      hold.setFolioLoanId(loan.getId());
      hold.setDueDateTime((int) loan.getDueDate().toInstant().getEpochSecond());
    });
  }

  @Override
  public void handleLoanUpdate(LoanDTO loan) {
    if ("checkedin".equals(loan.getAction()) && "Closed".equalsIgnoreCase(loan.getStatus().getName())) {
      updateAssociatedTransaction(loan, transaction -> {
        log.info("Updating transaction {} on loan closure {}", transaction.getId(), loan.getId());
        transaction.getHold().setDueDateTime(null);
        transaction.setState(ITEM_IN_TRANSIT);
        reportItemInTransit(transaction);
      });
    }
  }

  private void updateAssociatedTransaction(LoanDTO loan, Consumer<InnReachTransaction> transactionConsumer) {
    var itemId = loan.getItemId();
    var patronId = loan.getUserId();

    transactionRepository.fetchOpenByFolioItemIdAndPatronId(itemId, patronId)
      .ifPresent(transactionConsumer);
  }

  @Override
  public void borrowerRenewLoan(LoanDTO loan) {
    /*if (loan.getAction().equals("renewed")) {
      var folioLoanId = loan.getId();

      transactionRepository.fetchOneByLoanId(folioLoanId)
        .ifPresent(transaction -> {
          log.info("Loan {} associated with transaction ", folioLoanId);
          var transactionDueDate = Instant.ofEpochSecond(transaction.getHold().getDueDateTime());
          var loanDueDate = loan.getDueDate().toInstant().truncatedTo(ChronoUnit.SECONDS);
          if (!loanDueDate.equals(transactionDueDate))  {
            var loanIntegerDueDate = (int) (loan.getDueDate().getTime()/1000);
            String innReachRequestUri = resolveD2irCircPath("borrowerrenew", transaction.getTrackingId(), transaction.getCentralServerCode());
            innReachExternalService.postInnReachApi(transaction.getCentralServerCode(), innReachRequestUri, loanIntegerDueDate);
            transaction.setState(BORROWER_RENEW);
            transaction.getHold().setDueDateTime(loanIntegerDueDate);
            transactionRepository.save(transaction);
          }
        }); */
    }
  private PatronHoldCheckInResponseDTO checkInItem(InnReachTransaction transaction, UUID servicePointId) {
    var hold = (TransactionPatronHold) transaction.getHold();
    var shippedItemBarcode = hold.getShippedItemBarcode();
    var folioItemBarcode = hold.getFolioItemBarcode();

    Assert.isTrue(shippedItemBarcode != null, "shippedItemBarcode is not set");
    Assert.isTrue(folioItemBarcode != null, "folioItemBarcode is not set");

    var checkInResponse = requestService.checkInItem(transaction, servicePointId);

    return new PatronHoldCheckInResponseDTO()
      .transaction(transactionMapper.toDTO(transaction))
      .folioCheckIn(checkInResponse)
      .barcodeAugmented(!shippedItemBarcode.equals(folioItemBarcode));
  }

  private InnReachTransaction fetchTransactionById(UUID transactionId) {
    return transactionRepository.fetchOneById(transactionId)
      .orElseThrow(() -> new EntityNotFoundException("INN-Reach transaction is not found by id: " + transactionId));
  }

  private void reportItemReceived(InnReachTransaction transaction) {
    callD2irCircOperation(D2IR_ITEM_RECEIVED_OPERATION, transaction, null);
  }

  private void reportItemShipped(InnReachTransaction transaction, String itemBarcode, String callNumber) {
    var payload = new HashMap<>();
    payload.put("itemBarcode", itemBarcode);
    payload.put("callNumber", callNumber);

    callD2irCircOperation(D2IR_ITEM_SHIPPED_OPERATION, transaction, payload);
  }

  private void reportUnshippedItemReceived(InnReachTransaction transaction) {
    callD2irCircOperation(D2IR_RECEIVE_UNSHIPPED_OPERATION, transaction, null);
  }

  private void reportItemInTransit(InnReachTransaction transaction) {
    callD2irCircOperation(D2IR_IN_TRANSIT, transaction, null);
  }

  private void callD2irCircOperation(String operation, InnReachTransaction transaction, Map<Object, Object> payload) {
    var centralCode = transaction.getCentralServerCode();
    var trackingId = transaction.getTrackingId();
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
