package org.folio.innreach.domain.listener;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionalEventListener;

import org.folio.innreach.domain.event.CancelRequestEvent;
import org.folio.innreach.domain.event.RecallRequestEvent;
import org.folio.innreach.domain.service.RequestService;

@Service
@RequiredArgsConstructor
public class ApplicationEventListener {

  private final RequestService requestService;

  @TransactionalEventListener
  public void handleCancelRequestEvent(CancelRequestEvent event) {
    var trackingId = event.getTransactionTrackingId();
    var requestId = event.getRequestId();
    var reasonId = event.getCancellationReasonId();
    var details = event.getDetails();

    requestService.cancelRequest(trackingId, requestId, reasonId, details);
  }

  @TransactionalEventListener
  public void handleRecallRequestEvent(RecallRequestEvent event) {
    var recallUserId = event.getRecallUserId();
    var itemId = event.getItemId();
    var instanceId = event.getInstanceId();
    var holdingId = event.getHoldingId();

    requestService.createRecallRequest(recallUserId, itemId, instanceId, holdingId);
  }

}
