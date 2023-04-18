package org.folio.innreach.external.exception;

public class SocketTimeOutExceptionWrapper extends RuntimeException{
  public SocketTimeOutExceptionWrapper(String message) {
    super(message);
  }
}
