package org.folio.innreach.domain.exception;

public class CirculationException extends RuntimeException {

  public CirculationException(String message, Throwable throwable) {
    super(message, throwable);
  }
}
