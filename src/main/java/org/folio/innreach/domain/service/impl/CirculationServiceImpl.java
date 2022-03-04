package org.folio.innreach.domain.service.impl;

import static java.lang.String.format;
import static java.time.Instant.ofEpochSecond;
import static java.util.Objects.nonNull;
import static org.apache.commons.lang3.StringUtils.capitalize;

import static org.apache.commons.lang3.StringUtils.defaultIfEmpty;
import static org.apache.commons.lang3.StringUtils.equalsAny;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static org.folio.innreach.domain.dto.folio.circulation.RequestDTO.RequestStatus.CLOSED_CANCELLED;
import static org.folio.innreach.domain.dto.folio.circulation.RequestDTO.RequestStatus.OPEN_AWAITING_PICKUP;
import static org.folio.innreach.domain.dto.folio.circulation.RequestDTO.RequestStatus.OPEN_IN_TRANSIT;
import static org.folio.innreach.domain.entity.InnReachTransaction.TransactionState.BORROWER_RENEW;
import static org.folio.innreach.domain.entity.InnReachTransaction.TransactionState.BORROWING_SITE_CANCEL;
import static org.folio.innreach.domain.entity.InnReachTransaction.TransactionState.CANCEL_REQUEST;
import static org.folio.innreach.domain.entity.InnReachTransaction.TransactionState.CLAIMS_RETURNED;
import static org.folio.innreach.domain.entity.InnReachTransaction.TransactionState.FINAL_CHECKIN;
import static org.folio.innreach.domain.entity.InnReachTransaction.TransactionState.ITEM_HOLD;
import static org.folio.innreach.domain.entity.InnReachTransaction.TransactionState.ITEM_IN_TRANSIT;
import static org.folio.innreach.domain.entity.InnReachTransaction.TransactionState.ITEM_RECEIVED;
import static org.folio.innreach.domain.entity.InnReachTransaction.TransactionState.ITEM_SHIPPED;
import static org.folio.innreach.domain.entity.InnReachTransaction.TransactionState.LOCAL_HOLD;
import static org.folio.innreach.domain.entity.InnReachTransaction.TransactionState.OWNER_RENEW;
import static org.folio.innreach.domain.entity.InnReachTransaction.TransactionState.PATRON_HOLD;
import static org.folio.innreach.domain.entity.InnReachTransaction.TransactionState.RECALL;
import static org.folio.innreach.domain.entity.InnReachTransaction.TransactionState.RECEIVE_UNANNOUNCED;
import static org.folio.innreach.domain.entity.InnReachTransaction.TransactionState.RETURN_UNCIRCULATED;
import static org.folio.innreach.domain.entity.InnReachTransaction.TransactionState.TRANSFER;
import static org.folio.innreach.domain.entity.InnReachTransaction.TransactionType.ITEM;
import static org.folio.innreach.domain.entity.InnReachTransaction.TransactionType.LOCAL;
import static org.folio.innreach.domain.entity.InnReachTransaction.TransactionType.PATRON;

import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

import javax.persistence.EntityExistsException;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.innreach.domain.dto.folio.User;
import org.folio.innreach.domain.service.PatronTypeMappingService;
import org.folio.innreach.domain.service.UserService;
import org.folio.innreach.util.UUIDEncoder;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.Assert;

import org.folio.innreach.domain.dto.folio.circulation.RenewByIdDTO;
import org.folio.innreach.domain.dto.folio.inventory.InventoryItemDTO;
import org.folio.innreach.domain.entity.CentralServer;
import org.folio.innreach.domain.entity.InnReachRecallUser;
import org.folio.innreach.domain.entity.InnReachTransaction;
import org.folio.innreach.domain.entity.InnReachTransaction.TransactionState;
import org.folio.innreach.domain.entity.InnReachTransaction.TransactionType;
import org.folio.innreach.domain.entity.LocalAgency;
import org.folio.innreach.domain.entity.TransactionHold;
import org.folio.innreach.domain.exception.CirculationException;
import org.folio.innreach.domain.exception.EntityNotFoundException;
import org.folio.innreach.domain.service.CentralServerService;
import org.folio.innreach.domain.service.CirculationService;
import org.folio.innreach.domain.service.HoldingsService;
import org.folio.innreach.domain.service.ItemService;
import org.folio.innreach.domain.service.LoanService;
import org.folio.innreach.domain.service.MaterialTypeMappingService;
import org.folio.innreach.domain.service.PatronHoldService;
import org.folio.innreach.domain.service.RequestService;
import org.folio.innreach.dto.BaseCircRequestDTO;
import org.folio.innreach.dto.CancelRequestDTO;
import org.folio.innreach.dto.ClaimsItemReturnedDTO;
import org.folio.innreach.dto.Holding;
import org.folio.innreach.dto.InnReachResponseDTO;
import org.folio.innreach.dto.ItemReceivedDTO;
import org.folio.innreach.dto.ItemShippedDTO;
import org.folio.innreach.dto.LoanDTO;
import org.folio.innreach.dto.LocalHoldDTO;
import org.folio.innreach.dto.PatronHoldDTO;
import org.folio.innreach.dto.RecallDTO;
import org.folio.innreach.dto.RenewLoanDTO;
import org.folio.innreach.dto.ReturnUncirculatedDTO;
import org.folio.innreach.dto.TransactionHoldDTO;
import org.folio.innreach.dto.TransferRequestDTO;
import org.folio.innreach.external.service.InnReachExternalService;
import org.folio.innreach.mapper.InnReachTransactionHoldMapper;
import org.folio.innreach.mapper.InnReachTransactionPickupLocationMapper;
import org.folio.innreach.repository.CentralServerRepository;
import org.folio.innreach.repository.InnReachTransactionRepository;
import org.folio.innreach.repository.LocalAgencyRepository;

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
  private final LoanService loanService;
  private final InnReachExternalService innReachExternalService;
  private final CentralServerService centralServerService;
  private final MaterialTypeMappingService materialService;
  private final LocalAgencyRepository localAgencyRepository;
  private final PatronTypeMappingService patronTypeMappingService;
  private final UserService userService;

  private InnReachTransaction createTransactionWithItemHold(String trackingId, String centralCode) {
    var transaction = new InnReachTransaction();
    transaction.setTrackingId(trackingId);
    transaction.setCentralServerCode(centralCode);
    transaction.setType(InnReachTransaction.TransactionType.ITEM);
    transaction.setState(InnReachTransaction.TransactionState.ITEM_HOLD);
    return transaction;
  }

  @Override
  public InnReachResponseDTO createInnReachTransactionItemHold(String trackingId, String centralCode, TransactionHoldDTO dto) {
    try {
      transactionRepository.fetchOneByTrackingId(trackingId).ifPresent(m -> {
        throw new EntityExistsException("INN-Reach Transaction with tracking ID = " + trackingId
          + " already exists.");
      });
      var centralServer = centralServerService.getCentralServerByCentralCode(centralCode);
      var centralServerId = centralServer.getId();
      var transaction = createTransactionWithItemHold(trackingId, centralCode);
      var itemHold = transactionHoldMapper.toItemHold(dto);
      var item = itemService.getItemByHrId(itemHold.getItemId());
      var materialTypeId = item.getMaterialType().getId();
      var materialType = materialService.findByCentralServerAndMaterialType(centralServerId, materialTypeId);
      itemHold.setCentralItemType(materialType.getCentralItemType());
      itemHold.setTitle(item.getTitle());
      transaction.setHold(itemHold);
      transactionRepository.save(transaction);
    } catch (Exception e) {
      throw new CirculationException("An error occurred during creation of INN-Reach Transaction. " + e.getMessage(), e);
    }
    return success();
  }

  private final TransactionTemplate transactionTemplate;

  @Override
  public InnReachResponseDTO initiatePatronHold(String trackingId, String centralCode, PatronHoldDTO patronHold) {
    var transactionHold = transactionHoldMapper.mapRequest(patronHold);

    initiateTransactionHold(trackingId, centralCode, transactionHold, PATRON,
      (transaction, isExisting) -> {
        if (isExisting) {
          patronHoldService.updateVirtualItems(transaction);
        } else {
          patronHoldService.createVirtualItems(transaction);
        }
      });

    return success();
  }

  @Override
  public InnReachResponseDTO initiateLocalHold(String trackingId, String centralCode, LocalHoldDTO localHold) {
    var itemLocalAgency = findLocalAgency(localHold.getItemAgencyCode());
    var patronLocalAgency = findLocalAgency(localHold.getPatronAgencyCode());

    var itemLocalServer = itemLocalAgency.getCentralServer();
    var patronLocalServer = patronLocalAgency.getCentralServer();

    Assert.isTrue(itemLocalServer.equals(patronLocalServer),
      "The patron and item agencies should be on the same local server");

    var transactionHold = transactionHoldMapper.mapRequest(localHold);

    initiateTransactionHold(trackingId, centralCode, transactionHold, LOCAL,
      (transaction, isExisting) -> requestService.createLocalHoldRequest(transaction));

    return success();
  }

  @Override
  public InnReachResponseDTO trackPatronHoldShippedItem(String trackingId, String centralCode, ItemShippedDTO itemShipped) {
    var innReachTransaction = getTransactionOfType(trackingId, centralCode, PATRON);

    var itemBarcode = itemShipped.getItemBarcode();
    var callNumber = itemShipped.getCallNumber();

    if (nonNull(itemBarcode) || nonNull(callNumber)) {
      patronHoldService.addItemBarcodeAndCallNumber(innReachTransaction, itemBarcode, callNumber);
    }

    innReachTransaction.setState(ITEM_SHIPPED);

    return success();
  }

  @Override
  public InnReachResponseDTO cancelPatronHold(String trackingId, String centralCode, CancelRequestDTO cancelRequest) {
    log.info("Cancelling request for transaction: {}", trackingId);

    var transaction = getTransactionOfType(trackingId, centralCode, PATRON);

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
    var transaction = getTransactionOfType(trackingId, centralCode, PATRON);

    validateEquals(request::getItemId, () -> transaction.getHold().getItemId(), "item id");
    validateEquals(request::getItemAgencyCode, () -> transaction.getHold().getItemAgencyCode(), "item agency code");

    transaction.getHold().setItemId(request.getNewItemId());
    transaction.setState(TRANSFER);

    return success();
  }

  @Override
  public InnReachResponseDTO cancelItemHold(String trackingId, String centralCode, BaseCircRequestDTO cancelItemDTO) {
    var transaction = getTransactionOfType(trackingId, centralCode, ITEM);

    if (transaction.getHold().getFolioLoanId() != null) {
      throw new IllegalArgumentException("Requested item is already checked out.");
    }
    requestService.cancelRequest(transaction, "Request cancelled at borrowing site");
    transaction.setState(BORROWING_SITE_CANCEL);

    return success();
  }

  @Override
  public InnReachResponseDTO itemReceived(String trackingId, String centralCode, ItemReceivedDTO itemReceivedDTO) {
    var transaction = getTransactionOfType(trackingId, centralCode, ITEM);

    Assert.isTrue(transaction.getState() == ITEM_SHIPPED, unexpectedTransactionState(transaction));
    transaction.setState(ITEM_RECEIVED);

    var request = requestService.findRequest(transaction.getHold().getFolioRequestId());
    if (request.getStatus() == CLOSED_CANCELLED){
      innReachExternalService.postInnReachApi(centralCode, String.format("/circ/returnuncirculated/%s/%s", trackingId, centralCode));
      transaction.setState(RETURN_UNCIRCULATED);
    }

    return success();
  }

  @Override
  public InnReachResponseDTO receiveUnshipped(String trackingId, String centralCode,
                                              BaseCircRequestDTO receiveUnshippedRequest) {
    var transaction = getTransactionOfType(trackingId, centralCode, ITEM);

    if (transaction.getState() == TransactionState.ITEM_SHIPPED) {
      throw new IllegalArgumentException(unexpectedTransactionState(transaction));
    }

    if (transaction.getState() == TransactionState.ITEM_HOLD) {
      log.info("Attempting to create a loan");

      var folioPatronId = transaction.getHold().getFolioPatronId();
      var servicePointId = requestService.getDefaultServicePointIdForPatron(folioPatronId);
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
    var transaction = getTransactionOfType(trackingId, centralCode, ITEM);
    var state = transaction.getState();

    if (state == ITEM_RECEIVED || state == RECEIVE_UNANNOUNCED) {
      transaction.setState(RETURN_UNCIRCULATED);
      return success();
    } else {
      throw new IllegalArgumentException("Transaction state is not " + ITEM_RECEIVED.name() + " or " + RECEIVE_UNANNOUNCED.name());
    }
  }

  @Override
  public InnReachResponseDTO recall(String trackingId, String centralCode, RecallDTO recall) {
    var transaction = getTransactionOfType(trackingId, centralCode, PATRON);
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
        requestService.createRecallRequest(transaction, recallUser.getUserId());
      } catch (Exception e) {
        throw new CirculationException("Unable to create a recall request on the item: " + e.getMessage(), e);
      }
    }

    transaction.getHold().setDueDateTime(recall.getDueDateTime());
    transaction.setState(RECALL);

    return success();
  }

  @Override
  public InnReachResponseDTO borrowerRenewLoan(String trackingId, String centralCode, RenewLoanDTO renewLoan) {
    var transaction = getTransaction(trackingId, centralCode);
    var hold = transaction.getHold();
    var loan = loanService.getById(hold.getFolioLoanId());
    var existingDueDate = loan.getDueDate();
    var requestedDueDate = Date.from(ofEpochSecond(renewLoan.getDueDateTime()));

    try {
      hold.setDueDateTime(renewLoan.getDueDateTime());

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
  public InnReachResponseDTO ownerRenewLoan(String trackingId, String centralCode, RenewLoanDTO renewLoan) {
    var transaction = getTransactionOfType(trackingId, centralCode, PATRON);

    var renewedLoan = renewLoan(transaction.getHold());

    Instant calculatedDueDate = renewedLoan.getDueDate().toInstant();
    Instant requestedDueDate = ofEpochSecond(renewLoan.getDueDateTime());
    if (calculatedDueDate.isAfter(requestedDueDate)) {
      loanService.changeDueDate(renewedLoan, Date.from(requestedDueDate));
    }

    transaction.getHold().setDueDateTime(renewLoan.getDueDateTime());
    transaction.setState(OWNER_RENEW);

    return success();
  }

  @Override
  public InnReachResponseDTO finalCheckIn(String trackingId, String centralCode, BaseCircRequestDTO finalCheckIn) {
    var transaction = getTransaction(trackingId, centralCode);
    var state = transaction.getState();

    Assert.isTrue(state == ITEM_IN_TRANSIT || state == RETURN_UNCIRCULATED, unexpectedTransactionState(transaction));

    transaction.setState(FINAL_CHECKIN);

    return success();
  }

  @Override
  public InnReachResponseDTO claimsReturned(String trackingId, String centralCode, ClaimsItemReturnedDTO claimsItemReturned) {
    var transaction = getTransaction(trackingId, centralCode);

    var returnedDateSec = claimsItemReturned.getClaimsReturnedDate();
    var returnedDate = returnedDateSec != -1 ? Date.from(ofEpochSecond(returnedDateSec)) : new Date();

    var folioLoanId = transaction.getHold().getFolioLoanId();
    Assert.isTrue(folioLoanId != null, "Loan id is not set for transaction: " + trackingId);

    loanService.claimItemReturned(folioLoanId, returnedDate);

    transaction.setState(CLAIMS_RETURNED);

    return success();
  }

  private void initiateTransactionHold(String trackingId, String centralCode,
                                       TransactionHoldDTO transactionHold,
                                       TransactionType transactionType,
                                       BiConsumer<InnReachTransaction, Boolean> postProcessor) {

    var optTransaction = transactionRepository.findByTrackingIdAndCentralServerCode(trackingId, centralCode);
    var isExistingTransaction = optTransaction.isPresent();

    var transaction = transactionTemplate.execute(status -> {
      if (isExistingTransaction) {
        log.info("Transaction {} hold with trackingId [{}] and centralCode [{}] exists, start to update...",
          transactionType, trackingId, centralCode);

        var existingTransaction = optTransaction.get();

        updateTransactionHold(existingTransaction.getHold(), transactionHold);
        if (transactionType == PATRON) {
          populateTransactionHold(existingTransaction.getHold(), centralCode);
        }

        return transactionRepository.save(existingTransaction);
      } else {
        log.info("Transaction {} hold with trackingId [{}] and centralCode [{}] doesn't exist, create a new one...",
          transactionType, trackingId, centralCode);

        var newTransaction = createTransaction(trackingId, centralCode, transactionHold, transactionType);

        return transactionRepository.save(newTransaction);
      }
    });

    postProcessor.accept(transaction, isExistingTransaction);
  }

  private InnReachResponseDTO success() {
    return new InnReachResponseDTO().status("ok").reason("success");
  }

  private LoanDTO renewLoan(TransactionHold hold) {
    return loanService.renew(RenewByIdDTO.of(hold.getFolioItemId(), hold.getFolioPatronId()));
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

  private String resolveD2irCircPath(String operation, String trackingId, String centralCode) {
    return String.format("/circ/%s/%s/%s", operation, trackingId, centralCode);
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
    if (type == PATRON) {
      hold = transactionHoldMapper.toPatronHold(transactionHold);
      state = PATRON_HOLD;
      populateTransactionHold(hold, centralCode);
    } else if (type == LOCAL) {
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

  private TransactionHold populateTransactionHold(TransactionHold hold, String centralCode) {
    var user = getUser(hold);
    boolean changeName = matchName(user, hold.getPatronName());
    if (changeName) {
      hold.setPatronName(getPatronName(user));
    }
    var centralPatronType = getCentralPatronType(centralCode, user);
    hold.setCentralPatronType(centralPatronType);
    return hold;
  }

  private User getUser(TransactionHold hold) {
    var patronId = UUIDEncoder.decode(hold.getPatronId());
    System.out.println("getUser transactionHoldDTO patron id " + hold.getPatronId());
    return userService.getUserById(patronId)
      .orElseThrow(() -> new IllegalArgumentException("Patron is not found by id for creation patron hold transaction: " + patronId));
  }

  private Integer getCentralPatronType(String centralCode, User user) {
    var centralServer = centralServerService.getCentralServerByCentralCode(centralCode);
    var centralServerId = centralServer.getId();
    return patronTypeMappingService.getCentralPatronType(centralServerId, user.getPatronGroupId())
      .orElseThrow(() -> new IllegalStateException("centralPatronType is not resolved for patron with public id: " + user.getBarcode()));
  }

  private String getPatronName(User user) {
    var personal = user.getPersonal();

    var nameBuilder = new StringBuilder(personal.getLastName())
      .append(", ")
      .append(defaultIfEmpty(personal.getPreferredFirstName(), personal.getFirstName()));

    if (isNotEmpty(personal.getMiddleName())) {
      nameBuilder.append(" ").append(personal.getMiddleName());
    }
    return nameBuilder.toString();
  }

  private boolean matchName(User user, String patronName) {
    var personal = user.getPersonal();
    String[] patronNameTokens = patronName.split("\\s");

    if (patronNameTokens.length < 2) {
      return false;
    } else if (patronNameTokens.length == 2) {
      // "First Last" or "Middle Last" format
      return equalsAny(patronNameTokens[0], personal.getFirstName(), personal.getPreferredFirstName(), personal.getMiddleName()) &&
        patronNameTokens[1].equals(personal.getLastName());
    }

    // "First Middle Last" format
    return equalsAny(patronNameTokens[0], personal.getFirstName(), personal.getPreferredFirstName()) &&
      patronNameTokens[1].equals(personal.getMiddleName()) &&
      patronNameTokens[2].equals(personal.getLastName());
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
      format("%s [%s] from the request doesn't match with %s [%s] in the stored transaction",
        capitalize(fieldName), reqValue, fieldName.toLowerCase(), trxValue));
  }

  private InnReachTransaction getTransaction(String trackingId, String centralCode) {
    return transactionRepository.findByTrackingIdAndCentralServerCode(trackingId, centralCode)
      .orElseThrow(() -> new EntityNotFoundException(format(
        "InnReach transaction with tracking id [%s] and central code [%s] not found", trackingId, centralCode)));
  }

  private InnReachTransaction getTransactionOfType(String trackingId, String centralCode, TransactionType type) {
    InnReachTransaction transaction = getTransaction(trackingId, centralCode);

    if (transaction.getType() != type) {
      throw new IllegalArgumentException(format("InnReach transaction with tracking id [%s] and " +
        "central code [%s] is not of [%s] type", trackingId, centralCode, type));
    }

    return transaction;
  }

  private String unexpectedTransactionState(InnReachTransaction transaction) {
    return UNEXPECTED_TRANSACTION_STATE + transaction.getState();
  }

  private LocalAgency findLocalAgency(String code) {
    return localAgencyRepository.fetchOneByCode(code)
      .orElseThrow(() -> new EntityNotFoundException("Local agency with code: " + code + " not found."));
  }

}
