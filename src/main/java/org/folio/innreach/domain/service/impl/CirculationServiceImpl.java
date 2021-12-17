package org.folio.innreach.domain.service.impl;

import static java.util.Objects.nonNull;
import static org.apache.commons.lang3.StringUtils.capitalize;

import static org.folio.innreach.domain.dto.folio.circulation.RequestDTO.RequestStatus.OPEN_AWAITING_PICKUP;
import static org.folio.innreach.domain.dto.folio.circulation.RequestDTO.RequestStatus.OPEN_IN_TRANSIT;
import static org.folio.innreach.domain.entity.InnReachTransaction.TransactionState.BORROWER_RENEW;
import static org.folio.innreach.domain.entity.InnReachTransaction.TransactionState.BORROWING_SITE_CANCEL;
import static org.folio.innreach.domain.entity.InnReachTransaction.TransactionState.CANCEL_REQUEST;
import static org.folio.innreach.domain.entity.InnReachTransaction.TransactionState.FINAL_CHECKIN;
import static org.folio.innreach.domain.entity.InnReachTransaction.TransactionState.ITEM_HOLD;
import static org.folio.innreach.domain.entity.InnReachTransaction.TransactionState.ITEM_IN_TRANSIT;
import static org.folio.innreach.domain.entity.InnReachTransaction.TransactionState.ITEM_RECEIVED;
import static org.folio.innreach.domain.entity.InnReachTransaction.TransactionState.ITEM_SHIPPED;
import static org.folio.innreach.domain.entity.InnReachTransaction.TransactionState.LOCAL_HOLD;
import static org.folio.innreach.domain.entity.InnReachTransaction.TransactionState.PATRON_HOLD;
import static org.folio.innreach.domain.entity.InnReachTransaction.TransactionState.RECALL;
import static org.folio.innreach.domain.entity.InnReachTransaction.TransactionState.RECEIVE_UNANNOUNCED;
import static org.folio.innreach.domain.entity.InnReachTransaction.TransactionState.RETURN_UNCIRCULATED;
import static org.folio.innreach.domain.entity.InnReachTransaction.TransactionState.TRANSFER;

import java.util.Date;
import java.util.HashMap;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import org.folio.innreach.domain.dto.folio.inventory.InventoryItemDTO;
import org.folio.innreach.domain.entity.CentralServer;
import org.folio.innreach.domain.entity.InnReachRecallUser;
import org.folio.innreach.domain.entity.InnReachTransaction;
import org.folio.innreach.domain.entity.InnReachTransaction.TransactionState;
import org.folio.innreach.domain.entity.InnReachTransaction.TransactionType;
import org.folio.innreach.domain.entity.TransactionHold;
import org.folio.innreach.domain.entity.TransactionPatronHold;
import org.folio.innreach.domain.exception.CirculationException;
import org.folio.innreach.domain.exception.EntityNotFoundException;
import org.folio.innreach.domain.service.CirculationService;
import org.folio.innreach.domain.service.HoldingsService;
import org.folio.innreach.domain.service.ItemService;
import org.folio.innreach.domain.service.PatronHoldService;
import org.folio.innreach.domain.service.RequestService;
import org.folio.innreach.domain.service.UpdateTemplate.UpdateOperation;
import org.folio.innreach.dto.BaseCircRequestDTO;
import org.folio.innreach.dto.CancelRequestDTO;
import org.folio.innreach.dto.Holding;
import org.folio.innreach.dto.InnReachResponseDTO;
import org.folio.innreach.dto.ItemReceivedDTO;
import org.folio.innreach.dto.ItemShippedDTO;
import org.folio.innreach.dto.LocalHoldDTO;
import org.folio.innreach.dto.PatronHoldDTO;
import org.folio.innreach.dto.RecallDTO;
import org.folio.innreach.dto.ReturnUncirculatedDTO;
import org.folio.innreach.dto.TransactionHoldDTO;
import org.folio.innreach.dto.TransferRequestDTO;
import org.folio.innreach.dto.BorrowerRenewDTO;
import org.folio.innreach.dto.CheckOutResponseDTO;
import org.folio.innreach.dto.RenewLoanRequestDTO;
import org.folio.innreach.external.service.InnReachExternalService;
import org.folio.innreach.mapper.InnReachTransactionHoldMapper;
import org.folio.innreach.mapper.InnReachTransactionPickupLocationMapper;
import org.folio.innreach.repository.CentralServerRepository;
import org.folio.innreach.repository.InnReachTransactionRepository;
import org.folio.innreach.util.UUIDHelper;

@Log4j2
@Service
@Transactional
@RequiredArgsConstructor
public class CirculationServiceImpl implements CirculationService {

  private static final String[] TRANSACTION_HOLD_IGNORE_PROPS_ON_COPY = {
    "pickupLocation", "id", "createdBy", "updatedBy", "createdDate", "updatedDate",
    "folioPatronId", "folioInstanceId", "folioHoldingId", "folioItemId",
    "folioRequestId", "folioLoanId", "folioPatronBarcode", "folioItemBarcode"
  };
  private static final String[] PICKUP_LOC_IGNORE_PROPS_ON_COPY = {
    "id", "createdBy", "updatedBy", "createdDate", "updatedDate"
  };

  private static final String UNEXPECTED_TRANSACTION_STATE = "Unexpected transaction state: ";
  private static final String D2IR_ITEM_RECALL_OPERATION = "recall";

  private final InnReachTransactionRepository transactionRepository;
  private final CentralServerRepository centralserverRepository;
  private final InnReachTransactionHoldMapper transactionHoldMapper;
  private final InnReachTransactionPickupLocationMapper pickupLocationMapper;
  private final PatronHoldService patronHoldService;
  private final RequestService requestService;
  private final ItemService itemService;
  private final HoldingsService holdingsService;
  private final InnReachExternalService innReachExternalService;

  @Transactional(propagation = Propagation.NEVER)
  @Override
  public InnReachResponseDTO initiatePatronHold(String trackingId, String centralCode, PatronHoldDTO patronHold) {
    var optTransaction = transactionRepository.findByTrackingIdAndCentralServerCode(trackingId, centralCode);
    var transactionHold = transactionHoldMapper.mapRequest(patronHold);

    if (optTransaction.isPresent()) {
      log.info("Transaction patron hold with trackingId [{}] and centralCode [{}] exists, start to update...",
        trackingId, centralCode);
      var existingTransaction = optTransaction.get();

      updateTransactionHold(existingTransaction.getHold(), transactionHold);
      existingTransaction = transactionRepository.save(existingTransaction);

      patronHoldService.updateVirtualItems(existingTransaction);
    } else {
      log.info("Transaction patron hold with trackingId [{}] and centralCode [{}] doesn't exist, create a new one...",
        trackingId, centralCode);

      var newTransaction = createTransaction(trackingId, centralCode, transactionHold, TransactionType.PATRON);
      newTransaction = transactionRepository.save(newTransaction);

      patronHoldService.createVirtualItems(newTransaction);
    }

    return success();
  }

  @Transactional(propagation = Propagation.NEVER)
  @Override
  public InnReachResponseDTO initiateLocalHold(String trackingId, String centralCode, LocalHoldDTO localHold) {
    Assert.isTrue(StringUtils.equals(localHold.getItemAgencyCode(), localHold.getPatronAgencyCode()),
      "The patron and item agencies should be on the same local server");

    var optTransaction = transactionRepository.findByTrackingIdAndCentralServerCode(trackingId, centralCode);
    var transactionHold = transactionHoldMapper.mapRequest(localHold);

    InnReachTransaction transaction;
    if (optTransaction.isPresent()) {
      log.info("Transaction local hold with trackingId [{}] and centralCode [{}] exists, start to update...",
        trackingId, centralCode);

      transaction = optTransaction.get();

      updateTransactionHold(transaction.getHold(), transactionHold);
    } else {
      log.info("Transaction local hold with trackingId [{}] and centralCode [{}] doesn't exist, create a new one...",
        trackingId, centralCode);

      transaction = createTransaction(trackingId, centralCode, transactionHold, TransactionType.LOCAL);
    }
    transaction = transactionRepository.save(transaction);

    requestService.createLocalHoldRequest(transaction);

    return success();
  }

  @Override
  public InnReachResponseDTO trackPatronHoldShippedItem(String trackingId, String centralCode, ItemShippedDTO itemShipped) {
    var innReachTransaction = getTransaction(trackingId, centralCode);

    var itemBarcode = itemShipped.getItemBarcode();
    var folioItemBarcode = itemBarcode;
    var callNumber = itemShipped.getCallNumber();

    var transactionPatronHold = (TransactionPatronHold) innReachTransaction.getHold();

    if (nonNull(itemBarcode)) {
      var itemByBarcode = itemService.findItemByBarcode(itemBarcode);

      if (itemByBarcode.isPresent()) {
        folioItemBarcode += transactionPatronHold.getItemAgencyCode();
      }

      transactionPatronHold.setShippedItemBarcode(itemBarcode);
      transactionPatronHold.setFolioItemBarcode(folioItemBarcode);
    }

    if (nonNull(callNumber)) {
      transactionPatronHold.setCallNumber(callNumber);
    }

    UUID folioItemId = transactionPatronHold.getFolioItemId();

    itemService.changeAndUpdate(folioItemId,
        () -> new IllegalArgumentException("Item with id = " + folioItemId + " not found!"),
        changeFolioAssociatedItem(folioItemBarcode, callNumber));

    innReachTransaction.setState(ITEM_SHIPPED);

    return success();
  }

  @Override
  public InnReachResponseDTO cancelPatronHold(String trackingId, String centralCode, CancelRequestDTO cancelRequest) {
    log.info("Cancelling request for transaction: {}", trackingId);

    var transaction = getTransaction(trackingId, centralCode);

    transaction.setState(CANCEL_REQUEST);

    var itemId = transaction.getHold().getFolioItemId();

    requestService.cancelRequest(transaction, cancelRequest.getReason());

    removeItemTransactionInfo(itemId)
        .ifPresent(this::removeHoldingsTransactionInfo);

    log.info("Item request successfully cancelled");

    return success();
  }

  @Override
  public InnReachResponseDTO transferPatronHoldItem(String trackingId, String centralCode, TransferRequestDTO request) {
    var transaction = getTransaction(trackingId, centralCode);

    validateEquals(request::getItemId, () -> transaction.getHold().getItemId(), "item id");
    validateEquals(request::getItemAgencyCode, () -> transaction.getHold().getItemAgencyCode(), "item agency code");

    transaction.getHold().setItemId(request.getNewItemId());
    transaction.setState(TRANSFER);

    return success();
  }

  @Override
  public InnReachResponseDTO cancelItemHold(String trackingId, String centralCode, BaseCircRequestDTO cancelItemDTO) {
    var transaction = getTransaction(trackingId, centralCode);

    if (transaction.getHold().getFolioLoanId() != null) {
      throw new IllegalArgumentException("Requested item is already checked out.");
    }
    requestService.cancelRequest(transaction, "Request cancelled at borrowing site");
    transaction.setState(BORROWING_SITE_CANCEL);

    return success();
  }

  @Override
  public InnReachResponseDTO itemReceived(String trackingId, String centralCode, ItemReceivedDTO itemReceivedDTO) {
    var transaction = getTransaction(trackingId, centralCode);

    Assert.isTrue(transaction.getState() == ITEM_SHIPPED, unexpectedTransactionState(transaction));
    transaction.setState(ITEM_RECEIVED);

    return success();
  }

  @Override
  public InnReachResponseDTO receiveUnshipped(String trackingId, String centralCode,
                                              BaseCircRequestDTO receiveUnshippedRequest) {
    var transaction = getTransaction(trackingId, centralCode);

    if (transaction.getState() == TransactionState.ITEM_SHIPPED) {
      throw new IllegalArgumentException(unexpectedTransactionState(transaction));
    }

    if (transaction.getState() == TransactionState.ITEM_HOLD) {
      log.info("Attempting to create a loan");

      var patronId = UUIDHelper.fromStringWithoutHyphens(receiveUnshippedRequest.getPatronId());
      var servicePointId = requestService.getDefaultServicePointIdForPatron(patronId);
      var checkOutResponse = requestService.checkOutItem(transaction, servicePointId);
      var loanId = checkOutResponse.getId();

      log.info("Created a loan with id {}", loanId);

      transaction.getHold().setFolioLoanId(loanId);
      transaction.setState(RECEIVE_UNANNOUNCED);
    }

    return success();
  }

  @Override
  public InnReachResponseDTO itemInTransit(String trackingId, String centralCode, BaseCircRequestDTO itemInTransitRequest) {
    var transaction = getTransaction(trackingId, centralCode);
    var state = transaction.getState();

    Assert.isTrue(state == ITEM_RECEIVED || state == RECEIVE_UNANNOUNCED, unexpectedTransactionState(transaction));

    transaction.setState(ITEM_IN_TRANSIT);

    return success();
  }

  @Override
  public InnReachResponseDTO returnUncirculated(String trackingId, String centralCode, ReturnUncirculatedDTO returnUncirculated) {
    var transaction = getTransaction(trackingId, centralCode);
    var state = transaction.getState();

    if (state == ITEM_RECEIVED || state == RECEIVE_UNANNOUNCED) {
      transaction.setState(RETURN_UNCIRCULATED);
      return success();
    } else {
      throw new IllegalArgumentException("Transaction state is not " + ITEM_RECEIVED.name() + " or " + RECEIVE_UNANNOUNCED.name());
    }
  }

  @Override
  public InnReachResponseDTO recall(String trackingId, String centralCode, RecallDTO recallDTO) {
    var transaction = getTransaction(trackingId, centralCode);
    var requestId = transaction.getHold().getFolioRequestId();
    var request = requestService.findRequest(requestId);
    var requestStatus = request.getStatus();

    if (requestStatus == OPEN_AWAITING_PICKUP || requestStatus == OPEN_IN_TRANSIT) {
      try {
        requestService.cancelRequest(transaction, "Item has been recalled.");
      } catch (Exception e) {
        throw new CirculationException("Unable to create a cancel request on the item: " + e.getMessage(), e);
      }
    } else {
      try {
        var recallUser = getRecallUserForCentralServer(centralCode);
        requestService.createRecallRequest(recallUser.getUserId(), transaction.getHold().getFolioItemId());
      } catch (Exception e) {
        throw new CirculationException("Unable to create a recall request on the item: " + e.getMessage(), e);
      }
    }
    transaction.setState(RECALL);

    return success();
  }

  @Override
  public InnReachResponseDTO borrowerRenew(String trackingId, String centralCode, BorrowerRenewDTO borrowerRenew) {
    var transaction = getTransaction(trackingId, centralCode);
    var hold = transaction.getHold();
    var loan = requestService.getLoan(hold.getFolioLoanId());
    var existingDueDate = loan.getDueDate();
    var requestedDueDate = new Date(borrowerRenew.getDueDateTime() * 1000L);

    try {
      var renewedLoan = renewLoan(hold);
      var calculatedDueDate = renewedLoan.getDueDate();

      if (calculatedDueDate.after(requestedDueDate) || calculatedDueDate.equals(requestedDueDate)) {
        transaction.setState(BORROWER_RENEW);
      } else {
        recallRequestToCentralSever(transaction, existingDueDate);
      }
    } catch (Exception e) {
      if (existingDueDate.before(requestedDueDate)) {
        recallRequestToCentralSever(transaction, existingDueDate);
      } else {
        throw new CirculationException("Failed to renew loan: " + e.getMessage(), e);
      }
    }

    return success();
  }

  @Override
  public InnReachResponseDTO finalCheckIn(String trackingId, String centralCode, BaseCircRequestDTO finalCheckIn) {
    var transaction = getTransaction(trackingId, centralCode);
    var state = transaction.getState();

    if (state == ITEM_IN_TRANSIT || state == RETURN_UNCIRCULATED) {
      transaction.setState(FINAL_CHECKIN);
      return success();
    } else {
      throw new IllegalArgumentException("Transaction state is not: " + ITEM_IN_TRANSIT.name() + " or " + RETURN_UNCIRCULATED.name());
    }
  }

  private InnReachRecallUser getRecallUserForCentralServer(String centralCode) {
    return centralserverRepository.fetchOneByCentralCode(centralCode)
      .map(CentralServer::getInnReachRecallUser)
      .orElseThrow(() -> new EntityNotFoundException("Recall user is not set for central server with code = " + centralCode));
  }

  private void recallRequestToCentralSever(InnReachTransaction transaction, Date existingDueDate) {
    var trackingId = transaction.getTrackingId();
    var centralCode = transaction.getCentralServerCode();

    String uri = resolveD2irCircPath(D2IR_ITEM_RECALL_OPERATION, trackingId, centralCode);

    var dueDateForRecallRequest = new HashMap<>();
    var convertedDate = existingDueDate.getTime() / 1000;
    dueDateForRecallRequest.put("dueDateTime", convertedDate);
    try {
      innReachExternalService.postInnReachApi(centralCode, uri, dueDateForRecallRequest);
      transaction.setState(RECALL);
    } catch (Exception e) {
        throw new CirculationException("Failed to recall request to central server: " + e.getMessage(), e);
    }
  }

  private CheckOutResponseDTO renewLoan(TransactionHold hold) {
    var renewLoanRequestDTO = new RenewLoanRequestDTO();
    renewLoanRequestDTO.setItemId(hold.getFolioItemId());
    renewLoanRequestDTO.setUserId(hold.getFolioPatronId());

    return requestService.renewLoan(renewLoanRequestDTO);
  }

  private String resolveD2irCircPath(String operation, String trackingId, String centralCode) {
    return String.format("/circ/%s/%s/%s", operation, trackingId, centralCode);
  }

  private InnReachResponseDTO success() {
    return new InnReachResponseDTO().status("ok").reason("success");
  }

  private void updateTransactionHold(TransactionHold existingTransactionHold, TransactionHoldDTO transactionHold) {
    // update transaction hold
    BeanUtils.copyProperties(transactionHold, existingTransactionHold, TRANSACTION_HOLD_IGNORE_PROPS_ON_COPY);

    // update pickupLocation
    var pickupLocation = pickupLocationMapper.fromString(transactionHold.getPickupLocation());
    BeanUtils.copyProperties(pickupLocation, existingTransactionHold.getPickupLocation(), PICKUP_LOC_IGNORE_PROPS_ON_COPY);
  }

  private InnReachTransaction createTransaction(String trackingId, String centralCode,
                                                TransactionHoldDTO transactionHold, TransactionType type) {
    TransactionHold hold;
    TransactionState state;
    if (type == TransactionType.PATRON) {
      hold = transactionHoldMapper.toPatronHold(transactionHold);
      state = PATRON_HOLD;
    } else if (type == TransactionType.LOCAL) {
      hold = transactionHoldMapper.toLocalHold(transactionHold);
      state = LOCAL_HOLD;
    } else {
      hold = transactionHoldMapper.toItemHold(transactionHold);
      state = ITEM_HOLD;
    }

    var newInnReachTransaction = new InnReachTransaction();
    newInnReachTransaction.setCentralServerCode(centralCode);
    newInnReachTransaction.setTrackingId(trackingId);
    newInnReachTransaction.setType(type);
    newInnReachTransaction.setHold(hold);
    newInnReachTransaction.setState(state);

    return newInnReachTransaction;
  }

  private UpdateOperation<InventoryItemDTO> changeFolioAssociatedItem(String folioItemBarcode, String callNumber) {
    return item -> {
      if (nonNull(folioItemBarcode)) {
        item.setBarcode(folioItemBarcode);
      }

      if (nonNull(callNumber)) {
        item.setCallNumber(callNumber);
      }

      return item;
    };
  }

  private Optional<Holding> removeHoldingsTransactionInfo(InventoryItemDTO item) {
    return holdingsService.changeAndUpdate(item.getHoldingsRecordId(), holding -> {
      holding.setCallNumber(null);
      return holding;
    });
  }

  private Optional<InventoryItemDTO> removeItemTransactionInfo(UUID itemId) {
    return itemService.changeAndUpdate(itemId, item -> {
      item.setCallNumber(null);
      item.setBarcode(null);
      return item;
    });
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

  private String unexpectedTransactionState(InnReachTransaction transaction) {
    return UNEXPECTED_TRANSACTION_STATE + transaction.getState();
  }

}
