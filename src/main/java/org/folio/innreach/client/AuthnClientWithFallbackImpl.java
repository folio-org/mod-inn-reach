package org.folio.innreach.client;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

public class AuthnClientWithFallbackImpl implements AuthnClient{
  @Override
  public ResponseEntity<LoginResponse> loginWithExpiry(UserCredentials credentials) {
    return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
  }

  @Override
  public ResponseEntity<LoginResponse> login(UserCredentials credentials) {
    return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
  }

  @Override
  public void saveCredentials(UserCredentials credentials) {

  }
}
