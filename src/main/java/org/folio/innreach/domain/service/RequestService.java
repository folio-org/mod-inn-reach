package org.folio.innreach.domain.service;

import java.util.UUID;

import org.springframework.scheduling.annotation.Async;

import org.folio.innreach.domain.dto.folio.User;
import org.folio.innreach.domain.dto.folio.circulation.RequestDTO;
import org.folio.innreach.domain.dto.folio.circulation.RequestDTO.RequestType;
import org.folio.innreach.domain.dto.folio.inventory.InventoryItemDTO;
import org.folio.innreach.domain.entity.InnReachTransaction;
import org.folio.innreach.dto.Holding;

public interface RequestService {
  @Async
  void createItemHoldRequest(String trackingId, String centralCode);

  @Async
  void createLocalHoldRequest(InnReachTransaction transaction);

  void createItemRequest(InnReachTransaction transaction, Holding holding, InventoryItemDTO item,
                         User patron, UUID servicePointId, RequestType requestType);

  RequestDTO moveItemRequest(UUID requestId, InventoryItemDTO newItem);

  void cancelRequest(String trackingId, UUID requestId, UUID reasonId, String reason);

  void createRecallRequest(UUID recallUserId, UUID itemId, UUID instanceId, UUID holdingId);

  RequestDTO findRequest(UUID requestId);

  UUID getDefaultServicePointIdForPatron(UUID patronId);

  UUID getServicePointIdByCode(String locationCode);

  boolean isOpenRequest(RequestDTO request);

  boolean isCanceledOrExpired(RequestDTO request);

  void validateItemAvailability(InventoryItemDTO item);
}
