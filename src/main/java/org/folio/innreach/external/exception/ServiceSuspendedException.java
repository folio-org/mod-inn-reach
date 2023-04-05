package org.folio.innreach.external.exception;

public class ServiceSuspendedException extends RuntimeException {
  public ServiceSuspendedException(String message) {
    super(message);
  }
}
