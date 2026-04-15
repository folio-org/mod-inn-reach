package org.folio.innreach.external.client;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;

import java.net.URI;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestHeader;

import org.folio.innreach.external.dto.AccessTokenDTO;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

@HttpExchange("innReachAuth")
public interface InnReachAuthClient {

  @PostExchange()
  ResponseEntity<AccessTokenDTO> getAccessToken(URI authTokenUri, @RequestHeader(AUTHORIZATION) String authorizationHeader);
}
