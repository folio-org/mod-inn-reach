package org.folio.innreach.domain.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.innreach.client.ConfigurationClient;
import org.folio.innreach.domain.dto.folio.circulation.RenewByIdDTO;
import org.folio.innreach.domain.dto.folio.inventory.InventoryItemDTO;
import org.folio.innreach.domain.entity.InnReachTransaction;
import org.folio.innreach.domain.entity.InnReachTransaction.TransactionState;
import org.folio.innreach.domain.entity.InnReachTransaction.TransactionType;
import org.folio.innreach.domain.entity.LocalAgency;
import org.folio.innreach.domain.entity.TransactionHold;
import org.folio.innreach.domain.event.CancelRequestEvent;
import org.folio.innreach.domain.event.RecallRequestEvent;
import org.folio.innreach.domain.exception.CirculationException;
import org.folio.innreach.domain.exception.EntityNotFoundException;
import org.folio.innreach.domain.service.*;
import org.folio.innreach.dto.*;
import org.folio.innreach.external.service.InnReachExternalService;
import org.folio.innreach.mapper.InnReachTransactionHoldMapper;
import org.folio.innreach.mapper.InnReachTransactionPickupLocationMapper;
import org.folio.innreach.repository.InnReachTransactionRepository;
import org.folio.innreach.repository.LocalAgencyRepository;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.FolioModuleMetadata;
import org.folio.spring.scope.FolioExecutionContextSetter;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.Assert;

import javax.persistence.EntityExistsException;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

import static java.lang.String.format;
import static java.time.Instant.ofEpochSecond;
import static java.util.Objects.nonNull;
import static org.apache.commons.lang3.StringUtils.capitalize;
import static org.apache.commons.lang3.StringUtils.truncate;
import static org.folio.innreach.domain.dto.folio.circulation.RequestDTO.RequestStatus.OPEN_AWAITING_PICKUP;
import static org.folio.innreach.domain.dto.folio.circulation.RequestDTO.RequestStatus.OPEN_IN_TRANSIT;
import static org.folio.innreach.domain.entity.InnReachTransaction.TransactionState.*;
import static org.folio.innreach.domain.entity.InnReachTransaction.TransactionType.*;
import static org.folio.innreach.domain.service.impl.RequestServiceImpl.INN_REACH_CANCELLATION_REASON_ID;
import static org.folio.innreach.util.DateHelper.toEpochSec;
import static org.folio.innreach.util.InnReachTransactionUtils.*;
import static org.folio.innreach.util.JsonHelper.getCheckOutTimeDuration;

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
  public static final String CHECKOUT = "CHECKOUT";

  @Qualifier("modAsyncExecutor")
  private final ThreadPoolTaskScheduler taskExecutor;
  private final FolioExecutionContext folioExecutionContext;
  private final InnReachTransactionRepository transactionRepository;
  private final InnReachRecallUserService recallUserService;
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
  private final PatronInfoService patronInfoService;
  private final TransactionTemplate transactionTemplate;
  private final ApplicationEventPublisher eventPublisher;
  private final VirtualRecordService virtualRecordService;
  private final ConfigurationService configurationService;

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
      itemHold.setTitle(truncate(item.getTitle(), 255));
      transaction.setHold(itemHold);
      transactionRepository.save(transaction);
    } catch (Exception e) {
      throw new CirculationException("An error occurred during creation of INN-Reach Transaction. " + e.getMessage(), e);
    }
    return success();
  }

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
    patronInfoService.populateTransactionPatronInfo(innReachTransaction.getHold(), centralCode);

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
    log.info("Cancelling Patron Hold transaction: {}", trackingId);

    var transaction = getTransactionOfType(trackingId, centralCode, PATRON);
    patronInfoService.populateTransactionPatronInfo(transaction.getHold(), centralCode);

    transaction.setState(CANCEL_REQUEST);

    var folioItemId = transaction.getHold().getFolioItemId();

    removeItemTransactionInfo(folioItemId)
      .ifPresent(this::removeHoldingsTransactionInfo);

    var folioHoldingId = transaction.getHold().getFolioHoldingId();
    var folioInstanceId = transaction.getHold().getFolioInstanceId();
    var folioLoanId = transaction.getHold().getFolioLoanId();

     virtualRecordService.deleteVirtualRecords(folioItemId,folioHoldingId,folioInstanceId,folioLoanId);

    eventPublisher.publishEvent(CancelRequestEvent.of(transaction,
      INN_REACH_CANCELLATION_REASON_ID, cancelRequest.getReason()));

    clearPatronAndItemInfo(transaction.getHold());

    log.info("Item request successfully cancelled");

    return success();
  }
  @Override
  public InnReachResponseDTO transferPatronHoldItem(String trackingId, String centralCode, TransferRequestDTO request) {
    var transaction = getTransactionOfType(trackingId, centralCode, PATRON);
    patronInfoService.populateTransactionPatronInfo(transaction.getHold(), centralCode);

    validateEquals(request::getItemId, () -> transaction.getHold().getItemId(), "item id");
    validateEquals(request::getItemAgencyCode, () -> transaction.getHold().getItemAgencyCode(), "item agency code");

    transaction.getHold().setItemId(request.getNewItemId());
    transaction.setState(TRANSFER);

    return success();
  }

  @Override
  public InnReachResponseDTO cancelItemHold(String trackingId, String centralCode, BaseCircRequestDTO cancelItemDTO) {
    log.info("Cancelling Item Hold transaction: {}", trackingId);

    var transaction = getTransactionOfType(trackingId, centralCode, ITEM);
    var requestId = transaction.getHold().getFolioRequestId();

    if (transaction.getHold().getFolioLoanId() != null) {
      throw new IllegalArgumentException("Requested item is already checked out.");
    }

    transaction.setState(BORROWING_SITE_CANCEL);

    clearCentralPatronInfo(transaction.getHold());

    eventPublisher.publishEvent(new CancelRequestEvent(trackingId, requestId,
      INN_REACH_CANCELLATION_REASON_ID, "Request cancelled at borrowing site"));

    return success();
  }

  @Override
  public InnReachResponseDTO itemReceived(String trackingId, String centralCode, ItemReceivedDTO itemReceivedDTO) {
    var transaction = getTransactionOfType(trackingId, centralCode, ITEM);

    verifyState(transaction, ITEM_SHIPPED, ITEM_HOLD, TRANSFER);

    if (transaction.getState() != ITEM_SHIPPED) {
      createLoan(transaction);
    }
    transaction.setState(ITEM_RECEIVED);

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
      createLoan(transaction);
      transaction.setState(RECEIVE_UNANNOUNCED);
    }

    return success();
  }

  private void createLoan(InnReachTransaction transaction) {
    log.info("Attempting to create a loan");

    var request = requestService.findRequest(transaction.getHold().getFolioRequestId());
    var servicePointId = request.getPickupServicePointId();
    var checkOutResponse = loanService.checkOutItem(transaction, servicePointId);
    var loanId = checkOutResponse.getId();

    log.info("Created a loan with id {}", loanId);

    transaction.getHold().setFolioLoanId(loanId);
  }

  @Override
  public InnReachResponseDTO itemInTransit(String trackingId, String centralCode, BaseCircRequestDTO itemInTransitRequest) {
    var transaction = getTransaction(trackingId, centralCode);

    verifyState(transaction, ITEM_RECEIVED, RECEIVE_UNANNOUNCED);

    transaction.setState(ITEM_IN_TRANSIT);

    return success();
  }

  @Override
  public InnReachResponseDTO returnUncirculated(String trackingId, String centralCode, ReturnUncirculatedDTO returnUncirculated) {
    var transaction = getTransactionOfType(trackingId, centralCode, ITEM);

    verifyState(transaction, ITEM_RECEIVED, RECEIVE_UNANNOUNCED);

    transaction.setState(RETURN_UNCIRCULATED);

    return success();
  }

  @Override
  public InnReachResponseDTO recall(String trackingId, String centralCode, RecallDTO recall) {
    var transaction = getTransactionOfType(trackingId, centralCode, PATRON);
    patronInfoService.populateTransactionPatronInfo(transaction.getHold(), centralCode);

    var requestId = transaction.getHold().getFolioRequestId();
    var request = requestService.findRequest(requestId);
    var requestStatus = request.getStatus();

    transaction.getHold().setDueDateTime(recall.getDueDateTime());
    transaction.setState(RECALL);

    if (requestStatus == OPEN_AWAITING_PICKUP || requestStatus == OPEN_IN_TRANSIT) {
      eventPublisher.publishEvent(CancelRequestEvent.of(transaction,
        INN_REACH_CANCELLATION_REASON_ID, "Item has been recalled."));
    } else {
      var recallUser =  recallUserService.getRecallUserForCentralServer(centralCode);
      eventPublisher.publishEvent(RecallRequestEvent.of(transaction.getHold(), recallUser));
    }

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
    patronInfoService.populateTransactionPatronInfo(transaction.getHold(), centralCode);

    var renewedLoan = renewLoan(transaction.getHold());

    var calculatedDueDate = renewedLoan.getDueDate().toInstant();
    var requestedDueDate = ofEpochSecond(renewLoan.getDueDateTime());
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

    verifyStateNot(transaction, PATRON_HOLD, TRANSFER);

    transaction.setState(FINAL_CHECKIN);

    removeItemTransactionInfo(transaction.getHold().getFolioItemId())
      .ifPresent(this::removeHoldingsTransactionInfo);
    clearPatronAndItemInfo(transaction.getHold());

    var folioItemId = transaction.getHold().getFolioItemId();
    var folioHoldingId = transaction.getHold().getFolioHoldingId();
    var folioInstanceId = transaction.getHold().getFolioInstanceId();
    var folioLoanId = transaction.getHold().getFolioLoanId();

    log.info("folioItem->"+folioItemId);
    log.info("folioHolding->"+folioHoldingId);
    log.info("folioInstance->"+folioInstanceId);

    // fetching configurations
    var configDataList
            = configurationService.fetchConfigurationsDetailsByModule(CHECKOUT);

    log.info("Configuration data {}",configDataList.getResult());

    // fetching checkOutTimeDuration
    Long checkOutTimeDuration = getCheckOutTimeDuration(configDataList.getResult());

    log.info("checkOutTimeDuration :{}",checkOutTimeDuration);

    log.info("deleteVirtualRecords execution start time : " + new Date());
    var task = new FolioAsyncExecutorWrapper(folioExecutionContext,
            () -> executeDeleteVirtualRecordsWithDelay(folioItemId, folioHoldingId,
                    folioInstanceId, folioLoanId));

    taskExecutor.schedule(task, new Date(System.currentTimeMillis() + checkOutTimeDuration));

    // needs to be tested for multi tenant

    return success();
  }

  private void executeDeleteVirtualRecordsWithDelay(UUID folioItemId,
                                                    UUID folioHoldingId, UUID folioInstanceId, UUID folioLoanId) {
    virtualRecordService.deleteVirtualRecords(folioItemId, folioHoldingId, folioInstanceId, folioLoanId);
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
    clearCentralPatronInfo(transaction.getHold());

    return success();
  }

  private InnReachTransaction createTransactionWithItemHold(String trackingId, String centralCode) {
    var transaction = new InnReachTransaction();
    transaction.setTrackingId(trackingId);
    transaction.setCentralServerCode(centralCode);
    transaction.setType(InnReachTransaction.TransactionType.ITEM);
    transaction.setState(InnReachTransaction.TransactionState.ITEM_HOLD);
    return transaction;
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
          patronInfoService.populateTransactionPatronInfo(existingTransaction.getHold(), centralCode);
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

  private void recallRequestToCentralSever(InnReachTransaction transaction, Date existingDueDate) {
    var trackingId = transaction.getTrackingId();
    var centralCode = transaction.getCentralServerCode();

    var uri = resolveD2irCircPath(D2IR_ITEM_RECALL_OPERATION, trackingId, centralCode);

    var payload = new HashMap<>();
    payload.put("dueDateTime", toEpochSec(existingDueDate));
    try {
      innReachExternalService.postInnReachApi(centralCode, uri, payload);
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
      patronInfoService.populateTransactionPatronInfo(hold, centralCode);
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

  /*
   * this must be removed when the same functionality is implemented in the folio-spring-base library
   */
  private static class LocalFolioExecutionContext implements FolioExecutionContext {
    private final String tenantId;
    private final String okapiUrl;
    private final String token;
    private final UUID userId;
    private final String requestId;
    private final Map<String, Collection<String>> allHeaders;
    private final Map<String, Collection<String>> okapiHeaders;
    private final FolioModuleMetadata folioModuleMetadata;

    private LocalFolioExecutionContext(FolioExecutionContext sourceContext) {
      tenantId = sourceContext.getTenantId();
      okapiUrl = sourceContext.getOkapiUrl();
      this.token = sourceContext.getToken();
      this.userId = sourceContext.getUserId();
      requestId = sourceContext.getRequestId();
      allHeaders = sourceContext.getAllHeaders();
      okapiHeaders = sourceContext.getOkapiHeaders();
      folioModuleMetadata = sourceContext.getFolioModuleMetadata();
    }


    public String getTenantId() {
      return tenantId;
    }

    public String getOkapiUrl() {
      return okapiUrl;
    }

    public String getToken() {
      return token;
    }

    public UUID getUserId() {
      return userId;
    }

    public String getRequestId() {
      return requestId;
    }

    public Map<String, Collection<String>> getAllHeaders() {
      return allHeaders;
    }

    public Map<String, Collection<String>> getOkapiHeaders() {
      return okapiHeaders;
    }

    public FolioModuleMetadata getFolioModuleMetadata() {
      return folioModuleMetadata;
    }
  }

  private static class FolioAsyncExecutorWrapper implements Runnable {
    private final FolioExecutionContext localFolioExecutionContext;
    private final Runnable task;

    private FolioAsyncExecutorWrapper(FolioExecutionContext executionContext, Runnable task) {
      this.localFolioExecutionContext = new LocalFolioExecutionContext(executionContext);
      this.task = task;
    }

    @Override
    public void run() {
      try (var contextSetter = new FolioExecutionContextSetter(localFolioExecutionContext)) {
        task.run();
      }
    }
  }
}
