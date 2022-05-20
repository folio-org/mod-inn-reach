package org.folio.innreach.domain.event;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Data;

import org.folio.innreach.domain.entity.InnReachRecallUser;
import org.folio.innreach.domain.entity.TransactionHold;

@AllArgsConstructor
@Data
public class RecallRequestEvent {
  private UUID recallUserId;
  private UUID itemId;
  private UUID instanceId;
  private UUID holdingId;

  public static RecallRequestEvent of(TransactionHold hold, InnReachRecallUser recallUser) {
    return new RecallRequestEvent(recallUser.getUserId(), hold.getFolioItemId(), hold.getFolioInstanceId(), hold.getFolioHoldingId());
  }
}
