package org.folio.innreach.domain.exception;

import org.springframework.security.core.AuthenticationException;

public class LocalServerCredentialsNotFoundException extends AuthenticationException {

  public LocalServerCredentialsNotFoundException(String msg) {
    super(msg);
  }
}
