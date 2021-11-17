package org.folio.innreach.external.client.feign;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import static org.folio.innreach.external.InnReachHeaders.X_FROM_CODE;
import static org.folio.innreach.external.InnReachHeaders.X_TO_CODE;

import java.net.URI;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

import org.folio.innreach.external.client.feign.config.InnReachFeignClientConfig;

@FeignClient(value = "innReach", configuration = InnReachFeignClientConfig.class)
public interface InnReachClient {

  @GetMapping(headers = "Accept=application/json")
  String callInnReachApi(URI baseUri,
                         @RequestHeader(AUTHORIZATION) String authorizationHeader,
                         @RequestHeader(X_FROM_CODE) String xFromCode,
                         @RequestHeader(X_TO_CODE) String xToCode);

  @PostMapping(consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
  String postInnReachApi(URI baseUri,
                         @RequestHeader(AUTHORIZATION) String authorizationHeader,
                         @RequestHeader(X_FROM_CODE) String xFromCode,
                         @RequestHeader(X_TO_CODE) String xToCode,
                         Object dto);

  @PostMapping(value = "/innreach/v2/circ/{operation}/{trackingId}/{centralCode}",
    consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
  String postCircRequest(@RequestHeader(AUTHORIZATION) String authorizationHeader,
                         @RequestHeader(X_FROM_CODE) String xFromCode,
                         @RequestHeader(X_TO_CODE) String xToCode,
                         @PathVariable("operation") String operation,
                         @PathVariable("trackingId") String trackingId,
                         @PathVariable("centralCode") String centralCode,
                         @RequestBody(required = false) Object payload);
}
