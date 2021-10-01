package org.folio.innreach.domain.service.impl;

import lombok.RequiredArgsConstructor;
import org.folio.innreach.client.InventoryClient;
import org.folio.innreach.client.InventoryStorageClient;
import org.folio.innreach.client.RequestStorageClient;
import org.folio.innreach.client.UsersClient;
import org.folio.innreach.domain.dto.folio.inventory.InventoryItemDTO;
import org.folio.innreach.domain.dto.folio.inventory.InventoryItemStatus;
import org.folio.innreach.domain.dto.folio.requeststorage.RequestDTO;
import org.folio.innreach.domain.dto.folio.requeststorage.RequestsDTO;
import org.folio.innreach.domain.entity.InnReachTransaction;
import org.folio.innreach.domain.entity.InnReachTransaction.TransactionType;
import org.folio.innreach.domain.entity.TransactionItemHold;
import org.folio.innreach.domain.exception.EntityNotFoundException;
import org.folio.innreach.domain.service.CentralServerService;
import org.folio.innreach.domain.service.InnReachTransactionService;
import org.folio.innreach.dto.InnReachResponseDTO;
import org.folio.innreach.dto.TransactionItemHoldDTO;
import org.folio.innreach.mapper.InnReachTransactionMapper;
import org.folio.innreach.repository.CentralPatronTypeMappingRepository;
import org.folio.innreach.repository.CentralServerRepository;
import org.folio.innreach.repository.InnReachTransactionRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Set;
import java.util.UUID;

import static org.folio.innreach.domain.dto.folio.inventory.InventoryItemStatus.AVAILABLE;
import static org.folio.innreach.domain.dto.folio.inventory.InventoryItemStatus.AWAITING_DELIVERY;
import static org.folio.innreach.domain.dto.folio.inventory.InventoryItemStatus.AWAITING_PICKUP;
import static org.folio.innreach.domain.dto.folio.inventory.InventoryItemStatus.CHECKED_OUT;
import static org.folio.innreach.domain.dto.folio.inventory.InventoryItemStatus.DECLARED_LOST;
import static org.folio.innreach.domain.dto.folio.inventory.InventoryItemStatus.IN_PROCESS;
import static org.folio.innreach.domain.dto.folio.inventory.InventoryItemStatus.IN_TRANSIT;
import static org.folio.innreach.domain.dto.folio.inventory.InventoryItemStatus.PAGED;
import static org.folio.innreach.domain.dto.folio.inventory.InventoryItemStatus.WITHDRAWN;
import static org.folio.innreach.domain.dto.folio.requeststorage.RequestDTO.RequestStatus;
import static org.folio.innreach.domain.dto.folio.requeststorage.RequestDTO.RequestStatus.OPEN_AWAITING_DELIVERY;
import static org.folio.innreach.domain.dto.folio.requeststorage.RequestDTO.RequestStatus.OPEN_AWAITING_PICKUP;
import static org.folio.innreach.domain.dto.folio.requeststorage.RequestDTO.RequestStatus.OPEN_IN_TRANSIT;
import static org.folio.innreach.domain.dto.folio.requeststorage.RequestDTO.RequestStatus.OPEN_NOT_YET_FILLED;
import static org.folio.innreach.domain.dto.folio.requeststorage.RequestDTO.RequestType.HOLD;
import static org.folio.innreach.domain.dto.folio.requeststorage.RequestDTO.RequestType.PAGE;
import static org.folio.innreach.domain.dto.folio.requeststorage.RequestDTO.FulfilmentPreference.HOLD_SHELF;

@RequiredArgsConstructor
@Service
public class InnReachTransactionServiceImpl implements InnReachTransactionService {

  private final InnReachTransactionRepository repository;
  private final CentralPatronTypeMappingRepository centralPatronTypeMappingRepository;
  private final CentralServerRepository centralServerRepository;
  private final InnReachTransactionMapper mapper;
  private final CentralServerService centralServerService;

  private final InventoryClient inventoryClient;
  private final RequestStorageClient requestsClient;
  private final InventoryStorageClient inventoryStorageClient;
  private final UsersClient usersClient;

  private static final Set<RequestStatus> openRequestStatuses = Set.of(OPEN_AWAITING_PICKUP, OPEN_AWAITING_DELIVERY,
    OPEN_IN_TRANSIT, OPEN_NOT_YET_FILLED);
  private static final Set<InventoryItemStatus> availableItemStatuses = Set.of(
    AVAILABLE, CHECKED_OUT, PAGED, IN_TRANSIT, DECLARED_LOST, WITHDRAWN, AWAITING_PICKUP, AWAITING_DELIVERY);

  private InnReachTransaction createTransactionWithItemHold(String trackingId, String centralCode) {
    InnReachTransaction transaction = new InnReachTransaction();
    transaction.setTrackingId(trackingId);
    transaction.setCentralServerCode(centralCode);
    transaction.setType(TransactionType.ITEM);
    transaction.setState(InnReachTransaction.TransactionState.ITEM_HOLD);
    return transaction;
  }

  @Override
  public InnReachResponseDTO createInnReachTransactionItemHold(String trackingId, String centralCode, TransactionItemHoldDTO dto) {
    var response = new InnReachResponseDTO();
    response.setStatus("ok");
    try {
      centralServerService.getCentralServerByCentralCode(centralCode);
      var transaction = createTransactionWithItemHold(trackingId, centralCode);
      var itemHold = mapper.toItemHold(dto);
      transaction.setHold(itemHold);
      repository.save(transaction);
    } catch (Exception e) {
      response.setStatus("failed");
      response.setReason(e.getMessage());
    }
    return response;
  }

  @Override
  public void createItemRequest(String trackingId) {
    var transaction = repository.fetchOneByTrackingId(trackingId).orElseThrow(() ->
      new EntityNotFoundException("Transaction with trackingId = " + trackingId + " not found.")
    );
    var itemId = transaction.getHold().getItemId();
    String errorReason;

    var item = inventoryClient.getItemById(itemId);
    var requests = requestsClient.findRequests(itemId);

    if (itemIsRequestable(item, requests)) {
      try {
        var comment = "INN-Reach request: Patron Agency - " + transaction.getHold().getPatronAgencyCode() +
          ", Pickup Location - " + mapper.map(transaction.getHold().getPickupLocation());
        var centralServerId = centralServerRepository.fetchOneByCentralCode(transaction.getCentralServerCode())
          .orElseThrow(() ->
            new EntityNotFoundException("Central server with central code = " + transaction.getCentralServerCode() + " not found.")
          ).getId();
        var patronType = ((TransactionItemHold) transaction.getHold()).getCentralPatronType();
        var patronBarcode = centralPatronTypeMappingRepository.
          findOneByCentralServerIdAndCentralPatronType(centralServerId, patronType);
        var user = usersClient.query("(barcode==\"" + patronBarcode + "\")").getResult().get(0);
        var defaultServicePointId = inventoryStorageClient.findServicePointsUsers(UUID.fromString(user.getId()))
          .getServicePointsUsers().get(0).getDefaultServicePointId();
        var requestExpirationDate = transaction.getHold().getNeedBefore() == null ? null :
          OffsetDateTime.ofInstant(Instant.ofEpochMilli(transaction.getHold().getNeedBefore()), ZoneOffset.UTC);

        var newRequest = RequestDTO.builder()
          .requestType(item.getStatus() == AVAILABLE ? PAGE : HOLD)
          .itemId(itemId)
          .requesterId(UUID.fromString(user.getId()))
          .pickupServicePointId(defaultServicePointId)
          .requestExpirationDate(requestExpirationDate)
          .patronComments(comment)
          .requestDate(transaction.getCreatedDate())
          .fulfilmentPreference(HOLD_SHELF)
          .build();
        var createdRequest = requestsClient.sendRequest(newRequest);

        transaction.getHold().setFolioRequestId(createdRequest.getId());
        transaction.getHold().setFolioPatronId(UUID.fromString(user.getId()));
        transaction.getHold().setFolioItemId(itemId);
        repository.save(transaction);
      } catch (Exception e) {
        errorReason = "Request not permitted";
        //issue a "Owning site cancels request" call to the central server
      }
    } else {
      errorReason = "Item not available";
      //issue a "Owning site cancels request" call to the central server
    }
  }

  private boolean itemIsRequestable(InventoryItemDTO item, RequestsDTO requests) {
    return item.getStatus() == AVAILABLE ||
      item.getStatus() == IN_TRANSIT && noOpenRequests(requests) ||
      item.getStatus() == IN_PROCESS && noOpenRequests(requests) ||
      availableItemStatuses.contains(item.getStatus());
  }

  private boolean noOpenRequests(RequestsDTO requests) {
    return requests.getRequests().stream().noneMatch(r -> openRequestStatuses.contains(r.getStatus()));
  }
}
