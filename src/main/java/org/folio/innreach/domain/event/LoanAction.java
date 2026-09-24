package org.folio.innreach.domain.event;

import java.util.Arrays;
import org.apache.commons.lang3.Strings;

public enum LoanAction {
  CHECKED_IN("checkedin"),
  RENEW("renewed"),
  CLAIMED_RETURNED("claimedReturned"),
  RECALL_REQUESTED("recallrequested"),
  DUE_DATE_CHANGED("dueDateChanged");

  private final String value;

  LoanAction(String value) {
    this.value = value;
  }

  public String getValue() {
    return value;
  }

  public static LoanAction from(String action) {
    return Arrays.stream(values())
      .filter(status -> status.valueMatches(action))
      .findFirst()
      .orElse(null);
  }

  private boolean valueMatches(String value) {
    return Strings.CI.equals(getValue(), value);
  }
}
