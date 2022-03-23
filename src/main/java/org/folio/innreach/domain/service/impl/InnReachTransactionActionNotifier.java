package org.folio.innreach.domain.service.impl;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

import org.springframework.stereotype.Component;

import org.folio.innreach.domain.entity.InnReachTransaction;
import org.folio.innreach.external.exception.InnReachException;
import org.folio.innreach.external.service.InnReachExternalService;

@Log4j2
@RequiredArgsConstructor
@Component
public class InnReachTransactionActionNotifier {

  private static final String D2IR_ITEM_RECEIVED_OPERATION = "itemreceived";
  private static final String D2IR_ITEM_SHIPPED_OPERATION = "itemshipped";
  private static final String D2IR_RECEIVE_UNSHIPPED_OPERATION = "receiveunshipped";
  private static final String D2IR_IN_TRANSIT = "intransit";
  private static final String D2IR_BORROWER_RENEW = "borrowerrenew";
  private static final String D2IR_FINAL_CHECK_IN = "finalcheckin";
  private static final String D2IR_TRASFER_REQUEST = "transferrequest";
  private static final String D2IR_RETURN_UNCIRCULATED = "returnuncirculated";
  private static final String D2IR_OWNING_SITE_CANCEL = "owningsitecancel";
  private static final String D2IR_CLAIMS_RETURNED = "claimsreturned";
  private static final String D2IR_CANCEL_ITEM_HOLD = "cancelitemhold";
  private static final String D2IR_RECALL = "recall";
  private static final String D2IR_LOCAL_CHECKOUT = "localcheckout";

  private final InnReachExternalService innReachExternalService;

  public void reportCheckOut(InnReachTransaction transaction, String localBibId, String itemBarcode) {
    var payload = new HashMap<>();
    payload.put("localBibId", localBibId);

    if (itemBarcode != null) {
      payload.put("itemBarcode", itemBarcode);
    }
    callD2irCircOperation(D2IR_LOCAL_CHECKOUT, transaction, payload);
  }

  public void reportItemReceived(InnReachTransaction transaction) {
    callD2irCircOperation(D2IR_ITEM_RECEIVED_OPERATION, transaction, null);
  }

  public void reportCancelItemHold(InnReachTransaction transaction) {
    callD2irCircOperation(D2IR_CANCEL_ITEM_HOLD, transaction, null);
  }

  public void reportOwningSiteCancel(InnReachTransaction transaction, String localBibId, String patronName) {
    var payload = new HashMap<>();
    payload.put("localBibId", localBibId);
    payload.put("reasonCode", 7);
    payload.put("patronName", patronName);
    callD2irCircOperation(D2IR_OWNING_SITE_CANCEL, transaction, payload);
  }

  public void reportRecallRequested(InnReachTransaction transaction, Instant loanDueDate) {
    var payload = new HashMap<>();
    payload.put("dueDateTime", loanDueDate.getEpochSecond());
    callD2irCircOperation(D2IR_RECALL, transaction, payload);
  }

  public void reportBorrowerRenew(InnReachTransaction transaction, Integer loanIntegerDueDate) {
    var payload = new HashMap<>();
    payload.put("dueDateTime", loanIntegerDueDate);
    callD2irCircOperation(D2IR_BORROWER_RENEW, transaction, payload);
  }

  public void reportFinalCheckIn(InnReachTransaction transaction) {
    callD2irCircOperation(D2IR_FINAL_CHECK_IN, transaction, null);
  }

  public void reportItemShipped(InnReachTransaction transaction, String itemBarcode, String callNumber) {
    var payload = new HashMap<>();
    payload.put("itemBarcode", itemBarcode);
    payload.put("callNumber", callNumber);

    callD2irCircOperation(D2IR_ITEM_SHIPPED_OPERATION, transaction, payload);
  }

  public void reportUnshippedItemReceived(InnReachTransaction transaction) {
    callD2irCircOperation(D2IR_RECEIVE_UNSHIPPED_OPERATION, transaction, null);
  }

  public void reportItemInTransit(InnReachTransaction transaction) {
    callD2irCircOperation(D2IR_IN_TRANSIT, transaction, null);
  }

  public void reportTransferRequest(InnReachTransaction transaction, String hrid) {
    var payload = new HashMap<>();
    payload.put("newItemId", hrid);
    callD2irCircOperation(D2IR_TRASFER_REQUEST, transaction, payload);
  }

  public void reportReturnUncirculated(InnReachTransaction transaction) {
    callD2irCircOperation(D2IR_RETURN_UNCIRCULATED, transaction, null);
  }

  public void reportClaimsReturned(InnReachTransaction transaction, Integer claimsReturnedDateSec) {
    var payload = new HashMap<>();
    payload.put("claimsReturnedDate", claimsReturnedDateSec);
    callD2irCircOperation(D2IR_CLAIMS_RETURNED, transaction, payload);
  }

  private void callD2irCircOperation(String operation, InnReachTransaction transaction, Map<Object, Object> payload) {
    var centralCode = transaction.getCentralServerCode();
    var trackingId = transaction.getTrackingId();
    var requestPath = resolveD2irCircPath(operation, trackingId, centralCode);
    try {
      if (payload == null) {
        innReachExternalService.postInnReachApi(centralCode, requestPath);
      } else {
        innReachExternalService.postInnReachApi(centralCode, requestPath, payload);
      }
    } catch (InnReachException e) {
      //TODO: the suppression of error is temporal, see https://issues.folio.org/browse/MODINREACH-192 for more details.
      log.warn("Unexpected D2IR response: {}", e.getMessage(), e);
    }
  }

  private String resolveD2irCircPath(String operation, String trackingId, String centralCode) {
    return String.format("/circ/%s/%s/%s", operation, trackingId, centralCode);
  }

}