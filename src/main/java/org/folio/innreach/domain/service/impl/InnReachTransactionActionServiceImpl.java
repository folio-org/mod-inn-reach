package org.folio.innreach.domain.service.impl;

import static java.lang.String.format;
import static java.util.Optional.ofNullable;

import static org.folio.innreach.domain.dto.folio.circulation.RequestDTO.RequestStatus.CLOSED_CANCELLED;
import static org.folio.innreach.domain.entity.InnReachTransaction.TransactionState.BORROWER_RENEW;
import static org.folio.innreach.domain.entity.InnReachTransaction.TransactionState.BORROWING_SITE_CANCEL;
import static org.folio.innreach.domain.entity.InnReachTransaction.TransactionState.CANCEL_REQUEST;
import static org.folio.innreach.domain.entity.InnReachTransaction.TransactionState.CLAIMS_RETURNED;
import static org.folio.innreach.domain.entity.InnReachTransaction.TransactionState.FINAL_CHECKIN;
import static org.folio.innreach.domain.entity.InnReachTransaction.TransactionState.ITEM_HOLD;
import static org.folio.innreach.domain.entity.InnReachTransaction.TransactionState.ITEM_IN_TRANSIT;
import static org.folio.innreach.domain.entity.InnReachTransaction.TransactionState.ITEM_RECEIVED;
import static org.folio.innreach.domain.entity.InnReachTransaction.TransactionState.ITEM_SHIPPED;
import static org.folio.innreach.domain.entity.InnReachTransaction.TransactionState.LOCAL_CHECKOUT;
import static org.folio.innreach.domain.entity.InnReachTransaction.TransactionState.LOCAL_HOLD;
import static org.folio.innreach.domain.entity.InnReachTransaction.TransactionState.PATRON_HOLD;
import static org.folio.innreach.domain.entity.InnReachTransaction.TransactionState.RECALL;
import static org.folio.innreach.domain.entity.InnReachTransaction.TransactionState.RECEIVE_UNANNOUNCED;
import static org.folio.innreach.domain.entity.InnReachTransaction.TransactionState.RETURN_UNCIRCULATED;
import static org.folio.innreach.domain.entity.InnReachTransaction.TransactionState.TRANSFER;
import static org.folio.innreach.domain.entity.InnReachTransaction.TransactionType.ITEM;
import static org.folio.innreach.domain.entity.InnReachTransaction.TransactionType.LOCAL;
import static org.folio.innreach.domain.entity.InnReachTransaction.TransactionType.PATRON;
import static org.folio.innreach.dto.ItemStatus.NameEnum.AWAITING_PICKUP;
import static org.folio.innreach.util.DateHelper.toEpochSec;
import static org.folio.innreach.util.DateHelper.toInstantTruncatedToSec;
import static org.folio.innreach.util.InnReachTransactionUtils.clearCentralPatronInfo;
import static org.folio.innreach.util.InnReachTransactionUtils.clearPatronAndItemInfo;
import static org.folio.innreach.util.InnReachTransactionUtils.verifyState;

import java.time.Instant;
import java.util.Date;
import java.util.EnumSet;
import java.util.Optional;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import org.folio.innreach.client.CirculationClient;
import org.folio.innreach.client.InstanceStorageClient;
import org.folio.innreach.domain.dto.folio.circulation.RequestDTO;
import org.folio.innreach.domain.dto.folio.inventory.InventoryItemDTO;
import org.folio.innreach.domain.dto.folio.inventory.InventoryItemStatus;
import org.folio.innreach.domain.entity.InnReachTransaction;
import org.folio.innreach.domain.entity.TransactionItemHold;
import org.folio.innreach.domain.entity.TransactionPatronHold;
import org.folio.innreach.domain.event.CancelRequestEvent;
import org.folio.innreach.domain.event.MoveRequestEvent;
import org.folio.innreach.domain.exception.EntityNotFoundException;
import org.folio.innreach.domain.service.InnReachTransactionActionService;
import org.folio.innreach.domain.service.ItemService;
import org.folio.innreach.domain.service.LoanService;
import org.folio.innreach.domain.service.PatronHoldService;
import org.folio.innreach.domain.service.RequestService;
import org.folio.innreach.dto.CancelTransactionHoldDTO;
import org.folio.innreach.dto.CheckInDTO;
import org.folio.innreach.dto.InnReachTransactionDTO;
import org.folio.innreach.dto.LoanStatus;
import org.folio.innreach.dto.PatronHoldCheckInResponseDTO;
import org.folio.innreach.dto.StorageLoanDTO;
import org.folio.innreach.dto.TransactionCheckOutResponseDTO;
import org.folio.innreach.mapper.InnReachTransactionMapper;
import org.folio.innreach.repository.InnReachTransactionRepository;
import org.folio.innreach.util.DateHelper;
import org.folio.innreach.util.InnReachTransactionUtils;

@Log4j2
@Transactional
@RequiredArgsConstructor
@Service
public class InnReachTransactionActionServiceImpl implements InnReachTransactionActionService {

  private final InnReachTransactionRepository transactionRepository;
  private final InnReachTransactionMapper transactionMapper;
  private final RequestService requestService;
  private final LoanService loanService;
  private final PatronHoldService patronHoldService;
  private final ItemService itemService;
  private final InstanceStorageClient instanceStorageClient;
  private final InnReachTransactionActionNotifier notifier;
  private final ApplicationEventPublisher eventPublisher;
  private final CirculationClient circulationClient;

  @Override
  public PatronHoldCheckInResponseDTO checkInPatronHoldItem(UUID transactionId, UUID servicePointId) {
    var transaction = fetchTransactionById(transactionId);

    verifyState(transaction, ITEM_SHIPPED);

    var response = checkInItem(transaction, servicePointId);

    transaction.setState(ITEM_RECEIVED);

    notifier.reportItemReceived(transaction);

    handleItemWithCanceledRequest(transaction);

    return response;
  }

  @Override
  public PatronHoldCheckInResponseDTO checkInPatronHoldUnshippedItem(UUID transactionId, UUID servicePointId, String itemBarcode) {
    var transaction = fetchTransactionById(transactionId);
    var folioItemBarcode = transaction.getHold().getFolioItemBarcode();

    verifyState(transaction, PATRON_HOLD, TRANSFER);
    Assert.isTrue(folioItemBarcode == null, "Item associated with the transaction has a barcode assigned");

    patronHoldService.addItemBarcode(transaction, itemBarcode);

    var response = checkInItem(transaction, servicePointId);

    transaction.setState(RECEIVE_UNANNOUNCED);

    notifier.reportUnshippedItemReceived(transaction);

    handleItemWithCanceledRequest(transaction);

    return response;
  }

  @Override
  public TransactionCheckOutResponseDTO checkOutItemHoldItem(String itemBarcode, UUID servicePointId) {
    var transaction = transactionRepository.fetchOneByFolioItemBarcodeAndStates(itemBarcode,
        EnumSet.of(ITEM_HOLD, TRANSFER))
      .orElseThrow(() -> new EntityNotFoundException("INN-Reach transaction is not found by itemBarcode: " + itemBarcode));

    var hold = (TransactionItemHold) transaction.getHold();
    var folioPatronBarcode = hold.getFolioPatronBarcode();

    Assert.isTrue(folioPatronBarcode != null, "folioPatronBarcode is not set");

    var checkOutResponse = loanService.checkOutItem(transaction, servicePointId);
    var callNumber = checkOutResponse.getItem().getCallNumber();

    hold.setFolioLoanId(checkOutResponse.getId());

    transaction.setState(ITEM_SHIPPED);

    notifier.reportItemShipped(transaction, itemBarcode, callNumber);

    return new TransactionCheckOutResponseDTO()
      .transaction(transactionMapper.toDTO(transaction))
      .folioCheckOut(checkOutResponse);
  }

  @Override
  public TransactionCheckOutResponseDTO checkOutPatronHoldItem(UUID transactionId, UUID servicePointId) {
    var transaction = fetchTransactionOfType(transactionId, PATRON);

    verifyState(transaction, ITEM_RECEIVED, RECEIVE_UNANNOUNCED);

    return checkOutItem(transaction, servicePointId);
  }

  @Override
  public TransactionCheckOutResponseDTO checkOutLocalHoldItem(UUID transactionId, UUID servicePointId) {
    var transaction = fetchTransactionOfType(transactionId, LOCAL);

    verifyState(transaction, LOCAL_HOLD);

    return checkOutItem(transaction, servicePointId);
  }

  @Override
  public void associateNewLoanWithTransaction(StorageLoanDTO loan) {
    var itemId = loan.getItemId();
    var patronId = loan.getUserId();

    transactionRepository.fetchActiveByFolioItemIdAndPatronId(itemId, patronId)
      .ifPresent(transaction -> associateLoanWithTransaction(loan.getId(), loan.getDueDate(), itemId, transaction));
  }

  @Override
  public void handleLoanUpdate(StorageLoanDTO loan) {
    var transaction = transactionRepository.fetchActiveByLoanId(loan.getId()).orElse(null);
    if (transaction == null) {
      return;
    }

    var loanAction = loan.getAction();
    var loanStatus = ofNullable(loan.getStatus()).map(LoanStatus::getName).orElse(null);

    if (checkLoanActionAndStatus(loanAction, loanStatus)) {
      updateTransactionOnLoanClosure(loan, transaction);
    } else if ("renewed".equalsIgnoreCase(loanAction)) {
      updateTransactionOnLoanRenewal(loan, transaction);
    } else if ("claimedReturned".equalsIgnoreCase(loanAction)) {
      updateTransactionOnLoanClaimedReturned(loan, transaction);
    } else if ("recallrequested".equalsIgnoreCase(loanAction)) {
      updateTransactionOnLoanRecallRequested(loan, transaction);
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

      if (requestService.isCanceledOrExpired(request)) {
        log.info("Updating transaction {} on the hold shelf clearance for uncirculated items, check-in {}",
          transaction.getId(), checkIn.getId());

        transaction.setState(RETURN_UNCIRCULATED);

        notifier.reportReturnUncirculated(transaction);
      }
    }
  }

  @Override
  public InnReachTransactionDTO cancelPatronHold(UUID transactionId, CancelTransactionHoldDTO cancelRequest) {
    var transaction = fetchTransactionOfType(transactionId, PATRON);

    var requestId = transaction.getHold().getFolioRequestId();
    var request = requestService.findRequest(requestId);

    if (requestService.isOpenRequest(request)) {
      cancelPatronHoldWithOpenRequest(cancelRequest, transaction);
    } else if (request.getStatus() == CLOSED_CANCELLED) {
      cancelPatronHoldWithClosedRequest(transaction);
    }

    return transactionMapper.toDTO(transaction);
  }

  @Override
  public void cancelItemHold(UUID transactionId, CancelTransactionHoldDTO cancelRequest) {
    var transaction = fetchTransactionOfType(transactionId, ITEM);

    verifyState(transaction, ITEM_HOLD);

    eventPublisher.publishEvent(CancelRequestEvent.of(transaction,
      cancelRequest.getCancellationReasonId(),
      cancelRequest.getCancellationAdditionalInformation()));
  }

  @Override
  public void returnPatronHoldItem(UUID transactionId, UUID servicePointId) {
    var transaction = fetchTransactionOfType(transactionId, PATRON);

    verifyState(transaction, ITEM_RECEIVED, RECEIVE_UNANNOUNCED);

    log.info("Attempting to return item of Patron Hold transaction {}", transactionId);

    var loanId = transaction.getHold().getFolioLoanId();
    var requestId = transaction.getHold().getFolioRequestId();
    if (loanId != null) {
      var loan = loanService.getById(loanId);
      if (loanService.isOpen(loan)) {
        log.info("Checking-in item for transaction associated with an open loan");
        loanService.checkInItem(transaction, servicePointId);
      } else {
        updatePatronTransactionOnLoanClosure(transaction, loanId);
      }
    } else {
      var request = requestService.findRequest(requestId);
      if (requestService.isCanceledOrExpired(request)) {
        log.info("Checking-in item for transaction associated with a closed loan and canceled request");
        loanService.checkInItem(transaction, servicePointId);
      }
    }
  }

  @Override
  public void transferItemHold(UUID transactionId, String itemBarcode) {
    var transaction = fetchTransactionOfType(transactionId, ITEM);

    verifyState(transaction, ITEM_HOLD);

    var item = fetchItemByBarcode(itemBarcode);

    requestService.validateItemAvailability(item);

    eventPublisher.publishEvent(MoveRequestEvent.of(transaction, item));
  }

  @Override
  public void finalCheckinItemHold(UUID transactionId, UUID servicePointId) {
    var transaction = fetchTransactionById(transactionId);

    verifyState(transaction, ITEM_RECEIVED, RECEIVE_UNANNOUNCED);

    var loan = loanService.getById(transaction.getHold().getFolioLoanId());
    var loanAction = loan.getAction();
    var loanStatus = ofNullable(loan.getStatus()).map(LoanStatus::getName).orElse(null);

    if (checkLoanActionAndStatus(loanAction, loanStatus)) {
      updateItemTransactionOnLoanClosure(transaction, loan.getId());
    } else {
      loanService.checkInItem(transaction, servicePointId);
    }
  }

  private void associateLoanWithTransaction(UUID loanId, Date loanDueDate, UUID itemId, InnReachTransaction transaction) {
    if (transaction.getType() == PATRON) {
      log.info("Associating a new loan {} with patron transaction {}", loanId, transaction.getId());
      var hold = transaction.getHold();

      hold.setFolioLoanId(loanId);
      hold.setDueDateTime(toEpochSec(loanDueDate));
    } else if (transaction.getType() == LOCAL) {
      log.info("Associating a new loan {} with local transaction {}", loanId, transaction.getId());

      var hold = transaction.getHold();
      hold.setFolioLoanId(loanId);

      transaction.setState(LOCAL_CHECKOUT);

      var inventoryItemDTO = fetchItemById(itemId);
      notifier.reportCheckOut(transaction, inventoryItemDTO.getHrid(), inventoryItemDTO.getBarcode());

      clearPatronAndItemInfo(hold);
    }
  }

  private TransactionCheckOutResponseDTO checkOutItem(InnReachTransaction transaction, UUID servicePointId) {
    var hold = transaction.getHold();
    var folioItemId = hold.getFolioItemId();

    Assert.isTrue(hold.getFolioItemId() != null, "folioItemId is not set");

    var loan = loanService.findByItemId(folioItemId).orElse(null);
    if (loan != null) {
      associateLoanWithTransaction(loan.getId(), loan.getDueDate(), folioItemId, transaction);
    } else {
      loan = loanService.checkOutItem(transaction, servicePointId);
    }

    return new TransactionCheckOutResponseDTO()
      .folioCheckOut(loan)
      .transaction(transactionMapper.toDTO(transaction));
  }

  private void updateTransactionOnLoanClaimedReturned(StorageLoanDTO loan, InnReachTransaction transaction) {
    if (transaction.getType() != PATRON) {
      return;
    }

    log.info("Updating patron transaction {} on the claimed returned loan {}", transaction.getId(), loan.getId());

    transaction.setState(CLAIMS_RETURNED);

    var claimedReturnedDateSec = ofNullable(loan.getClaimedReturnedDate()).map(DateHelper::toEpochSec).orElse(-1);

    notifier.reportClaimsReturned(transaction, claimedReturnedDateSec);

    clearPatronAndItemInfo(transaction.getHold());
  }

  private void updateTransactionOnLoanRenewal(StorageLoanDTO loan, InnReachTransaction transaction) {
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

      notifier.reportBorrowerRenew(transaction, loanIntegerDueDate);
    }
  }

  private void updateTransactionOnLoanClosure(StorageLoanDTO loan, InnReachTransaction transaction) {
    if (transaction.getType() == ITEM) {
      updateItemTransactionOnLoanClosure(transaction, loan.getId());
    } else if (transaction.getType() == PATRON) {
      updatePatronTransactionOnLoanClosure(transaction, loan.getId());
    }
  }

  private void updateItemTransactionOnLoanClosure(InnReachTransaction transaction, UUID loanId) {
    log.info("Updating item transaction {} on loan closure {}", transaction.getId(), loanId);

    transaction.getHold().setDueDateTime(null);

    clearCentralPatronInfo(transaction.getHold());

    transaction.setState(FINAL_CHECKIN);

    notifier.reportFinalCheckIn(transaction);
  }

  private void updatePatronTransactionOnLoanClosure(InnReachTransaction transaction, UUID loanId) {
    log.info("Updating patron transaction {} on loan closure {}", transaction.getId(), loanId);

    transaction.getHold().setDueDateTime(null);

    transaction.setState(ITEM_IN_TRANSIT);

    notifier.reportItemInTransit(transaction);
  }

  private void updateTransactionOnLoanRecallRequested(StorageLoanDTO loan, InnReachTransaction transaction) {
    if (transaction.getType() != ITEM) {
      return;
    }

    log.info("Updating item transaction {} on recall requested within the loan {}", transaction.getId(), loan.getId());

    transaction.setState(RECALL);

    var loanDueDate = toInstantTruncatedToSec(loan.getDueDate());
    notifier.reportRecallRequested(transaction, loanDueDate);
  }

  private void handleItemWithCanceledRequest(InnReachTransaction transaction) {
    var request = requestService.findRequest(transaction.getHold().getFolioRequestId());
    if (request.getStatus() == CLOSED_CANCELLED) {
      log.info("Updating transaction {} to uncirculated state due to cancelled request {}",
        transaction.getId(), request.getId());

      notifier.reportReturnUncirculated(transaction);

      transaction.setState(RETURN_UNCIRCULATED);
    }
  }

  private PatronHoldCheckInResponseDTO checkInItem(InnReachTransaction transaction, UUID servicePointId) {
    var hold = (TransactionPatronHold) transaction.getHold();
    var shippedItemBarcode = hold.getShippedItemBarcode();
    var folioItemBarcode = hold.getFolioItemBarcode();

    Assert.isTrue(shippedItemBarcode != null, "shippedItemBarcode is not set");
    Assert.isTrue(folioItemBarcode != null, "folioItemBarcode is not set");

    var checkInResponse = loanService.checkInItem(transaction, servicePointId);

    return new PatronHoldCheckInResponseDTO()
      .transaction(transactionMapper.toDTO(transaction))
      .folioCheckIn(checkInResponse)
      .barcodeAugmented(!shippedItemBarcode.equals(folioItemBarcode));
  }

  private void updateItemTransactionOnRequestChange(RequestDTO request, InnReachTransaction transaction) {
    var requestId = request.getId();
    var itemId = request.getItemId();
    var hold = transaction.getHold();
    if (request.getStatus() == CLOSED_CANCELLED) {
      log.info("Updating item hold transaction {} on cancellation of a request {}", transaction.getId(), request.getId());

      transaction.setState(CANCEL_REQUEST);

      var instance = instanceStorageClient.getInstanceById(request.getInstanceId());
      notifier.reportOwningSiteCancel(transaction, instance.getHrid(), hold.getPatronName());

      clearCentralPatronInfo(transaction.getHold());
    } else {
      var transactionItemId = hold.getFolioItemId();
      if (!itemId.equals(transactionItemId)) {
        log.info("Updating item hold transaction {} on moving a request {} from item {} to {}",
          transaction.getId(), requestId, transactionItemId, itemId);

        var item = fetchItemById(request.getItemId());

        hold.setFolioItemId(itemId);
        hold.setFolioInstanceId(request.getInstanceId());
        hold.setFolioHoldingId(request.getHoldingsRecordId());
        hold.setFolioItemBarcode(item.getBarcode());
        transaction.setState(TRANSFER);

        notifier.reportTransferRequest(transaction, item.getHrid());
      }
    }
  }

  private void updatePatronTransactionOnRequestChange(RequestDTO requestDTO, InnReachTransaction transaction) {
    if (requestDTO.getStatus() == CLOSED_CANCELLED &&
      (transaction.getState() == PATRON_HOLD || transaction.getState() == TRANSFER)) {
      log.info("Updating patron hold transaction {} on cancellation of a request {}", transaction.getId(), requestDTO.getId());
      transaction.setState(BORROWING_SITE_CANCEL);
      notifier.reportCancelItemHold(transaction);
      clearPatronTransactionAndItemRecord(requestDTO.getItemId(), transaction);
    }
  }

  private void cancelPatronHoldWithOpenRequest(CancelTransactionHoldDTO cancelRequest,
                                               InnReachTransaction transaction) {
    var trackingId = transaction.getTrackingId();
    var requestId = transaction.getHold().getFolioRequestId();
    if (transaction.getState() != ITEM_SHIPPED) {
      transaction.setState(BORROWING_SITE_CANCEL);
      notifier.reportCancelItemHold(transaction);
    }

    eventPublisher.publishEvent(new CancelRequestEvent(trackingId, requestId,
      cancelRequest.getCancellationReasonId(),
      cancelRequest.getCancellationAdditionalInformation()));
  }

  private void cancelPatronHoldWithClosedRequest(InnReachTransaction transaction) {
    if (transaction.getState() == PATRON_HOLD || transaction.getState() == TRANSFER) {
      transaction.setState(BORROWING_SITE_CANCEL);

      notifier.reportCancelItemHold(transaction);
    } else if (EnumSet.of(ITEM_SHIPPED, RECEIVE_UNANNOUNCED, ITEM_RECEIVED).contains(transaction.getState())) {
      var item = fetchItemById(transaction.getHold().getFolioItemId());

      if (item.getStatus() != InventoryItemStatus.AWAITING_PICKUP) {
        transaction.setState(RETURN_UNCIRCULATED);

        notifier.reportReturnUncirculated(transaction);
      }
    }
  }

  private void clearPatronTransactionAndItemRecord(UUID itemId, InnReachTransaction transaction) {
    var patronTransaction = (TransactionPatronHold) transaction.getHold();
    InnReachTransactionUtils.clearPatronAndItemInfo(patronTransaction);
    updateItem(itemId);
  }

  private Optional<InventoryItemDTO> updateItem(UUID itemId) {
    return itemService.changeAndUpdate(itemId, item -> {
      item.setBarcode(null);
      return item;
    });
  }

  private InnReachTransaction fetchTransactionById(UUID transactionId) {
    return transactionRepository.fetchOneById(transactionId)
      .orElseThrow(() -> new EntityNotFoundException("INN-Reach transaction is not found by id: " + transactionId));
  }

  private InnReachTransaction fetchTransactionOfType(UUID transactionId, InnReachTransaction.TransactionType type) {
    InnReachTransaction transaction = fetchTransactionById(transactionId);

    if (transaction.getType() != type) {
      throw new IllegalArgumentException(format("InnReach transaction with transaction id [%s] " +
        "is not of [%s] type", transactionId, type));
    }

    return transaction;
  }

  private InventoryItemDTO fetchItemById(UUID itemId) {
    return itemService.find(itemId)
      .orElseThrow(() -> new IllegalArgumentException("Item is not found by id: " + itemId));
  }

  private InventoryItemDTO fetchItemByBarcode(String itemBarcode) {
    return itemService.findItemByBarcode(itemBarcode)
      .orElseThrow(() -> new IllegalArgumentException("Item is not found by barcode: " + itemBarcode));
  }

  private boolean checkLoanActionAndStatus(String loanAction, String loanStatus) {
    return "checkedin".equalsIgnoreCase(loanAction) && "closed".equalsIgnoreCase(loanStatus);
  }
}
