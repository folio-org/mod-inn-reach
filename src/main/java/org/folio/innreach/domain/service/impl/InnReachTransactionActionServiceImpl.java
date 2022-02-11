package org.folio.innreach.domain.service.impl;

import static java.util.Optional.ofNullable;

import static org.folio.innreach.domain.dto.folio.circulation.RequestDTO.RequestStatus.CLOSED_CANCELLED;
import static org.folio.innreach.domain.dto.folio.circulation.RequestDTO.RequestStatus.CLOSED_PICKUP_EXPIRED;
import static org.folio.innreach.domain.entity.InnReachTransaction.TransactionState.BORROWER_RENEW;
import static org.folio.innreach.domain.entity.InnReachTransaction.TransactionState.BORROWING_SITE_CANCEL;
import static org.folio.innreach.domain.entity.InnReachTransaction.TransactionState.CANCEL_REQUEST;
import static org.folio.innreach.domain.entity.InnReachTransaction.TransactionState.CLAIMS_RETURNED;
import static org.folio.innreach.domain.entity.InnReachTransaction.TransactionState.FINAL_CHECKIN;
import static org.folio.innreach.domain.entity.InnReachTransaction.TransactionState.ITEM_HOLD;
import static org.folio.innreach.domain.entity.InnReachTransaction.TransactionState.ITEM_IN_TRANSIT;
import static org.folio.innreach.domain.entity.InnReachTransaction.TransactionState.ITEM_RECEIVED;
import static org.folio.innreach.domain.entity.InnReachTransaction.TransactionState.ITEM_SHIPPED;
import static org.folio.innreach.domain.entity.InnReachTransaction.TransactionState.PATRON_HOLD;
import static org.folio.innreach.domain.entity.InnReachTransaction.TransactionState.RECEIVE_UNANNOUNCED;
import static org.folio.innreach.domain.entity.InnReachTransaction.TransactionState.RETURN_UNCIRCULATED;
import static org.folio.innreach.domain.entity.InnReachTransaction.TransactionState.TRANSFER;
import static org.folio.innreach.domain.entity.InnReachTransaction.TransactionType.ITEM;
import static org.folio.innreach.domain.entity.InnReachTransaction.TransactionType.PATRON;
import static org.folio.innreach.dto.ItemStatus.NameEnum.AWAITING_PICKUP;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import org.folio.innreach.client.InstanceStorageClient;
import org.folio.innreach.domain.dto.folio.circulation.RequestDTO;
import org.folio.innreach.domain.dto.folio.inventory.InventoryItemDTO;
import org.folio.innreach.domain.entity.InnReachTransaction;
import org.folio.innreach.domain.entity.TransactionItemHold;
import org.folio.innreach.domain.entity.TransactionPatronHold;
import org.folio.innreach.domain.exception.EntityNotFoundException;
import org.folio.innreach.domain.service.InnReachTransactionActionService;
import org.folio.innreach.domain.service.ItemService;
import org.folio.innreach.domain.service.PatronHoldService;
import org.folio.innreach.domain.service.RequestService;
import org.folio.innreach.dto.CheckInDTO;
import org.folio.innreach.dto.ItemHoldCheckOutResponseDTO;
import org.folio.innreach.dto.LoanDTO;
import org.folio.innreach.dto.LoanStatus;
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
  private static final String D2IR_BORROWER_RENEW = "borrowerrenew";
  private static final String D2IR_FINAL_CHECK_IN = "finalcheckin";
  private static final String D2IR_TRASFER_REQUEST = "transferrequest";
  private static final String D2IR_RETURN_UNCIRCULATED = "returnuncirculated";
  private static final String D2IR_OWNING_SITE_CANCEL = "owningsitecancel";
  private static final String D2IR_CLAIMS_RETURNED = "claimsreturned";
  private static final String D2IR_CANCEL_ITEM_HOLD = "cancelitemhold";

  private final InnReachTransactionRepository transactionRepository;
  private final InnReachTransactionMapper transactionMapper;
  private final InnReachExternalService innReachExternalService;
  private final RequestService requestService;
  private final PatronHoldService patronHoldService;
  private final ItemService itemService;
  private final InstanceStorageClient instanceStorageClient;

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
    var itemId = loan.getItemId();
    var patronId = loan.getUserId();

    var transaction = transactionRepository.fetchActiveByFolioItemIdAndPatronId(itemId, patronId).orElse(null);
    if (transaction == null || transaction.getType() != PATRON) {
      return;
    }

    log.info("Associating a new loan {} with patron transaction {}", loan.getId(), transaction.getId());

    var hold = transaction.getHold();
    hold.setFolioLoanId(loan.getId());
    hold.setDueDateTime(toEpochSec(loan.getDueDate()));
  }

  @Override
  public void handleLoanUpdate(LoanDTO loan) {
    var transaction = transactionRepository.fetchActiveByLoanId(loan.getId()).orElse(null);
    if (transaction == null) {
      return;
    }

    var loanAction = loan.getAction();
    var loanStatus = ofNullable(loan.getStatus()).map(LoanStatus::getName).orElse(null);

    if ("checkedin".equalsIgnoreCase(loanAction) && "Closed".equalsIgnoreCase(loanStatus)) {
      updateTransactionOnLoanClosure(loan, transaction);
    } else if ("renewed".equalsIgnoreCase(loanAction)) {
      updateTransactionOnLoanRenewal(loan, transaction);
    } else if ("claimedReturned".equalsIgnoreCase(loanAction)) {
      updateTransactionOnLoanClaimedReturned(loan, transaction);
    }
  }

  @Override
  public void handleRequestUpdate(RequestDTO requestDTO) {

    var transaction = transactionRepository.fetchActiveByRequestId(requestDTO.getId()).orElse(null);
    if (transaction == null) {
      return;
    }

    if (transaction.getType() == ITEM) {
      updateItemTransactionOnRequestChange(requestDTO, transaction);
    } else if (transaction.getType() == PATRON) {
      updatePatronTransactionOnRequestChange(requestDTO, transaction);
    }
  }

  @Override
  public void handleCheckInCreation(CheckInDTO checkIn) {
    var itemId = checkIn.getItemId();
    var itemStatusPriorToCheckIn = checkIn.getItemStatusPriorToCheckIn();

    if (AWAITING_PICKUP.getValue().equalsIgnoreCase(itemStatusPriorToCheckIn)) {
      var transaction = transactionRepository.fetchActiveByFolioItemId(itemId).orElse(null);

      if (transaction == null || transaction.getType() != PATRON) {
        return;
      }

      var requestId = transaction.getHold().getFolioRequestId();
      var request = requestService.findRequest(requestId);
      var requestStatus = request.getStatus();

      if (requestStatus == CLOSED_PICKUP_EXPIRED || requestStatus == CLOSED_CANCELLED) {
        log.info("Updating transaction {} on the hold shelf clearance for uncirculated items, check-in {}",
          transaction.getId(), checkIn.getId());

        transaction.setState(RETURN_UNCIRCULATED);

        reportReturnUncirculated(transaction);
      }
    }
  }

  private void updateTransactionOnLoanClaimedReturned(LoanDTO loan, InnReachTransaction transaction) {
    if (transaction.getType() != PATRON) {
      return;
    }

    log.info("Updating patron transaction {} on the claimed returned loan {}", transaction.getId(), loan.getId());

    transaction.setState(CLAIMS_RETURNED);

    reportClaimsReturned(transaction, toEpochSec(new Date()));
  }

  private void updateTransactionOnLoanRenewal(LoanDTO loan, InnReachTransaction transaction) {
    if (transaction.getType() != PATRON) {
      return;
    }

    log.info("Updating patron transaction {} on the renewal of loan {}", transaction.getId(), loan.getId());

    var transactionDueDate = Instant.ofEpochSecond(transaction.getHold().getDueDateTime());
    var loanDueDate = toInstantTruncatedToSec(loan.getDueDate());
    if (!loanDueDate.equals(transactionDueDate)) {
      var loanIntegerDueDate = (int) loanDueDate.getEpochSecond();

      transaction.setState(BORROWER_RENEW);
      transaction.getHold().setDueDateTime(loanIntegerDueDate);

      reportBorrowerRenew(transaction, loanIntegerDueDate);
    }
  }

  private void updateTransactionOnLoanClosure(LoanDTO loan, InnReachTransaction transaction) {
    if (transaction.getType() == ITEM) {
      log.info("Updating item transaction {} on loan closure {}", transaction.getId(), loan.getId());

      var hold = (TransactionItemHold) transaction.getHold();
      hold.setPatronName(null);
      hold.setPatronId(null);
      hold.setDueDateTime(null);

      transaction.setState(FINAL_CHECKIN);

      reportFinalCheckIn(transaction);
    } else if (transaction.getType() == PATRON) {
      log.info("Updating patron transaction {} on loan closure {}", transaction.getId(), loan.getId());

      transaction.getHold().setDueDateTime(null);
      transaction.setState(ITEM_IN_TRANSIT);

      reportItemInTransit(transaction);
    }
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

  private void updateItemTransactionOnRequestChange(RequestDTO requestDTO, InnReachTransaction transaction) {
    var requestId = requestDTO.getId();
    var itemId = requestDTO.getItemId();
    if (requestDTO.getStatus() == CLOSED_CANCELLED) {
      log.info("Updating item hold transaction {} on cancellation of a request {}", transaction.getId(), requestDTO.getId());
      var transactionItemHold = (TransactionItemHold) transaction.getHold();
      var instance = instanceStorageClient.getInstanceById(requestDTO.getInstanceId());
      transaction.setState(CANCEL_REQUEST);

      reportOwningSiteCancel(transaction, instance.getHrid(), transactionItemHold.getPatronName());
      return;
    }

    var hold = transaction.getHold();
    if (!hold.getFolioItemId().equals(itemId)) {
      log.info("Updating transaction {} on moving a request {} from one item to another", transaction.getId(), requestId);

      var item = fetchItemById(requestDTO.getItemId());

      hold.setFolioItemId(itemId);
      hold.setFolioInstanceId(requestDTO.getInstanceId());
      hold.setFolioHoldingId(requestDTO.getHoldingsRecordId());
      hold.setFolioItemBarcode(item.getBarcode());
      transaction.setState(TRANSFER);

      reportTransferRequest(transaction, item.getHrid());
    }
  }

  private void updatePatronTransactionOnRequestChange(RequestDTO requestDTO, InnReachTransaction transaction) {
    if (requestDTO.getStatus() == CLOSED_CANCELLED) {
      log.info("Updating patron hold transaction {} on cancellation of a request {}", transaction.getId(), requestDTO.getId());
      transaction.setState(BORROWING_SITE_CANCEL);
      reportCancelItemHold(transaction);
    }
  }

  private static Instant toInstantTruncatedToSec(Date date) {
    return date.toInstant().truncatedTo(ChronoUnit.SECONDS);
  }

  private static int toEpochSec(Date date) {
    return (int) toInstantTruncatedToSec(date).getEpochSecond();
  }

  private InnReachTransaction fetchTransactionById(UUID transactionId) {
    return transactionRepository.fetchOneById(transactionId)
      .orElseThrow(() -> new EntityNotFoundException("INN-Reach transaction is not found by id: " + transactionId));
  }

  private InventoryItemDTO fetchItemById(UUID itemId) {
    return itemService.find(itemId)
      .orElseThrow(() -> new IllegalArgumentException("Item is not found by id: " + itemId));
  }

  private void reportItemReceived(InnReachTransaction transaction) {
    callD2irCircOperation(D2IR_ITEM_RECEIVED_OPERATION, transaction, null);
  }

  private void reportCancelItemHold(InnReachTransaction transaction) {
    callD2irCircOperation(D2IR_CANCEL_ITEM_HOLD, transaction, null);
  }

  private void reportOwningSiteCancel(InnReachTransaction transaction, String localBibId, String patronName) {
    var payload = new HashMap<>();
    payload.put("localBibId", localBibId);
    payload.put("reasonCode", 7);
    payload.put("patronName", patronName);
    callD2irCircOperation(D2IR_OWNING_SITE_CANCEL, transaction, payload);
  }

  private void reportBorrowerRenew(InnReachTransaction transaction, Integer loanIntegerDueDate) {
    var payload = new HashMap<>();
    payload.put("dueDateTime", loanIntegerDueDate);
    callD2irCircOperation(D2IR_BORROWER_RENEW, transaction, payload);
  }

  private void reportFinalCheckIn(InnReachTransaction transaction) {
    callD2irCircOperation(D2IR_FINAL_CHECK_IN, transaction, null);
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

  private void reportTransferRequest(InnReachTransaction transaction, String hrid) {
    var payload = new HashMap<>();
    payload.put("newItemId", hrid);
    callD2irCircOperation(D2IR_TRASFER_REQUEST, transaction, payload);
  }

  private void reportReturnUncirculated(InnReachTransaction transaction) {
    callD2irCircOperation(D2IR_RETURN_UNCIRCULATED, transaction, null);
  }

  private void reportClaimsReturned(InnReachTransaction transaction, Integer claimsReturnedDateSec) {
    var payload = new HashMap<>();
    payload.put("claimsReturnedDateSec", claimsReturnedDateSec);
    callD2irCircOperation(D2IR_CLAIMS_RETURNED, transaction, payload);
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
