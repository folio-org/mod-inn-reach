package org.folio.innreach.domain.exception;

public class ItemNotRequestableException extends RuntimeException {
  public ItemNotRequestableException(String message) {
    super(message);
  }

  public ItemNotRequestableException(String message, Throwable cause) {
    super(message, cause);
  }
}
