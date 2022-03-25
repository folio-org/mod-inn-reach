package org.folio.innreach.domain.service.impl;

import static java.lang.String.format;
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
import static org.folio.innreach.domain.entity.InnReachTransaction.TransactionState.LOCAL_CHECKOUT;
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

import java.time.Instant;
import java.util.EnumSet;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.Assert;

import org.folio.innreach.client.InstanceStorageClient;
import org.folio.innreach.domain.dto.folio.circulation.RequestDTO;
import org.folio.innreach.domain.dto.folio.inventory.InventoryItemDTO;
import org.folio.innreach.domain.dto.folio.inventory.InventoryItemStatus;
import org.folio.innreach.domain.entity.InnReachTransaction;
import org.folio.innreach.domain.entity.TransactionItemHold;
import org.folio.innreach.domain.entity.TransactionPatronHold;
import org.folio.innreach.domain.exception.EntityNotFoundException;
import org.folio.innreach.domain.service.InnReachTransactionActionService;
import org.folio.innreach.domain.service.ItemService;
import org.folio.innreach.domain.service.LoanService;
import org.folio.innreach.domain.service.PatronHoldService;
import org.folio.innreach.domain.service.RequestService;
import org.folio.innreach.dto.CancelPatronHoldDTO;
import org.folio.innreach.dto.CheckInDTO;
import org.folio.innreach.dto.InnReachTransactionDTO;
import org.folio.innreach.dto.LoanStatus;
import org.folio.innreach.dto.PatronHoldCheckInResponseDTO;
import org.folio.innreach.dto.StorageLoanDTO;
import org.folio.innreach.dto.TransactionCheckOutResponseDTO;
import org.folio.innreach.mapper.InnReachTransactionMapper;
import org.folio.innreach.repository.InnReachTransactionRepository;
import org.folio.innreach.util.DateHelper;

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
  private final TransactionTemplate transactionTemplate;

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
    var transaction = fetchTransactionById(transactionId);
    var hold = transaction.getHold();
    var folioItemId = hold.getFolioItemId();

    verifyState(transaction, ITEM_RECEIVED, RECEIVE_UNANNOUNCED);
    Assert.isTrue(hold.getFolioItemId() != null, "folioItemId is not set");

    var loan = loanService.findByItemId(folioItemId)
      .orElse(loanService.checkOutItem(transaction, servicePointId));

    hold.setFolioLoanId(loan.getId());
    hold.setDueDateTime(toEpochSec(loan.getDueDate()));

    return new TransactionCheckOutResponseDTO()
      .folioCheckOut(loan)
      .transaction(transactionMapper.toDTO(transaction));
  }

  @Override
  public void associateNewLoanWithTransaction(StorageLoanDTO loan) {
    var itemId = loan.getItemId();
    var patronId = loan.getUserId();

    var transaction = transactionRepository.fetchActiveByFolioItemIdAndPatronId(itemId, patronId).orElse(null);
    if (transaction == null) {
      return;
    }

    if (transaction.getType() == PATRON) {
      associateNewLoanWithPatronTransaction(loan, transaction);
    } else if (transaction.getType() == LOCAL) {
      associateNewLoanWithLocalTransaction(loan, transaction);
    }

  }

  @Override
  public void handleLoanUpdate(StorageLoanDTO loan) {
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
      var requestStatus = request.getStatus();

      if (requestStatus == CLOSED_PICKUP_EXPIRED || requestStatus == CLOSED_CANCELLED) {
        log.info("Updating transaction {} on the hold shelf clearance for uncirculated items, check-in {}",
          transaction.getId(), checkIn.getId());

        transaction.setState(RETURN_UNCIRCULATED);

        notifier.reportReturnUncirculated(transaction);
      }
    }
  }

  @Override
  public InnReachTransactionDTO cancelPatronHold(UUID transactionId, CancelPatronHoldDTO cancelRequest) {
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

  private void associateNewLoanWithPatronTransaction(StorageLoanDTO loan, InnReachTransaction transaction) {
    log.info("Associating a new loan {} with patron transaction {}", loan.getId(), transaction.getId());
    var hold = transaction.getHold();
    hold.setFolioLoanId(loan.getId());
    hold.setDueDateTime(toEpochSec(loan.getDueDate()));
  }

  private void associateNewLoanWithLocalTransaction(StorageLoanDTO loan, InnReachTransaction transaction) {
    log.info("Associating a new loan {} with local transaction {}", loan.getId(), transaction.getId());
    var hold = transaction.getHold();
    hold.setFolioLoanId(loan.getId());
    transaction.setState(LOCAL_CHECKOUT);
    var inventoryItemDTO = fetchItemById(loan.getItemId());
    notifier.reportCheckOut(transaction, inventoryItemDTO.getHrid(), inventoryItemDTO.getBarcode());
  }

  private void updateTransactionOnLoanClaimedReturned(StorageLoanDTO loan, InnReachTransaction transaction) {
    if (transaction.getType() != PATRON) {
      return;
    }

    log.info("Updating patron transaction {} on the claimed returned loan {}", transaction.getId(), loan.getId());

    transaction.setState(CLAIMS_RETURNED);

    var claimedReturnedDateSec = ofNullable(loan.getClaimedReturnedDate()).map(DateHelper::toEpochSec).orElse(-1);

    notifier.reportClaimsReturned(transaction, claimedReturnedDateSec);
    clearPatronAndItemInfo(transaction);
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
    var hold = transaction.getHold();
    if (transaction.getType() == ITEM) {
      log.info("Updating item transaction {} on loan closure {}", transaction.getId(), loan.getId());

      hold.setDueDateTime(null);

      clearCentralPatronInfo(transaction);

      transaction.setState(FINAL_CHECKIN);

      notifier.reportFinalCheckIn(transaction);
    } else if (transaction.getType() == PATRON) {
      log.info("Updating patron transaction {} on loan closure {}", transaction.getId(), loan.getId());

      hold.setDueDateTime(null);

      transaction.setState(ITEM_IN_TRANSIT);

      notifier.reportItemInTransit(transaction);
    }
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

  private void updateItemTransactionOnRequestChange(RequestDTO requestDTO, InnReachTransaction transaction) {
    var requestId = requestDTO.getId();
    var itemId = requestDTO.getItemId();
    var hold = transaction.getHold();
    if (requestDTO.getStatus() == CLOSED_CANCELLED) {
      log.info("Updating item hold transaction {} on cancellation of a request {}", transaction.getId(), requestDTO.getId());

      transaction.setState(CANCEL_REQUEST);

      clearCentralPatronInfo(transaction);

      var instance = instanceStorageClient.getInstanceById(requestDTO.getInstanceId());
      notifier.reportOwningSiteCancel(transaction, instance.getHrid(), hold.getPatronName());
      clearCentralPatronInfo(transaction);
    } else if (!itemId.equals(hold.getFolioItemId())) {
      log.info("Updating transaction {} on moving a request {} from one item to another", transaction.getId(), requestId);

      var item = fetchItemById(requestDTO.getItemId());

      hold.setFolioItemId(itemId);
      hold.setFolioInstanceId(requestDTO.getInstanceId());
      hold.setFolioHoldingId(requestDTO.getHoldingsRecordId());
      hold.setFolioItemBarcode(item.getBarcode());
      transaction.setState(TRANSFER);

      notifier.reportTransferRequest(transaction, item.getHrid());
    }
  }

  private void updatePatronTransactionOnRequestChange(RequestDTO requestDTO, InnReachTransaction transaction) {
    if (requestDTO.getStatus() == CLOSED_CANCELLED &&
      (transaction.getState() == PATRON_HOLD || transaction.getState() == TRANSFER)) {
      log.info("Updating patron hold transaction {} on cancellation of a request {}", transaction.getId(), requestDTO.getId());
      transaction.setState(BORROWING_SITE_CANCEL);
      notifier.reportCancelItemHold(transaction);
    }
  }

  private void cancelPatronHoldWithOpenRequest(CancelPatronHoldDTO cancelRequest,
                                               InnReachTransaction transaction) {
    if (transaction.getState() != ITEM_SHIPPED) {
      transaction.setState(BORROWING_SITE_CANCEL);

      transaction = saveInNewDbTransaction(transaction);

      requestService.cancelRequest(transaction, cancelRequest.getCancellationReasonId(),
        cancelRequest.getCancellationAdditionalInformation());

      notifier.reportCancelItemHold(transaction);
    } else {
      requestService.cancelRequest(transaction, cancelRequest.getCancellationReasonId(),
        cancelRequest.getCancellationAdditionalInformation());
    }
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

  private void verifyState(InnReachTransaction transaction, InnReachTransaction.TransactionState... states) {
    var state = transaction.getState();
    Assert.isTrue(ArrayUtils.contains(states, state), "Unexpected transaction state: " + state);
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

  private InnReachTransaction saveInNewDbTransaction(InnReachTransaction transaction) {
    return transactionTemplate.execute(status -> transactionRepository.save(transaction));
  }

}
