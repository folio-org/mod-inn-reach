package org.folio.innreach.domain.exception;

public class ItemNotRequestableException extends CirculationException {
  public ItemNotRequestableException(String message) {
    super(message);
  }

  public ItemNotRequestableException(String message, Throwable cause) {
    super(message, cause);
  }
}
