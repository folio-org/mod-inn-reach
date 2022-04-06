package org.folio.innreach.domain.event;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Data;

import org.folio.innreach.domain.entity.InnReachTransaction;


@AllArgsConstructor
@Data
public class CancelRequestEvent {
  private String transactionTrackingId;
  private UUID requestId;
  private UUID cancellationReasonId;
  private String details;

  public static CancelRequestEvent of(InnReachTransaction transaction, UUID reasonId, String details) {
    return new CancelRequestEvent(transaction.getTrackingId(), transaction.getHold().getFolioRequestId(), reasonId, details);
  }
}
