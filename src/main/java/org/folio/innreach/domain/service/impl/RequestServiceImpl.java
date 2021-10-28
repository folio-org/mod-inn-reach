package org.folio.innreach.domain.service.impl;

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
import static org.folio.innreach.domain.dto.folio.requeststorage.RequestDTO.FulfilmentPreference.HOLD_SHELF;
import static org.folio.innreach.domain.dto.folio.requeststorage.RequestDTO.RequestStatus.OPEN_AWAITING_DELIVERY;
import static org.folio.innreach.domain.dto.folio.requeststorage.RequestDTO.RequestStatus.OPEN_AWAITING_PICKUP;
import static org.folio.innreach.domain.dto.folio.requeststorage.RequestDTO.RequestStatus.OPEN_IN_TRANSIT;
import static org.folio.innreach.domain.dto.folio.requeststorage.RequestDTO.RequestStatus.OPEN_NOT_YET_FILLED;
import static org.folio.innreach.domain.dto.folio.requeststorage.RequestDTO.RequestType.HOLD;
import static org.folio.innreach.domain.dto.folio.requeststorage.RequestDTO.RequestType.PAGE;

import java.net.URI;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Set;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import org.folio.innreach.client.CirculationClient;
import org.folio.innreach.client.RequestStorageClient;
import org.folio.innreach.client.ServicePointsUsersClient;
import org.folio.innreach.client.UsersClient;
import org.folio.innreach.domain.dto.OwningSiteCancelsRequestDTO;
import org.folio.innreach.domain.dto.folio.ResultList;
import org.folio.innreach.domain.dto.folio.User;
import org.folio.innreach.domain.dto.folio.inventory.InventoryItemDTO;
import org.folio.innreach.domain.dto.folio.inventory.InventoryItemStatus;
import org.folio.innreach.domain.dto.folio.requeststorage.RequestDTO;
import org.folio.innreach.domain.dto.folio.requeststorage.RequestDTO.RequestStatus;
import org.folio.innreach.domain.entity.InnReachTransaction;
import org.folio.innreach.domain.entity.TransactionHold;
import org.folio.innreach.domain.entity.TransactionItemHold;
import org.folio.innreach.domain.exception.EntityNotFoundException;
import org.folio.innreach.domain.exception.ItemNotRequestableException;
import org.folio.innreach.domain.service.FolioCirculationService;
import org.folio.innreach.domain.service.RequestService;
import org.folio.innreach.external.service.InnReachExternalService;
import org.folio.innreach.external.service.InventoryService;
import org.folio.innreach.mapper.InnReachTransactionPickupLocationMapper;
import org.folio.innreach.repository.CentralPatronTypeMappingRepository;
import org.folio.innreach.repository.CentralServerRepository;
import org.folio.innreach.repository.InnReachTransactionRepository;

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

  private final InnReachTransactionRepository transactionRepository;
  private final CentralPatronTypeMappingRepository centralPatronTypeMappingRepository;
  private final CentralServerRepository centralServerRepository;

  private final InnReachTransactionPickupLocationMapper transactionPickupLocationMapper;
  private final RequestStorageClient requestsClient;
  private final FolioCirculationService circulationClient;
  private final ServicePointsUsersClient servicePointsUsersClient;
  private final UsersClient usersClient;

  private final InnReachExternalService innReachService;
  private final InventoryService inventoryService;

  @Async
  @Override
  public void createItemRequest(String trackingId) {
    var transaction = transactionRepository.fetchOneByTrackingId(trackingId).orElseThrow(() ->
      new EntityNotFoundException("Transaction not found for trackingId = " + trackingId)
    );
    var centralServerId = getCentralServerId(transaction.getCentralServerCode());

    try {
      var patronType = ((TransactionItemHold) transaction.getHold()).getCentralPatronType();
      var patronBarcode = getUserBarcode(centralServerId, patronType);
      var userId = UUID.fromString(getUserByBarcode(patronBarcode).getId());
      var servicePointId = getDefaultServicePointId(userId);

      createItemRequest(transaction, centralServerId, servicePointId, userId);
    } catch (ItemNotRequestableException e) {
      log.warn("Requested item is not available. Sending \"Owning site cancels\" request.");
      var errorReason = "Item not available";
      issueOwningSiteCancelsRequest(errorReason, transaction, centralServerId);
    } catch (Exception e) {
      log.warn("An error occurred during request processing. Sending \"Owning site cancels\" request.", e);
      var errorReason = "Request not permitted";
      issueOwningSiteCancelsRequest(errorReason, transaction, centralServerId);
    }
  }

  @Override
  public void createItemRequest(InnReachTransaction transaction, UUID centralServerId, UUID servicePointId, UUID requesterId) {
    log.info("Creating item request for transaction {}", transaction);
    var itemHrId = transaction.getHold().getItemId();

    var item = inventoryService.getItemByHrId(itemHrId);

    var requests = requestsClient.findRequests(item.getId());
    if (!isItemRequestable(item, requests)) {
      throw new ItemNotRequestableException("Requested item is not available");
    }

    //getting required data for a request
    var comment = createPatronComment(transaction.getHold());

    var requestExpirationDate = getRequestExpirationDate(transaction.getHold());
    var requestType = item.getStatus() == AVAILABLE ? PAGE : HOLD;

    //creating and sending new request
    var newRequest = RequestDTO.builder()
      .requestType(requestType.getName())
      .itemId(item.getId())
      .requesterId(requesterId)
      .pickupServicePointId(servicePointId)
      .requestExpirationDate(requestExpirationDate)
      .patronComments(comment)
      .requestDate(transaction.getCreatedDate())
      .fulfilmentPreference(HOLD_SHELF.getName())
      .build();
    var createdRequest = requestsClient.sendRequest(newRequest);

    //updating transaction data
    transaction.getHold().setFolioRequestId(createdRequest.getId());
    transaction.getHold().setFolioPatronId(requesterId);
    transaction.getHold().setFolioItemId(item.getId());
    transactionRepository.save(transaction);

    log.info("Item request successfully created.");
  }

  @Override
  public void moveItemRequest(InnReachTransaction transaction) {
    log.info("Moving item request for transaction {}", transaction);

    var hold = transaction.getHold();
    var itemHrId = hold.getItemId();
    var requestId = hold.getFolioRequestId();

    var item = inventoryService.getItemByHrId(itemHrId);

    var requests = requestsClient.findRequests(item.getId());
    if (!isItemRequestable(item, requests)) {
      throw new ItemNotRequestableException("Item with hrid " + itemHrId + " is not requestable");
    }

    var payload = CirculationClient.MoveRequest.builder()
      .requestType(PAGE.getName())
      .destinationItemId(item.getId())
      .build();

    var movedRequest = circulationClient.moveRequest(requestId, payload);

    hold.setFolioItemId(item.getId());
    hold.setFolioRequestId(movedRequest.getId());
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

  private String getUserBarcode(UUID centralServerId, Integer patronType) {
    return centralPatronTypeMappingRepository.
      findOneByCentralServerIdAndCentralPatronType(centralServerId, patronType).orElseThrow(() ->
        new EntityNotFoundException("User barcode not found for central server id = " + centralServerId +
          " and patron type = " + patronType)
      ).getBarcode();
  }

  private OffsetDateTime getRequestExpirationDate(TransactionHold hold) {
    return hold.getNeedBefore() == null ? null :
      OffsetDateTime.ofInstant(Instant.ofEpochMilli(hold.getNeedBefore()), ZoneOffset.UTC);
  }

  private URI createOwningSiteCancelsRequestUri(String trackingId, String centralServerCode) {
    return URI.create(OWNING_SITE_CANCELS_REQUEST_BASE_URI + trackingId + "/" + centralServerCode);
  }

  private void issueOwningSiteCancelsRequest(String reason, InnReachTransaction transaction, UUID centralServerId) {
    var request = OwningSiteCancelsRequestDTO.builder()
      .reason(reason)
      .reasonCode(7)
      .localBibId(transaction.getHold().getItemId())
      .patronName(((TransactionItemHold) transaction.getHold()).getPatronName())
      .build();
    innReachService.postInnReachApi(
      centralServerId, createOwningSiteCancelsRequestUri(transaction.getTrackingId(), transaction.getCentralServerCode()),
      request);
  }
}
