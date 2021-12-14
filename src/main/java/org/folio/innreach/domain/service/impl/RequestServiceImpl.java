package org.folio.innreach.domain.service.impl;

import static org.folio.innreach.domain.dto.folio.circulation.RequestDTO.FulfilmentPreference.HOLD_SHELF;
import static org.folio.innreach.domain.dto.folio.circulation.RequestDTO.RequestStatus.OPEN_AWAITING_DELIVERY;
import static org.folio.innreach.domain.dto.folio.circulation.RequestDTO.RequestStatus.OPEN_AWAITING_PICKUP;
import static org.folio.innreach.domain.dto.folio.circulation.RequestDTO.RequestStatus.OPEN_IN_TRANSIT;
import static org.folio.innreach.domain.dto.folio.circulation.RequestDTO.RequestStatus.OPEN_NOT_YET_FILLED;
import static org.folio.innreach.domain.dto.folio.circulation.RequestDTO.RequestType.HOLD;
import static org.folio.innreach.domain.dto.folio.circulation.RequestDTO.RequestType.PAGE;
import static org.folio.innreach.domain.dto.folio.circulation.RequestDTO.RequestType.RECALL;
import static org.folio.innreach.domain.dto.folio.inventory.InventoryItemStatus.AGED_TO_LOST;
import static org.folio.innreach.domain.dto.folio.inventory.InventoryItemStatus.AVAILABLE;
import static org.folio.innreach.domain.dto.folio.inventory.InventoryItemStatus.AWAITING_DELIVERY;
import static org.folio.innreach.domain.dto.folio.inventory.InventoryItemStatus.AWAITING_PICKUP;
import static org.folio.innreach.domain.dto.folio.inventory.InventoryItemStatus.CLAIMED_RETURNED;
import static org.folio.innreach.domain.dto.folio.inventory.InventoryItemStatus.DECLARED_LOST;
import static org.folio.innreach.domain.dto.folio.inventory.InventoryItemStatus.IN_PROCESS;
import static org.folio.innreach.domain.dto.folio.inventory.InventoryItemStatus.IN_PROCESS_NON_REQUESTABLE;
import static org.folio.innreach.domain.dto.folio.inventory.InventoryItemStatus.IN_TRANSIT;
import static org.folio.innreach.domain.dto.folio.inventory.InventoryItemStatus.LONG_MISSING;
import static org.folio.innreach.domain.dto.folio.inventory.InventoryItemStatus.LOST_AND_PAID;
import static org.folio.innreach.domain.dto.folio.inventory.InventoryItemStatus.MISSING;
import static org.folio.innreach.domain.dto.folio.inventory.InventoryItemStatus.ON_ORDER;
import static org.folio.innreach.domain.dto.folio.inventory.InventoryItemStatus.PAGED;
import static org.folio.innreach.domain.dto.folio.inventory.InventoryItemStatus.UNAVAILABLE;
import static org.folio.innreach.domain.dto.folio.inventory.InventoryItemStatus.UNKNOWN;
import static org.folio.innreach.domain.dto.folio.inventory.InventoryItemStatus.WITHDRAWN;
import static org.folio.innreach.domain.entity.InnReachTransaction.TransactionState.CANCEL_REQUEST;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.Set;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import org.folio.innreach.client.CirculationClient;
import org.folio.innreach.client.ServicePointsUsersClient;
import org.folio.innreach.client.UsersClient;
import org.folio.innreach.domain.dto.OwningSiteCancelsRequestDTO;
import org.folio.innreach.domain.dto.folio.ResultList;
import org.folio.innreach.domain.dto.folio.User;
import org.folio.innreach.domain.dto.folio.circulation.MoveRequestDTO;
import org.folio.innreach.domain.dto.folio.circulation.RequestDTO;
import org.folio.innreach.domain.dto.folio.circulation.RequestDTO.RequestStatus;
import org.folio.innreach.domain.dto.folio.circulation.RequestDTO.RequestType;
import org.folio.innreach.domain.dto.folio.inventory.InventoryItemDTO;
import org.folio.innreach.domain.dto.folio.inventory.InventoryItemStatus;
import org.folio.innreach.domain.entity.CentralPatronTypeMapping;
import org.folio.innreach.domain.entity.InnReachTransaction;
import org.folio.innreach.domain.entity.TransactionHold;
import org.folio.innreach.domain.entity.TransactionItemHold;
import org.folio.innreach.domain.entity.TransactionLocalHold;
import org.folio.innreach.domain.exception.EntityNotFoundException;
import org.folio.innreach.domain.exception.ItemNotRequestableException;
import org.folio.innreach.domain.service.HoldingsService;
import org.folio.innreach.domain.service.ItemService;
import org.folio.innreach.domain.service.RequestService;
import org.folio.innreach.dto.CheckInRequestDTO;
import org.folio.innreach.dto.CheckInResponseDTO;
import org.folio.innreach.dto.CheckOutRequestDTO;
import org.folio.innreach.dto.Holding;
import org.folio.innreach.dto.LoanDTO;
import org.folio.innreach.external.service.InnReachExternalService;
import org.folio.innreach.mapper.InnReachTransactionPickupLocationMapper;
import org.folio.innreach.repository.CentralPatronTypeMappingRepository;
import org.folio.innreach.repository.CentralServerRepository;
import org.folio.innreach.repository.InnReachTransactionRepository;
import org.folio.innreach.util.UUIDHelper;

@Log4j2
@RequiredArgsConstructor
@Service
public class RequestServiceImpl implements RequestService {
  private static final String OWNING_SITE_CANCELS_REQUEST_BASE_URI = "/circ/owningsitecancel/";

  private static final Set<RequestStatus> openRequestStatuses = Set.of(OPEN_AWAITING_PICKUP, OPEN_AWAITING_DELIVERY,
    OPEN_IN_TRANSIT, OPEN_NOT_YET_FILLED);
  private static final Set<InventoryItemStatus> notAvailableItemStatuses = Set.of(
    LONG_MISSING, DECLARED_LOST, AGED_TO_LOST, LOST_AND_PAID, AWAITING_PICKUP, MISSING, PAGED, CLAIMED_RETURNED,
    WITHDRAWN, AWAITING_DELIVERY, IN_PROCESS_NON_REQUESTABLE, UNAVAILABLE, UNKNOWN);
  private static final Set<InventoryItemStatus> noOpenRequestsAvailableStatuses = Set.of(
    IN_TRANSIT, IN_PROCESS, ON_ORDER);

  private static final UUID INN_REACH_CANCELLATION_REASON_ID = UUID.fromString("941c7055-04f8-4db3-82cb-f63965c1506f");

  private final InnReachTransactionRepository transactionRepository;
  private final CentralPatronTypeMappingRepository centralPatronTypeMappingRepository;
  private final CentralServerRepository centralServerRepository;

  private final InnReachTransactionPickupLocationMapper transactionPickupLocationMapper;
  private final CirculationClient circulationClient;
  private final ServicePointsUsersClient servicePointsUsersClient;
  private final UsersClient usersClient;

  private final InnReachExternalService innReachService;
  private final ItemService itemService;
  private final HoldingsService holdingsService;

  @Async
  @Override
  public void createItemHoldRequest(String trackingId) {
    var transaction = fetchTransactionByTrackingId(trackingId);
    var centralServerId = getCentralServerId(transaction.getCentralServerCode());
    var hold = (TransactionItemHold) transaction.getHold();
    var patronType = hold.getCentralPatronType();
    var patronBarcode = getUserBarcode(centralServerId, patronType);
    var patron = getUserByBarcode(patronBarcode);
    var centralPatronName = hold.getPatronName();

    createOwningSiteItemRequest(transaction, patron, centralPatronName);
  }

  @Async
  @Override
  public void createLocalHoldRequest(String trackingId) {
    var transaction = fetchTransactionByTrackingId(trackingId);
    var hold = (TransactionLocalHold) transaction.getHold();
    var patronId = UUIDHelper.fromStringWithoutHyphens(hold.getPatronId());
    var patron = getUserById(patronId);
    var centralPatronName = hold.getPatronName();

    createOwningSiteItemRequest(transaction, patron, centralPatronName);
  }

  @Override
  public void createItemRequest(InnReachTransaction transaction, Holding holding, InventoryItemDTO item,
                                User patron, UUID servicePointId, RequestType requestType) {
    log.info("Creating item request for transaction {}", transaction);
    var hold = transaction.getHold();

    validateItemAvailability(item);

    //getting required data for a request
    var comment = createPatronComment(hold);

    var requestExpirationDate = getRequestExpirationDate(hold);

    //creating and sending new request
    var newRequest = RequestDTO.builder()
      .requestType(requestType.getName())
      .itemId(item.getId())
      .requesterId(patron.getId())
      .pickupServicePointId(servicePointId)
      .requestExpirationDate(requestExpirationDate)
      .patronComments(comment)
      .requestDate(transaction.getCreatedDate())
      .fulfilmentPreference(HOLD_SHELF.getName())
      .build();
    var createdRequest = circulationClient.sendRequest(newRequest);

    updateTransaction(transaction, item, holding, createdRequest, patron);

    log.info("Item request successfully created.");
  }

  @Override
  public void moveItemRequest(InnReachTransaction transaction, Holding holding, InventoryItemDTO item) {
    log.info("Moving item request for transaction {}", transaction);

    var hold = transaction.getHold();
    var requestId = hold.getFolioRequestId();
    Assert.isTrue(requestId != null, "requestId is not set for transaction with trackingId: " + transaction.getTrackingId());

    validateItemAvailability(item);

    var payload = MoveRequestDTO.builder()
      .requestType(PAGE.getName())
      .destinationItemId(item.getId())
      .build();

    var movedRequest = circulationClient.moveRequest(requestId, payload);

    updateTransaction(transaction, item, holding, movedRequest, null);

    log.info("Item request successfully moved");
  }

  @Override
  public void cancelRequest(InnReachTransaction transaction, String reason) {
    log.info("Canceling item request for transaction {}", transaction);

    var requestId = transaction.getHold().getFolioRequestId();
    if (requestId == null) {
      log.warn("FOLIO requestId is not set for transaction with trackingId: {}", transaction.getTrackingId());
      return;
    }

    circulationClient.findRequest(requestId)
      .ifPresentOrElse(r -> cancelRequest(r, reason),
        () -> log.warn("No request found with id {}", requestId));
  }

  @Override
  public CheckInResponseDTO checkInItem(InnReachTransaction transaction, UUID servicePointId) {
    log.info("Processing item check-in for transaction {}", transaction);

    var checkIn = new CheckInRequestDTO()
      .servicePointId(servicePointId)
      .itemBarcode(transaction.getHold().getFolioItemBarcode())
      .checkInDate(new Date());

    return circulationClient.checkInByBarcode(checkIn);
  }

  @Override
  public LoanDTO checkOutItem(InnReachTransaction transaction, UUID servicePointId) {
    log.info("Processing item check-out for transaction {}", transaction);

    var hold = transaction.getHold();

    var checkOut = new CheckOutRequestDTO()
      .servicePointId(servicePointId)
      .userBarcode(hold.getFolioPatronBarcode())
      .itemBarcode(hold.getFolioItemBarcode());

    return circulationClient.checkOutByBarcode(checkOut);
  }

  private void createOwningSiteItemRequest(InnReachTransaction transaction, User patron, String centralPatronName) {
    try {
      var hold = transaction.getHold();
      var servicePointId = getDefaultServicePointId(patron.getId());
      var item = itemService.getItemByHrId(hold.getItemId());
      var requestType = item.getStatus() == AVAILABLE ? PAGE : HOLD;
      var holding = holdingsService.find(item.getHoldingsRecordId()).orElse(null);

      createItemRequest(transaction, holding, item, patron, servicePointId, requestType);

    } catch (Exception e) {
      log.warn("An error occurred during request processing", e);

      cancelTransaction(transaction);

      var errorReason = e instanceof ItemNotRequestableException ? "Item not available" : "Request not permitted";
      issueOwningSiteCancelsRequest(transaction, centralPatronName, errorReason);
    }
  }

  private void cancelTransaction(InnReachTransaction transaction) {
    transaction.setState(CANCEL_REQUEST);
    transactionRepository.save(transaction);
  }

  @Override
  public void createRecallRequest(UUID recallUserId, UUID itemId) {
    var pickupServicePoint = getDefaultServicePointId(recallUserId);

    var request = RequestDTO.builder()
      .itemId(itemId)
      .requesterId(recallUserId)
      .requestType(RECALL.getName())
      .requestDate(OffsetDateTime.now())
      .fulfilmentPreference(HOLD_SHELF.getName())
      .pickupServicePointId(pickupServicePoint)
      .build();
    circulationClient.sendRequest(request);
  }

  @Override
  public RequestDTO findRequest(UUID requestId) {
    return circulationClient.findRequest(requestId).orElseThrow(() -> new EntityNotFoundException(
      "No request found with id = " + requestId));
  }

  @Override
  public LoanDTO findLoan(UUID loanId) {
    return circulationClient.findLoan(loanId).orElseThrow(() -> new EntityNotFoundException (
        "No loan found with id = " + loanId));
  }

  @Override
  public LoanDTO updateLoan(LoanDTO loan) {
    circulationClient.updateLoan(loan.getId(), loan);
    return loan;
  }

  private void cancelRequest(RequestDTO request, String reason) {
    request.setStatus(RequestStatus.CLOSED_CANCELLED);
    request.setCancellationReasonId(INN_REACH_CANCELLATION_REASON_ID);
    request.setCancellationAdditionalInformation(reason);

    circulationClient.updateRequest(request.getId(), request);

    log.info("Item request successfully cancelled");
  }

  private void validateItemAvailability(InventoryItemDTO item) {
    var requests = circulationClient.queryRequestsByItemId(item.getId());
    if (!isItemRequestable(item, requests)) {
      throw new ItemNotRequestableException("Item is not requestable: " + item.getId());
    }
  }

  private void updateTransaction(InnReachTransaction transaction, InventoryItemDTO item,
                                 Holding holding, RequestDTO request, User patron) {
    var hold = transaction.getHold();
    hold.setFolioRequestId(request.getId());
    hold.setFolioItemId(item.getId());
    hold.setFolioItemBarcode(item.getBarcode());
    if (holding != null) {
      hold.setFolioHoldingId(holding.getId());
      hold.setFolioInstanceId(holding.getInstanceId());
    }
    if (patron != null) {
      hold.setFolioPatronId(patron.getId());
      hold.setFolioPatronBarcode(patron.getBarcode());
    }
    transactionRepository.save(transaction);
  }

  private boolean isItemRequestable(InventoryItemDTO item, ResultList<RequestDTO> requests) {
    return item.getStatus() == AVAILABLE ||
      !notAvailableItemStatuses.contains(item.getStatus()) ||
      noOpenRequests(requests) && noOpenRequestsAvailableStatuses.contains(item.getStatus());
  }

  private boolean noOpenRequests(ResultList<RequestDTO> requests) {
    return requests.getResult().stream().noneMatch(r -> openRequestStatuses.contains(r.getStatus()));
  }

  private String createPatronComment(TransactionHold hold) {
    return "INN-Reach request: Patron Agency - " + hold.getPatronAgencyCode() +
      ", Pickup Location - " + transactionPickupLocationMapper.toString(hold.getPickupLocation());
  }

  private UUID getCentralServerId(String centralServerCode) {
    return centralServerRepository.fetchOneByCentralCode(centralServerCode)
      .orElseThrow(() -> new EntityNotFoundException("Central server not found for central code = " + centralServerCode)
      ).getId();
  }

  private UUID getDefaultServicePointId(UUID userId) {
    return servicePointsUsersClient.findServicePointsUsers(userId)
      .getResult().stream().findFirst().orElseThrow(
        () -> new EntityNotFoundException("Service points not found for user id = " + userId)
      ).getDefaultServicePointId();
  }

  private String queryByBarcode(String patronBarcode) {
    return "(barcode==\"" + patronBarcode + "\")";
  }

  private User getUserByBarcode(String patronBarcode) {
    return usersClient.query(queryByBarcode(patronBarcode)).getResult().stream().findFirst().orElseThrow(
      () -> new EntityNotFoundException("User not found for barcode = " + patronBarcode)
    );
  }

  private User getUserById(UUID patronId) {
    return usersClient.getUserById(patronId)
      .orElseThrow(() -> new IllegalArgumentException("Patron is not found by id: " + patronId));
  }

  private String getUserBarcode(UUID centralServerId, Integer patronType) {
    return centralPatronTypeMappingRepository.findOneByCentralServerIdAndCentralPatronType(centralServerId, patronType)
      .map(CentralPatronTypeMapping::getBarcode)
      .orElseThrow(() ->
        new EntityNotFoundException("User barcode not found for central server id = " + centralServerId +
          " and patron type = " + patronType));
  }

  private InnReachTransaction fetchTransactionByTrackingId(String trackingId) {
    return transactionRepository.fetchOneByTrackingId(trackingId).orElseThrow(() ->
      new EntityNotFoundException("INN-Reach transaction not found for trackingId = " + trackingId)
    );
  }

  private OffsetDateTime getRequestExpirationDate(TransactionHold hold) {
    return hold.getNeedBefore() == null ? null :
      OffsetDateTime.ofInstant(Instant.ofEpochSecond(hold.getNeedBefore()), ZoneOffset.UTC);
  }

  private void issueOwningSiteCancelsRequest(InnReachTransaction transaction, String patronName, String reason) {
    log.info("Issuing owning site cancel request for transaction {}. Reason: {}", transaction.getTrackingId(), reason);
    var trackingId = transaction.getTrackingId();
    var centralCode = transaction.getCentralServerCode();

    var requestPath = OWNING_SITE_CANCELS_REQUEST_BASE_URI + trackingId + "/" + centralCode;

    var request = OwningSiteCancelsRequestDTO.builder()
      .reason(reason)
      .reasonCode(7)
      .localBibId(transaction.getHold().getItemId())
      .patronName(patronName)
      .build();

    innReachService.postInnReachApi(centralCode, requestPath, request);
  }
}
