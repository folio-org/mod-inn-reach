package org.folio.innreach.domain.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.UUID;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class EntityNotFoundException extends RuntimeException {

  public EntityNotFoundException(String message) {
    super(message);
  }

  public static EntityNotFoundException centralServerNotFoundByCode(String code) {
    return new EntityNotFoundException("Central server with code: " + code + " not found");
  }

  public static EntityNotFoundException centralServerNotFoundById(UUID centralServerId) {
    return new EntityNotFoundException("Central server with ID: " + centralServerId + " not found");
  }

}
