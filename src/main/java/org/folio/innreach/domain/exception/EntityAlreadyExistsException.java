package org.folio.innreach.domain.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class EntityAlreadyExistsException extends RuntimeException {

  public EntityAlreadyExistsException(String message) {
    super(message);
  }

  public EntityAlreadyExistsException(String message, Throwable cause) {
    super(message, cause);
  }
}
