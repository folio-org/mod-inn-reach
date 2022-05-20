package org.folio.innreach.domain.event;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Data;

import org.folio.innreach.domain.dto.folio.inventory.InventoryItemDTO;
import org.folio.innreach.domain.entity.InnReachTransaction;

@Data
@AllArgsConstructor
public class MoveRequestEvent {
  private UUID requestId;
  private InventoryItemDTO newItem;

  public static MoveRequestEvent of(InnReachTransaction transaction, InventoryItemDTO newItem) {
    return new MoveRequestEvent(transaction.getHold().getFolioRequestId(), newItem);
  }
}
