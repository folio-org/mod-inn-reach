package org.folio.innreach.util;

import lombok.experimental.UtilityClass;

import org.apache.commons.lang3.ArrayUtils;
import org.springframework.util.Assert;

import org.folio.innreach.domain.entity.InnReachTransaction;
import org.folio.innreach.domain.entity.TransactionHold;

import java.util.List;

import static org.folio.innreach.domain.entity.InnReachTransaction.TransactionState.BORROWER_RENEW;
import static org.folio.innreach.domain.entity.InnReachTransaction.TransactionState.BORROWING_SITE_CANCEL;
import static org.folio.innreach.domain.entity.InnReachTransaction.TransactionState.CANCEL_REQUEST;
import static org.folio.innreach.domain.entity.InnReachTransaction.TransactionState.CLAIMS_RETURNED;
import static org.folio.innreach.domain.entity.InnReachTransaction.TransactionState.FINAL_CHECKIN;
import static org.folio.innreach.domain.entity.InnReachTransaction.TransactionState.ITEM_HOLD;
import static org.folio.innreach.domain.entity.InnReachTransaction.TransactionState.ITEM_IN_TRANSIT;
import static org.folio.innreach.domain.entity.InnReachTransaction.TransactionState.ITEM_RECEIVED;
import static org.folio.innreach.domain.entity.InnReachTransaction.TransactionState.ITEM_SHIPPED;
import static org.folio.innreach.domain.entity.InnReachTransaction.TransactionState.LOCAL_CHECKOUT;
import static org.folio.innreach.domain.entity.InnReachTransaction.TransactionState.LOCAL_HOLD;
import static org.folio.innreach.domain.entity.InnReachTransaction.TransactionState.OWNER_RENEW;
import static org.folio.innreach.domain.entity.InnReachTransaction.TransactionState.RECALL;
import static org.folio.innreach.domain.entity.InnReachTransaction.TransactionState.RECEIVE_UNANNOUNCED;
import static org.folio.innreach.domain.entity.InnReachTransaction.TransactionState.RETURN_UNCIRCULATED;

@UtilityClass
public class InnReachTransactionUtils {

  public static void verifyState(InnReachTransaction transaction, InnReachTransaction.TransactionState... expectedStates) {
    var actualState = transaction.getState();
    Assert.isTrue(ArrayUtils.contains(expectedStates, actualState), "Unexpected transaction state: " + actualState);
  }

  public static void verifyStateForFinalCheckIn(InnReachTransaction transaction) {
    var actualState = transaction.getState();
    var expectedStates = List.of(ITEM_HOLD, LOCAL_HOLD, BORROWER_RENEW,
      BORROWING_SITE_CANCEL, ITEM_IN_TRANSIT, RECEIVE_UNANNOUNCED, RETURN_UNCIRCULATED, CLAIMS_RETURNED,
      ITEM_RECEIVED, ITEM_SHIPPED, LOCAL_CHECKOUT, CANCEL_REQUEST, FINAL_CHECKIN, RECALL, OWNER_RENEW);
    Assert.isTrue(expectedStates.contains(actualState), "Unexpected transaction state: " + actualState);
  }

  public static void clearPatronAndItemInfo(TransactionHold hold) {
    hold.setPatronId(null);
    hold.setPatronName(null);
    hold.setFolioPatronId(null);
    hold.setFolioPatronBarcode(null);
    hold.setFolioItemId(null);
    hold.setFolioHoldingId(null);
    hold.setFolioInstanceId(null);
    hold.setFolioRequestId(null);
    hold.setFolioLoanId(null);
    hold.setFolioItemBarcode(null);
  }

  public static void clearCentralPatronInfo(TransactionHold hold) {
    hold.setPatronId(null);
    hold.setPatronName(null);
  }

}
