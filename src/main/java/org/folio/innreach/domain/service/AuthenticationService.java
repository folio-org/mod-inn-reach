package org.folio.innreach.domain.service;

import org.folio.innreach.dto.AuthenticationRequest;

public interface AuthenticationService {

  void authenticate(AuthenticationRequest authenticationRequest);
}
