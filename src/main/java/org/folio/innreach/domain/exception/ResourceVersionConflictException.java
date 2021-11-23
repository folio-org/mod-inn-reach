package org.folio.innreach.domain.exception;

import feign.error.FeignExceptionConstructor;

public class ResourceVersionConflictException extends RuntimeException {

  @FeignExceptionConstructor
  public ResourceVersionConflictException(String message) {
    super(message);
  }

}
