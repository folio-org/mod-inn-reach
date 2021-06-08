package org.folio.innreach.domain.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import static org.folio.innreach.fixture.AuthenticationRequestFixture.createAuthenticationRequest;
import static org.folio.innreach.fixture.LocalServerCredentialsFixture.createLocalServerCredentials;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import org.folio.innreach.domain.exception.LocalServerCredentialsNotFoundException;
import org.folio.innreach.repository.LocalServerCredentialsRepository;

class AuthenticationServiceImplTest {

  @Mock
  private LocalServerCredentialsRepository localServerCredentialsRepository;

  @Mock
  private PasswordEncoder passwordEncoder;

  @InjectMocks
  private AuthenticationServiceImpl authenticationService;

  @BeforeEach
  void setupBeforeEach() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  void throwException_when_credentialsByServerCodeAndKeyNotFound() {
    when(localServerCredentialsRepository.findByLocalServerCodeAndKey(any(), any())).thenReturn(Optional.empty());

    var authenticationRequest = createAuthenticationRequest();

    var credentialsNotFoundException = assertThrows(LocalServerCredentialsNotFoundException.class,
        () -> authenticationService.authenticate(authenticationRequest));

    assertEquals("Can't find credentials for Central Server with code: " + authenticationRequest.getLocalServerCode(),
        credentialsNotFoundException.getMessage());

    verify(localServerCredentialsRepository).findByLocalServerCodeAndKey(any(), any());
  }

  @Test
  void throwException_when_credentialsAreNotValid() {
    when(localServerCredentialsRepository.findByLocalServerCodeAndKey(any(), any())).thenReturn(Optional.of(
        createLocalServerCredentials()));

    when(passwordEncoder.matches(any(), any())).thenReturn(false);

    var authenticationRequest = createAuthenticationRequest();

    var badCredentialsException = assertThrows(BadCredentialsException.class, () -> authenticationService.authenticate(
        authenticationRequest));

    assertEquals("Invalid Credentials", badCredentialsException.getMessage());

    verify(localServerCredentialsRepository).findByLocalServerCodeAndKey(any(), any());
    verify(passwordEncoder).matches(any(), any());
  }

  @Test
  void successfullyAuthenticate_when_credentialsAreValid() {
    when(localServerCredentialsRepository.findByLocalServerCodeAndKey(any(), any())).thenReturn(Optional.of(
        createLocalServerCredentials()));

    when(passwordEncoder.matches(any(), any())).thenReturn(true);

    var authenticationRequest = createAuthenticationRequest();

    authenticationService.authenticate(authenticationRequest);

    verify(localServerCredentialsRepository).findByLocalServerCodeAndKey(any(), any());
    verify(passwordEncoder).matches(any(), any());
  }

}
