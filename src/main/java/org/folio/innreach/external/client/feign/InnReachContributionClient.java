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

import org.folio.innreach.dto.BibInfo;
import org.folio.innreach.external.client.feign.config.InnReachFeignClientConfig;
import org.folio.innreach.external.dto.BibItemsInfo;
import org.folio.innreach.external.dto.InnReachResponse;

@FeignClient(value = "innReachContribution", configuration = InnReachFeignClientConfig.class)
public interface InnReachContributionClient {

  @PostMapping(value = "/innreach/v2/contribution/bib/{bibId}", consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
  InnReachResponse contributeBib(URI baseUri,
                                 @RequestHeader(AUTHORIZATION) String authorizationHeader,
                                 @RequestHeader(X_FROM_CODE) String xFromCode,
                                 @RequestHeader(X_TO_CODE) String xToCode,
                                 @PathVariable String bibId,
                                 @RequestBody BibInfo bib);

  @PostMapping(value = "/innreach/v2/contribution/items/{bibId}", produces = APPLICATION_JSON_VALUE)
  InnReachResponse contributeBibItems(URI baseUri,
                                      @RequestHeader(AUTHORIZATION) String authorizationHeader,
                                      @RequestHeader(X_FROM_CODE) String xFromCode,
                                      @RequestHeader(X_TO_CODE) String xToCode,
                                      @PathVariable String bibId,
                                      @RequestBody BibItemsInfo bibItems);

  @GetMapping(value = "/innreach/v2/local/{localCode}/bib/{bibId}", produces = APPLICATION_JSON_VALUE)
  InnReachResponse lookUpBib(URI baseUri,
                             @RequestHeader(AUTHORIZATION) String authorizationHeader,
                             @RequestHeader(X_FROM_CODE) String xFromCode,
                             @RequestHeader(X_TO_CODE) String xToCode,
                             @PathVariable("localCode") String localCode,
                             @PathVariable("bibId") String bibId);

}
