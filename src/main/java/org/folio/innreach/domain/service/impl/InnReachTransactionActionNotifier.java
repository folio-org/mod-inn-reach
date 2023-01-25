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
    log.debug("reportCheckOut:: parameters transaction: {}, localBibId: {}, itemBarcode: {}", transaction, localBibId, itemBarcode);
    var payload = new HashMap<>();
    payload.put("localBibId", localBibId);

    if (itemBarcode != null) {
      payload.put("itemBarcode", itemBarcode);
    }
    callD2irCircOperation(D2IR_LOCAL_CHECKOUT, transaction, payload);
    log.info("reportCheckOut:: Report checkout completed");
  }

  public void reportItemReceived(InnReachTransaction transaction) {
    callD2irCircOperation(D2IR_ITEM_RECEIVED_OPERATION, transaction, null);
  }

  public void reportCancelItemHold(InnReachTransaction transaction) {
    callD2irCircOperation(D2IR_CANCEL_ITEM_HOLD, transaction, null);
  }

  public void reportOwningSiteCancel(InnReachTransaction transaction, String localBibId, String patronName) {
    log.debug("reportOwningSiteCancel:: parameters transaction: {}, localBibId: {}, patronName: {}", transaction, localBibId, patronName);
    var payload = new HashMap<>();
    payload.put("localBibId", localBibId);
    payload.put("reasonCode", 7);
    payload.put("patronName", patronName);
    callD2irCircOperation(D2IR_OWNING_SITE_CANCEL, transaction, payload);
    log.info("reportOwningSiteCancel:: Report owning site cancelled");
  }

  public void reportRecallRequested(InnReachTransaction transaction, Instant loanDueDate) {
    log.debug("reportRecallRequested:: parameters transaction: {}, loanDueDate: {}", transaction, loanDueDate);
    var payload = new HashMap<>();
    payload.put("dueDateTime", loanDueDate.getEpochSecond());
    callD2irCircOperation(D2IR_RECALL, transaction, payload);
    log.info("reportRecallRequested:: Report recall request completed");
  }

  public void reportBorrowerRenew(InnReachTransaction transaction, Integer loanIntegerDueDate) {
    log.debug("reportBorrowerRenew:: parameters transaction: {}, loanIntegerDueDate: {}", transaction, loanIntegerDueDate);
    var payload = new HashMap<>();
    payload.put("dueDateTime", loanIntegerDueDate);
    callD2irCircOperation(D2IR_BORROWER_RENEW, transaction, payload);
    log.info("reportBorrowerRenew:: Report borrower renew completed");
  }

  public void reportFinalCheckIn(InnReachTransaction transaction) {
    callD2irCircOperation(D2IR_FINAL_CHECK_IN, transaction, null);
  }

  public void reportItemShipped(InnReachTransaction transaction, String itemBarcode, String callNumber) {
    log.debug("reportItemShipped:: parameters transaction: {}, itemBarcode: {}, callNumber: {}", transaction, itemBarcode, callNumber);
    var payload = new HashMap<>();
    payload.put("itemBarcode", itemBarcode);
    payload.put("callNumber", callNumber);

    callD2irCircOperation(D2IR_ITEM_SHIPPED_OPERATION, transaction, payload);
    log.info("reportItemShipped:: Report item shipped");
  }

  public void reportUnshippedItemReceived(InnReachTransaction transaction) {
    callD2irCircOperation(D2IR_RECEIVE_UNSHIPPED_OPERATION, transaction, null);
  }

  public void reportItemInTransit(InnReachTransaction transaction) {
    callD2irCircOperation(D2IR_IN_TRANSIT, transaction, null);
  }

  public void reportTransferRequest(InnReachTransaction transaction, String hrid) {
    log.debug("reportTransferRequest:: parameters transaction: {}, hrid: {}", transaction, hrid);
    var payload = new HashMap<>();
    payload.put("newItemId", hrid);
    callD2irCircOperation(D2IR_TRASFER_REQUEST, transaction, payload);
    log.info("reportTransferRequest:: Report transfer request completed");
  }

  public void reportReturnUncirculated(InnReachTransaction transaction) {
    callD2irCircOperation(D2IR_RETURN_UNCIRCULATED, transaction, null);
  }

  public void reportClaimsReturned(InnReachTransaction transaction, Integer claimsReturnedDateSec) {
    log.debug("reportClaimsReturned:: parameters transaction: {}, claimsReturnedDateSec: {}", transaction, claimsReturnedDateSec);
    var payload = new HashMap<>();
    payload.put("claimsReturnedDate", claimsReturnedDateSec);
    callD2irCircOperation(D2IR_CLAIMS_RETURNED, transaction, payload);
    log.info("reportClaimsReturned:: Report claims returned");
  }

  private void callD2irCircOperation(String operation, InnReachTransaction transaction, Map<Object, Object> payload) {
    log.debug("callD2irCircOperation:: parameters operation: {}, transaction: {}, payload: {}", operation, transaction, payload);
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
