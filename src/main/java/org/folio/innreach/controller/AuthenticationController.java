package org.folio.innreach.controller;

import javax.validation.Valid;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import org.folio.innreach.domain.service.AuthenticationService;
import org.folio.innreach.dto.AuthenticationRequest;
import org.folio.innreach.rest.resource.AuthenticationApi;

@RequiredArgsConstructor
@RestController
@RequestMapping("/inn-reach/authentication")
public class AuthenticationController implements AuthenticationApi {

  private final AuthenticationService authenticationService;

  @Override
  @PostMapping
  public ResponseEntity<Void> authenticateLocalServerKeySecret(
      @Valid AuthenticationRequest authenticationRequest) {
    authenticationService.authenticate(authenticationRequest);
    return ResponseEntity.ok().build();
  }
}
