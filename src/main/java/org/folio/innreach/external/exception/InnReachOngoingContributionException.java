package org.folio.innreach.external.exception;

import static org.folio.innreach.util.InnReachConstants.ONGOING_CONTRIBUTION_RETRY_LIMIT_MESSAGE;

public class InnReachOngoingContributionException extends RuntimeException {

  public InnReachOngoingContributionException(String message) {
    super(message);
  }

  public static InnReachOngoingContributionException ongoingContributionRetryExhausted() {
    return new InnReachOngoingContributionException(ONGOING_CONTRIBUTION_RETRY_LIMIT_MESSAGE);
  }
}
