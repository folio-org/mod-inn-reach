package org.folio.innreach.external.client;

import static org.folio.innreach.util.UriHelper.buildUri;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import static org.folio.innreach.external.InnReachHeaders.X_FROM_CODE;
import static org.folio.innreach.external.InnReachHeaders.X_TO_CODE;

import java.net.URI;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

import org.folio.innreach.dto.BibInfo;
import org.folio.innreach.external.dto.BibItemsInfo;
import org.folio.innreach.external.dto.InnReachResponse;
import org.springframework.web.service.annotation.DeleteExchange;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

@HttpExchange("innReachContribution")
public interface InnReachContributionClient {

  @PostExchange(accept = APPLICATION_JSON_VALUE)
  InnReachResponse contributeRecord(URI fullUri,
                                    @RequestHeader(AUTHORIZATION) String authorizationHeader,
                                    @RequestHeader(X_FROM_CODE) String xFromCode,
                                    @RequestHeader(X_TO_CODE) String xToCode,
                                    @RequestBody Object requestBody);

  @DeleteExchange(accept = APPLICATION_JSON_VALUE)
  InnReachResponse deContributeRecord(URI fullUri,
                                      @RequestHeader(AUTHORIZATION) String authorizationHeader,
                                      @RequestHeader(X_FROM_CODE) String xFromCode,
                                      @RequestHeader(X_TO_CODE) String xToCode);

  @GetExchange(accept = APPLICATION_JSON_VALUE)
  InnReachResponse lookUpRecord(URI fullUri,
                                @RequestHeader(AUTHORIZATION) String authorizationHeader,
                                @RequestHeader(X_FROM_CODE) String xFromCode,
                                @RequestHeader(X_TO_CODE) String xToCode);

  default InnReachResponse contributeBib(URI baseUri,
                                         @RequestHeader(AUTHORIZATION) String authorizationHeader,
                                         @RequestHeader(X_FROM_CODE) String xFromCode,
                                         @RequestHeader(X_TO_CODE) String xToCode,
                                         @PathVariable String bibId,
                                         @RequestBody BibInfo bib) {
    var fullUri = buildUri(baseUri, "/innreach/v2/contribution/bib/{bibId}", bibId);
    return contributeRecord(fullUri, authorizationHeader, xFromCode, xToCode, bib);
  }

  default InnReachResponse contributeBibItems(URI baseUri,
                                              @RequestHeader(AUTHORIZATION) String authorizationHeader,
                                              @RequestHeader(X_FROM_CODE) String xFromCode,
                                              @RequestHeader(X_TO_CODE) String xToCode,
                                              @PathVariable String bibId,
                                              @RequestBody BibItemsInfo bibItems) {
    var fullUri = buildUri(baseUri, "/innreach/v2/contribution/items/{bibId}", bibId);
    return contributeRecord(fullUri, authorizationHeader, xFromCode, xToCode, bibItems);
  }

  default InnReachResponse deContributeBib(URI baseUri,
                                           @RequestHeader(AUTHORIZATION) String authorizationHeader,
                                           @RequestHeader(X_FROM_CODE) String xFromCode,
                                           @RequestHeader(X_TO_CODE) String xToCode,
                                           @PathVariable String bibId) {
    var fullUri = buildUri(baseUri, "/innreach/v2/contribution/bib/{bibId}", bibId);
    return deContributeRecord(fullUri, authorizationHeader, xFromCode, xToCode);
  }

  default InnReachResponse deContributeBibItem(URI baseUri,
                                               @RequestHeader(AUTHORIZATION) String authorizationHeader,
                                               @RequestHeader(X_FROM_CODE) String xFromCode,
                                               @RequestHeader(X_TO_CODE) String xToCode,
                                               @PathVariable String itemId) {
    var fullUri = buildUri(baseUri, "/innreach/v2/contribution/item/{itemId}", itemId);
    return deContributeRecord(fullUri, authorizationHeader, xFromCode, xToCode);
  }

  default InnReachResponse lookUpBib(URI baseUri,
                                     @RequestHeader(AUTHORIZATION) String authorizationHeader,
                                     @RequestHeader(X_FROM_CODE) String xFromCode,
                                     @RequestHeader(X_TO_CODE) String xToCode,
                                     @PathVariable("localCode") String localCode,
                                     @PathVariable("bibId") String bibId) {
    var fullUri = buildUri(baseUri, "/innreach/v2/local/{localCode}/bib/{bibId}", localCode, bibId);
    return lookUpRecord(fullUri, authorizationHeader, xFromCode, xToCode);
  }

  default InnReachResponse lookUpBibItem(URI baseUri,
                                 @RequestHeader(AUTHORIZATION) String authorizationHeader,
                                 @RequestHeader(X_FROM_CODE) String xFromCode,
                                 @RequestHeader(X_TO_CODE) String xToCode,
                                 @PathVariable("localCode") String localCode,
                                 @PathVariable("bibId") String bibId,
                                 @PathVariable("itemId") String itemId) {
    var fullUri = buildUri(baseUri, "/innreach/v2/local/{localCode}/bib/{bibId}/item/{itemId}", localCode, bibId, itemId);
    return lookUpRecord(fullUri, authorizationHeader, xFromCode, xToCode);
  }

}
