package org.folio.innreach.external.exception;

public class RetryException extends RuntimeException {
  public RetryException(String message) {
    super(message);
  }
}
