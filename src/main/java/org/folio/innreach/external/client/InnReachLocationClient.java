package org.folio.innreach.external.client;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;

import static org.folio.innreach.external.InnReachHeaders.X_FROM_CODE;
import static org.folio.innreach.external.InnReachHeaders.X_TO_CODE;

import java.net.URI;

import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

import org.folio.innreach.external.dto.InnReachLocationDTO;
import org.folio.innreach.external.dto.InnReachLocationsDTO;
import org.springframework.web.service.annotation.DeleteExchange;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;
import org.springframework.web.service.annotation.PutExchange;

@HttpExchange("innReachLocation")
public interface InnReachLocationClient {

  @GetExchange(accept = "text/json")
  InnReachLocationsDTO getAllLocations(URI baseUri,
                                       @RequestHeader(AUTHORIZATION) String authorizationHeader,
                                       @RequestHeader(X_FROM_CODE) String xFromCode,
                                       @RequestHeader(X_TO_CODE) String xToCode);

  @PostExchange(accept = "text/json")
  void addAllLocations(URI uri,
                       @RequestHeader(AUTHORIZATION) String authorizationHeader,
                       @RequestHeader(X_FROM_CODE) String xFromCode,
                       @RequestHeader(X_TO_CODE) String xToCode,
                       @RequestBody InnReachLocationsDTO locationsDTO);

  @PostExchange(accept = "text/json")
  void addLocation(URI uri,
                   @RequestHeader(AUTHORIZATION) String authorizationHeader,
                   @RequestHeader(X_FROM_CODE) String xFromCode,
                   @RequestHeader(X_TO_CODE) String xToCode,
                   @RequestBody InnReachLocationDTO locationDTO);

  @PutExchange(value = "/innreach/v2/location/{locationKey}", accept = "text/json")
  void updateLocation(URI uri,
                      @RequestHeader(AUTHORIZATION) String authorizationHeader,
                      @RequestHeader(X_FROM_CODE) String xFromCode,
                      @RequestHeader(X_TO_CODE) String xToCode,
                      @RequestBody InnReachLocationDTO locationDTO);

  @DeleteExchange(accept = "text/json")
  void deleteLocation(URI uri,
                      @RequestHeader(AUTHORIZATION) String authorizationHeader,
                      @RequestHeader(X_FROM_CODE) String xFromCode,
                      @RequestHeader(X_TO_CODE) String xToCode);

}
