package org.folio.innreach.external.exception;

import static org.folio.innreach.util.InnReachConstants.INITIAL_CONTRIBUTION_RETRY_LIMIT_MESSAGE;

public class InnReachInitialContributionException extends RuntimeException {

  public InnReachInitialContributionException(String message) {
    super(message);
  }

  public static InnReachInitialContributionException initialContributionRetryExhausted() {
    return new InnReachInitialContributionException(INITIAL_CONTRIBUTION_RETRY_LIMIT_MESSAGE);
  }
}
