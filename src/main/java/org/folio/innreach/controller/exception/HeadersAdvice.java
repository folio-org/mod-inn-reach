package org.folio.innreach.controller.exception;

import static org.springframework.http.HttpHeaders.ACCEPT;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;

import static org.folio.innreach.external.InnReachHeaders.X_D2IR_AUTHORIZATION;
import static org.folio.innreach.external.InnReachHeaders.X_FROM_CODE;
import static org.folio.innreach.external.InnReachHeaders.X_TO_CODE;

import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackages = "org.folio.innreach.controller.d2ir")
public class HeadersAdvice {
  @ModelAttribute
  public void fetchHeader(@RequestHeader(X_TO_CODE) String xToCode,
                          @RequestHeader(X_FROM_CODE) String xFromCode,
                          @RequestHeader(X_D2IR_AUTHORIZATION) String xD2IRAuthorization,
                          @RequestHeader(ACCEPT) String accept,
                          @RequestHeader(CONTENT_TYPE) String contentType) {
    //will throw an exception if required headers are not present
  }

}
