package org.folio.innreach.domain.service;

import java.util.UUID;

import org.springframework.scheduling.annotation.Async;

import org.folio.innreach.domain.dto.folio.User;
import org.folio.innreach.domain.dto.folio.circulation.RequestDTO.RequestType;
import org.folio.innreach.domain.dto.folio.inventory.InventoryItemDTO;
import org.folio.innreach.domain.entity.InnReachTransaction;
import org.folio.innreach.dto.CheckInResponseDTO;
import org.folio.innreach.dto.Holding;

public interface RequestService {
  @Async
  void createItemHoldRequest(String transactionTrackingId);

  void createItemRequest(InnReachTransaction transaction, Holding holding, InventoryItemDTO item,
                         User patron, UUID servicePointId, RequestType requestType);

  void moveItemRequest(InnReachTransaction transaction, Holding holding, InventoryItemDTO item);

  void cancelRequest(InnReachTransaction transaction, String reason);

  CheckInResponseDTO checkInItem(InnReachTransaction transaction, UUID servicePointId);
}
