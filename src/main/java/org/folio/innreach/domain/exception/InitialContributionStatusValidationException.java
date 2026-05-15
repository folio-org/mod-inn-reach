package org.folio.innreach.domain.exception;

public class InitialContributionStatusValidationException extends RuntimeException {

  public InitialContributionStatusValidationException(String message) {
    super(message);
  }

  public InitialContributionStatusValidationException(String message, Throwable cause) {
    super(message, cause);
  }
}
