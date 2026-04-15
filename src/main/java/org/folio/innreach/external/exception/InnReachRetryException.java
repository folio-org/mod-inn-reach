package org.folio.innreach.external.exception;

public class InnReachRetryException extends RuntimeException {
  public InnReachRetryException(String message) {
    super(message);
  }

  public InnReachRetryException(String message, Throwable throwable) {
    super(message, throwable);
  }
}
