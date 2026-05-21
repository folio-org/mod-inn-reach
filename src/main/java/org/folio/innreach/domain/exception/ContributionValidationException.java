package org.folio.innreach.domain.exception;

public class ContributionValidationException extends RuntimeException {

  public ContributionValidationException(String message) {
    super(message);
  }

  public ContributionValidationException(String message, Throwable cause) {
    super(message, cause);
  }
}
