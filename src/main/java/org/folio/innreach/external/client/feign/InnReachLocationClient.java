package org.folio.innreach.external.client.feign;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;

import static org.folio.innreach.external.InnReachHeaders.X_FROM_CODE;
import static org.folio.innreach.external.InnReachHeaders.X_TO_CODE;

import java.net.URI;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

import org.folio.innreach.external.client.feign.config.InnReachFeignClientConfig;
import org.folio.innreach.external.dto.InnReachLocationDTO;
import org.folio.innreach.external.dto.InnReachLocationsDTO;

@FeignClient(value = "innReachLocation", configuration = InnReachFeignClientConfig.class)
public interface InnReachLocationClient {

  @GetMapping(value = "/innreach/v2/contribution/locations", headers = "Accept=text/json")
  InnReachLocationsDTO getAllLocations(URI baseUri,
                                       @RequestHeader(AUTHORIZATION) String authorizationHeader,
                                       @RequestHeader(X_FROM_CODE) String xFromCode,
                                       @RequestHeader(X_TO_CODE) String xToCode);

  @PostMapping(value = "/innreach/v2/contribution/locations", headers = "Accept=text/json")
  void addAllLocations(URI baseUri,
                       @RequestHeader(AUTHORIZATION) String authorizationHeader,
                       @RequestHeader(X_FROM_CODE) String xFromCode,
                       @RequestHeader(X_TO_CODE) String xToCode,
                       @RequestBody InnReachLocationsDTO locationsDTO);

  @PostMapping(value = "/innreach/v2/location/{locationKey}", headers = "Accept=text/json")
  void addLocation(URI baseUri,
                   @RequestHeader(AUTHORIZATION) String authorizationHeader,
                   @RequestHeader(X_FROM_CODE) String xFromCode,
                   @RequestHeader(X_TO_CODE) String xToCode,
                   @PathVariable("locationKey") String locationKey,
                   @RequestBody InnReachLocationDTO locationDTO);

  @PutMapping(value = "/innreach/v2/location/{locationKey}", headers = "Accept=text/json")
  void updateLocation(URI baseUri,
                      @RequestHeader(AUTHORIZATION) String authorizationHeader,
                      @RequestHeader(X_FROM_CODE) String xFromCode,
                      @RequestHeader(X_TO_CODE) String xToCode,
                      @PathVariable("locationKey") String locationKey,
                      @RequestBody InnReachLocationDTO locationDTO);

  @DeleteMapping(value = "/innreach/v2/location/{locationKey}", headers = "Accept=text/json")
  void deleteLocation(URI baseUri,
                      @RequestHeader(AUTHORIZATION) String authorizationHeader,
                      @RequestHeader(X_FROM_CODE) String xFromCode,
                      @RequestHeader(X_TO_CODE) String xToCode,
                      @PathVariable("locationKey") String locationKey);

}
