package org.folio.innreach.external.client;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import static org.folio.innreach.external.InnReachHeaders.X_FROM_CODE;
import static org.folio.innreach.external.InnReachHeaders.X_TO_CODE;

import java.net.URI;

import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

@HttpExchange(value = "innReach", contentType = APPLICATION_JSON_VALUE, accept = APPLICATION_JSON_VALUE)
public interface InnReachClient {

  @GetExchange
  String callInnReachApi(URI baseUri,
                         @RequestHeader(AUTHORIZATION) String authorizationHeader,
                         @RequestHeader(X_FROM_CODE) String xFromCode,
                         @RequestHeader(X_TO_CODE) String xToCode);

  @PostExchange
  String postInnReachApi(URI baseUri,
                         @RequestHeader(AUTHORIZATION) String authorizationHeader,
                         @RequestHeader(X_FROM_CODE) String xFromCode,
                         @RequestHeader(X_TO_CODE) String xToCode,
                         @RequestBody Object dto);

  @PostExchange
  String postInnReachApi(URI baseUri,
                         @RequestHeader(AUTHORIZATION) String authorizationHeader,
                         @RequestHeader(X_FROM_CODE) String xFromCode,
                         @RequestHeader(X_TO_CODE) String xToCode);
}
