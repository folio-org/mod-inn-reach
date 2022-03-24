package org.folio.innreach.util;

import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.util.Assert;

import org.folio.innreach.domain.entity.InnReachTransaction;

@UtilityClass
public class InnReachTransactionUtils {

  public static void verifyState(InnReachTransaction transaction, InnReachTransaction.TransactionState... expectedStates) {
    var actualState = transaction.getState();
    Assert.isTrue(ArrayUtils.contains(expectedStates, actualState), "Unexpected transaction state: " + actualState);
  }

  public static void clearCentralPatronInfo(InnReachTransaction transaction) {
    var hold = transaction.getHold();
    hold.setPatronId(null);
    hold.setPatronName(null);
  }

}
