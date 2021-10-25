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
import org.folio.innreach.external.service.InventoryService;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import org.folio.innreach.client.ServicePointsUsersClient;
import org.folio.innreach.client.RequestStorageClient;
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
import org.folio.innreach.domain.service.RequestService;
import org.folio.innreach.external.service.InnReachExternalService;
import org.folio.innreach.mapper.InnReachTransactionMapper;
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

  private final InnReachTransactionMapper transactionMapper;

  private final RequestStorageClient requestsClient;
  private final ServicePointsUsersClient servicePointsUsersClient;
  private final UsersClient usersClient;

  private final InnReachExternalService innReachService;
  private final InventoryService inventoryService;

  @Async
  @Override
  public void createItemRequest(String trackingId) {
    log.info("Creating an item request...");
    var transaction = transactionRepository.fetchOneByTrackingId(trackingId).orElseThrow(() ->
      new EntityNotFoundException("Transaction not found for trackingId = " + trackingId)
    );
    var itemHrId = transaction.getHold().getItemId();
    var centralServerId = getCentralServerId(transaction.getCentralServerCode());

    var item = inventoryService.getItemByHrId(itemHrId);
    var requests = requestsClient.findRequests(item.getId());

    if (itemIsRequestable(item, requests)) {
      try {
        //getting required data for a request
        var comment = createPatronComment(transaction.getHold());
        var patronType = ((TransactionItemHold) transaction.getHold()).getCentralPatronType();
        var patronBarcode = getUserBarcode(centralServerId, patronType);
        var userId = getUserByBarcode(patronBarcode).getId();
        var defaultServicePointId = getDefaultServicePointId(userId);
        var requestExpirationDate = getRequestExpirationDate(transaction.getHold());
        var requestType = item.getStatus() == AVAILABLE ? PAGE.getName() : HOLD.getName();

        //creating and sending new request
        var newRequest = RequestDTO.builder()
          .requestType(requestType)
          .itemId(item.getId())
          .requesterId(UUID.fromString(userId))
          .pickupServicePointId(defaultServicePointId)
          .requestExpirationDate(requestExpirationDate)
          .patronComments(comment)
          .requestDate(transaction.getCreatedDate())
          .fulfilmentPreference(HOLD_SHELF.getName())
          .build();
        var createdRequest = requestsClient.sendRequest(newRequest);

        //updating transaction data
        transaction.getHold().setFolioRequestId(createdRequest.getId());
        transaction.getHold().setFolioPatronId(UUID.fromString(userId));
        transaction.getHold().setFolioItemId(item.getId());
        transactionRepository.save(transaction);

        log.info("Item request successfully created.");
      } catch (Exception e) {
        log.warn("An error occurred during request processing. Sending \"Owning site cancels\" request.", e);
        var errorReason = "Request not permitted";
        issueOwningSiteCancelsRequest(errorReason, transaction, centralServerId);
      }
    } else {
      log.warn("Requested item is not available. Sending \"Owning site cancels\" request.");
      var errorReason = "Item not available";
      issueOwningSiteCancelsRequest(errorReason, transaction, centralServerId);
    }
  }

  private boolean itemIsRequestable(InventoryItemDTO item, ResultList<RequestDTO> requests) {
    return item.getStatus() == AVAILABLE ||
      !notAvailableItemStatuses.contains(item.getStatus()) ||
      noOpenRequests(requests) && noOpenRequestsAvailableStatuses.contains(item.getStatus());
  }

  private boolean noOpenRequests(ResultList<RequestDTO> requests) {
    return requests.getResult().stream().noneMatch(r -> openRequestStatuses.contains(r.getStatus()));
  }

  private String createPatronComment(TransactionHold hold) {
    return "INN-Reach request: Patron Agency - " + hold.getPatronAgencyCode() +
      ", Pickup Location - " + transactionMapper.map(hold.getPickupLocation());
  }

  private UUID getCentralServerId(String centralServerCode) {
    return centralServerRepository.fetchOneByCentralCode(centralServerCode)
      .orElseThrow(() -> new EntityNotFoundException("Central server not found for central code = " + centralServerCode)
      ).getId();
  }

  private UUID getDefaultServicePointId(String userId) {
    return servicePointsUsersClient.findServicePointsUsers(UUID.fromString(userId))
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
