package org.folio.innreach.domain.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import org.folio.innreach.domain.exception.LocalServerCredentialsNotFoundException;
import org.folio.innreach.domain.service.AuthenticationService;
import org.folio.innreach.dto.AuthenticationRequest;
import org.folio.innreach.repository.LocalServerCredentialsRepository;

@Log4j2
@RequiredArgsConstructor
@Service
public class AuthenticationServiceImpl implements AuthenticationService {

  private final LocalServerCredentialsRepository localServerCredentialsRepository;
  private final PasswordEncoder passwordEncoder;

  @Override
  public void authenticate(AuthenticationRequest authenticationRequest) {
    log.debug("authenticate:: parameters authenticationRequest: {}", authenticationRequest);
    var localServerCredentials = localServerCredentialsRepository
      .findByLocalServerKey(authenticationRequest.getKey().toString())
      .orElseThrow(() -> new LocalServerCredentialsNotFoundException(
        "Can't find credentials for Local Server with key: " + authenticationRequest.getKey()));

    if (!doSecretsMatch(authenticationRequest.getSecret().toString(), localServerCredentials.getLocalServerSecret())) {
      throw new BadCredentialsException("Invalid Credentials");
    }
    log.info("authenticate:: Authentication successful");
  }

  private boolean doSecretsMatch(String rawSecret, String encodedSecret) {
    return passwordEncoder.matches(rawSecret, encodedSecret);
  }
}
