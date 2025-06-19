package org.folio.innreach.domain.service.impl;

import static org.folio.innreach.domain.dto.folio.circulation.RequestDTO.FulfillmentPreference.HOLD_SHELF;
import static org.folio.innreach.domain.dto.folio.circulation.RequestDTO.RequestStatus.CLOSED_CANCELLED;
import static org.folio.innreach.domain.dto.folio.circulation.RequestDTO.RequestStatus.CLOSED_PICKUP_EXPIRED;
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
import static org.folio.innreach.domain.entity.InnReachTransaction.TransactionType.ITEM;
import static org.folio.innreach.util.CqlHelper.matchAny;
import static org.folio.innreach.util.ListUtils.getFirstItem;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.innreach.client.ItemStorageClient;
import org.folio.innreach.domain.dto.folio.inventory.InventoryInstanceDTO;
import org.folio.innreach.domain.service.RecordContributionService;
import org.folio.innreach.domain.service.UserService;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import org.folio.innreach.client.CirculationClient;
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
import org.folio.innreach.domain.entity.TransactionLocalHold;
import org.folio.innreach.domain.exception.CirculationException;
import org.folio.innreach.domain.exception.EntityNotFoundException;
import org.folio.innreach.domain.exception.ItemNotRequestableException;
import org.folio.innreach.domain.service.HoldingsService;
import org.folio.innreach.domain.service.InventoryService;
import org.folio.innreach.domain.service.ItemService;
import org.folio.innreach.domain.service.RequestPreferenceService;
import org.folio.innreach.domain.service.RequestService;
import org.folio.innreach.domain.service.CentralServerService;
import org.folio.innreach.domain.service.InstanceService;
import org.folio.innreach.dto.Item;
import org.folio.innreach.dto.Holding;
import org.folio.innreach.external.service.InnReachExternalService;
import org.folio.innreach.mapper.InnReachTransactionPickupLocationMapper;
import org.folio.innreach.repository.CentralPatronTypeMappingRepository;
import org.folio.innreach.repository.CentralServerRepository;
import org.folio.innreach.repository.InnReachTransactionRepository;
import org.folio.innreach.util.UUIDEncoder;

@Log4j2
@RequiredArgsConstructor
@Service
public class RequestServiceImpl implements RequestService {
  private static final String OWNING_SITE_CANCELS_REQUEST_BASE_URI = "/circ/owningsitecancel/";

  private static final Set<RequestStatus> openRequestStatuses = Set.of(
    OPEN_AWAITING_PICKUP, OPEN_AWAITING_DELIVERY, OPEN_IN_TRANSIT, OPEN_NOT_YET_FILLED);
  private static final Set<RequestStatus> canceledExpiredStatuses = Set.of(CLOSED_CANCELLED, CLOSED_PICKUP_EXPIRED);

  private static final Set<InventoryItemStatus> notAvailableItemStatuses = Set.of(
    LONG_MISSING, DECLARED_LOST, AGED_TO_LOST, LOST_AND_PAID, AWAITING_PICKUP, MISSING, PAGED, CLAIMED_RETURNED,
    WITHDRAWN, AWAITING_DELIVERY, IN_PROCESS_NON_REQUESTABLE, UNAVAILABLE, UNKNOWN);
  private static final Set<InventoryItemStatus> noOpenRequestsAvailableStatuses = Set.of(
    IN_TRANSIT, IN_PROCESS, ON_ORDER);

  public static final UUID INN_REACH_CANCELLATION_REASON_ID = UUID.fromString("941c7055-04f8-4db3-82cb-f63965c1506f");

  private final InnReachTransactionRepository transactionRepository;
  private final CentralPatronTypeMappingRepository centralPatronTypeMappingRepository;
  private final CentralServerRepository centralServerRepository;

  private final InnReachTransactionPickupLocationMapper transactionPickupLocationMapper;
  private final CirculationClient circulationClient;
  private final InventoryService inventoryService;
  private final UserService userService;

  private final InnReachExternalService innReachService;
  private final ItemService itemService;
  private final HoldingsService holdingsService;

  private final RequestPreferenceService requestPreferenceService;
  private final CentralServerService centralServerService;
  private final InstanceService instanceService;
  private final RecordContributionService recordContributionService;
  private final ItemStorageClient itemStorageClient;

  @Async
  @Override
  public void createItemHoldRequest(String trackingId, String centralCode) {
    log.debug("createItemHoldRequest:: parameters trackingId: {}, centralCode: {}", trackingId, centralCode);
    var transaction = fetchTransaction(trackingId, centralCode);
    var hold = transaction.getHold();
    var centralPatronName = hold.getPatronName();
    try {
      var centralServerId = getCentralServerId(transaction.getCentralServerCode());
      var patronType = hold.getCentralPatronType();
      var patronBarcode = getUserBarcode(centralServerId, patronType);
      var patron = getUserByBarcode(patronBarcode);
      var servicePointId = getItemHoldServicePointId(transaction, patron);

      createOwningSiteItemRequest(transaction, patron, servicePointId);
      log.info("createItemHoldRequest:: Item hold request created");
    } catch (Exception e) {
      handleOwningSiteRequestException(transaction, centralPatronName, e);
    }
  }

  @Async
  @Override
  public void createLocalHoldRequest(InnReachTransaction transaction) {
    log.debug("createLocalHoldRequest:: parameters transaction: {}", transaction);
    var hold = (TransactionLocalHold) transaction.getHold();
    var centralPatronName = hold.getPatronName();
    try {
      var patronId = UUIDEncoder.decode(hold.getPatronId());
      var patron = getUserById(patronId);
      var pickupLocationCode = hold.getPickupLocation().getPickupLocCode();
      var servicePointId = getServicePointIdByCode(pickupLocationCode);

      createOwningSiteItemRequest(transaction, patron, servicePointId);
      log.info("createLocalHoldRequest:: Local hold request created");
    } catch (Exception e) {
      handleOwningSiteRequestException(transaction, centralPatronName, e);
    }
  }

  @Override
  public void createItemRequest(InnReachTransaction transaction, Holding holding, InventoryItemDTO item,
                                User patron, UUID servicePointId, RequestType requestType) {
    log.info("Creating item request for transaction {}", transaction);
    var hold = transaction.getHold();

    //getting required data for a request
    var comment = createPatronComment(hold);

    var requestExpirationDate = getRequestExpirationDate(hold);

    //creating and sending new request
    var newRequest = RequestDTO.builder()
      .requestType(requestType.getName())
      .requestLevel(RequestDTO.RequestLevel.ITEM.getName())
      .instanceId(holding == null ? null : holding.getInstanceId())
      .holdingsRecordId(item.getHoldingsRecordId())
      .itemId(item.getId())
      .requesterId(patron.getId())
      .pickupServicePointId(servicePointId)
      .requestExpirationDate(requestExpirationDate)
      .patronComments(comment)
      .requestDate(transaction.getCreatedDate())
      .fulfillmentPreference(HOLD_SHELF.getName())
      .build();
    var createdRequest = circulationClient.sendRequest(newRequest);
    log.info("createdRequest {}", createdRequest.toString());
    updateTransaction(transaction, item, holding, createdRequest, patron);

    log.info("Item request successfully created.");
  }

  @Override
  public RequestDTO moveItemRequest(UUID requestId, InventoryItemDTO newItem) {
    var newItemId = newItem.getId();

    log.info("Moving request {} to a new item {}", requestId, newItemId);

    Assert.isTrue(requestId != null, "requestId can't be null");

    var payload = MoveRequestDTO.builder()
      .requestType(PAGE.getName())
      .destinationItemId(newItemId)
      .build();

    return circulationClient.moveRequest(requestId, payload);
  }

  @Override
  public void cancelRequest(String trackingId, UUID requestId, UUID reasonId, String reasonDetails) {
    log.info("Canceling item request for transaction with trackingId {}", trackingId);
    if (requestId == null) {
      log.warn("FOLIO requestId is not set for transaction with trackingId: {}", trackingId);
      return;
    }

    circulationClient.findRequest(requestId)
      .ifPresentOrElse(r -> cancelRequest(r, reasonId, reasonDetails),
        () -> log.warn("No request found with id {}", requestId));
  }

  private void createOwningSiteItemRequest(InnReachTransaction transaction, User patron, UUID servicePointId) {
    var hold = transaction.getHold();
    var item = itemService.getItemByHrId(hold.getItemId());
    var requestType = item.getStatus() == AVAILABLE ? PAGE : HOLD;
    var holding = holdingsService.find(item.getHoldingsRecordId()).orElse(null);

    validateItemAvailability(item);

    createItemRequest(transaction, holding, item, patron, servicePointId, requestType);
  }

  private void handleOwningSiteRequestException(InnReachTransaction transaction, String centralPatronName, Exception e) {
    log.warn("An error occurred during request processing", e);

    cancelTransaction(transaction);

    var errorReason = e instanceof ItemNotRequestableException ? "Item not available" : "Request not permitted";
    issueOwningSiteCancelsRequest(transaction, centralPatronName, errorReason);

    reContributeItem(transaction);
  }

  private void reContributeItem(InnReachTransaction transaction) {
    // re-contribute item to the central server in order to update back its status to AVAILABLE or
    // what was before the request
    log.info("re-contributing item for transaction with trackingId: {}", transaction.getTrackingId());
    var centralServerId = centralServerService.getCentralServerIdByCentralCode(transaction.getCentralServerCode());

    try {
      var item = fetchItem(transaction.getHold().getItemId());
      var bibId = holdingsService.find(item.getHoldingsRecordId())
        .map(Holding::getInstanceId)
        .flatMap(instanceService::find)
        .map(InventoryInstanceDTO::getHrid)
        .orElse(null);

      recordContributionService.contributeItems(centralServerId, bibId, List.of(item));
    } catch (Exception e) {
      log.error("Failed to re-contribute item for transaction with trackingId: {}. Error: {}",
        transaction.getTrackingId(), e.getMessage());
    }
  }

  private void cancelTransaction(InnReachTransaction transaction) {
    transaction.setState(CANCEL_REQUEST);
    transactionRepository.save(transaction);
  }

  private Item fetchItem(String itemHrId) {
    var resultList = itemStorageClient.getItemByHrId(itemHrId);
    return getFirstItem(resultList)
      .orElseThrow(() -> new IllegalArgumentException("Item with hrid = " + itemHrId + " not found."));
  }

  @Override
  public void createRecallRequest(UUID recallUserId, UUID itemId, UUID instanceId, UUID holdingId) {
    log.debug("createRecallRequest:: parameters recallUserId: {}, itemId: {}, instanceId: {}, holdingId: {}", recallUserId, itemId, instanceId, holdingId);
    var pickupServicePoint = getDefaultServicePointIdForPatron(recallUserId);

    var request = RequestDTO.builder()
      .itemId(itemId)
      .requesterId(recallUserId)
      .requestType(RECALL.getName())
      .requestLevel(RequestDTO.RequestLevel.ITEM.getName())
      .instanceId(instanceId)
      .holdingsRecordId(holdingId)
      .requestDate(OffsetDateTime.now())
      .fulfillmentPreference(HOLD_SHELF.getName())
      .pickupServicePointId(pickupServicePoint)
      .build();
    circulationClient.sendRequest(request);
    log.info("createRecallRequest:: Recall request created successfully");
  }

  @Override
  public RequestDTO findRequest(UUID requestId) {
    return circulationClient.findRequest(requestId).orElseThrow(() -> new EntityNotFoundException(
      "No request found with id = " + requestId));
  }

  @Override
  public void deleteRequest(UUID requestId) {
    circulationClient.findRequest(requestId)
      .ifPresentOrElse(requestDTO -> circulationClient.deleteRequest(requestId),
        () -> log.info("Request not present with requestId : {}", requestId));
    log.info("Request deleted with requestId:{}", requestId);
  }

  @Override
  public UUID getDefaultServicePointIdForPatron(UUID patronId) {
    return inventoryService.findDefaultServicePointIdForUser(patronId)
      .orElseThrow(() -> new CirculationException("Default service point is not set for the patron: " + patronId));
  }

  @Override
  public UUID getServicePointIdByCode(String locationCode) {
    return inventoryService.findServicePointIdByCode(locationCode)
      .orElseThrow(() -> new CirculationException("Service point is not found by location code: " + locationCode));
  }

  @Override
  public boolean isOpenRequest(RequestDTO request) {
    return openRequestStatuses.contains(request.getStatus());
  }

  @Override
  public boolean isCanceledOrExpired(RequestDTO request) {
    return canceledExpiredStatuses.contains(request.getStatus());
  }

  @Override
  public void validateItemAvailability(InventoryItemDTO item) {
    var requests = circulationClient.queryRequestsByItemId(item.getId());
    if (!isItemRequestable(item, requests)) {
      throw new ItemNotRequestableException("Item is not requestable: " + item.getId());
    }
  }

  @Override
  public ResultList<RequestDTO> getRequestsByItemId(UUID itemId) {
    return circulationClient.queryRequestsByItemId(itemId);
  }

  @Override
  public ResultList<RequestDTO> findNotFilledRequestsByIds(Set<UUID> requestIds, int limit) {
    return circulationClient.queryNotFilledRequestsByIds(matchAny(requestIds), limit);
  }

  private void cancelRequest(RequestDTO request, UUID reasonId, String reasonDetails) {
    request.setStatus(RequestStatus.CLOSED_CANCELLED);
    request.setCancellationReasonId(reasonId);
    request.setCancellationAdditionalInformation(reasonDetails);

    circulationClient.updateRequest(request.getId(), request);

    log.info("Item request successfully cancelled");
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
    if(transaction.getType() == ITEM) {
      var instance = instanceService.find(request.getInstanceId()).orElse(null);
      if (instance != null) {
        var author = instanceService.getAuthor(instance);
        hold.setAuthor(author);
      }
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

  private UUID getItemHoldServicePointId(InnReachTransaction transaction, User patron) {
    UUID servicePointId = null;
    var centralServer = centralServerService.getCentralServerByCentralCode(transaction.getCentralServerCode());
    if (centralServer.getCheckPickupLocation()) {
      var pickupLocationCode = transaction.getHold().getPickupLocation().getPickupLocCode();
      servicePointId = inventoryService.findServicePointIdByCode(pickupLocationCode).orElse(null);
    }

    return servicePointId != null ? servicePointId : getUserRequestPreferenceDefaultServicePoint(patron.getId());
  }

  private String queryByBarcode(String patronBarcode) {
    return "(barcode==\"" + patronBarcode + "\")";
  }

  private User getUserByBarcode(String patronBarcode) {
    return userService.getUserByQuery(queryByBarcode(patronBarcode)).orElseThrow(
      () -> new EntityNotFoundException("User not found for barcode = " + patronBarcode)
    );
  }

  private User getUserById(UUID patronId) {
    return userService.getUserById(patronId)
      .orElseThrow(() -> new IllegalArgumentException("Patron is not found by id: " + patronId));
  }

  private String getUserBarcode(UUID centralServerId, Integer patronType) {
    return centralPatronTypeMappingRepository.findOneByCentralServerIdAndCentralPatronType(centralServerId, patronType)
      .map(CentralPatronTypeMapping::getBarcode)
      .orElseThrow(() ->
        new EntityNotFoundException("User barcode not found for central server id = " + centralServerId +
          " and patron type = " + patronType));
  }

  private InnReachTransaction fetchTransaction(String trackingId, String centralCode) {
    return transactionRepository.findByTrackingIdAndCentralServerCode(trackingId, centralCode).orElseThrow(() ->
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


  private UUID getUserRequestPreferenceDefaultServicePoint(UUID patronId) {
    var requestPreference = requestPreferenceService.findUserRequestPreference(patronId);
    return requestPreference.getDefaultServicePointId();
  }
}
