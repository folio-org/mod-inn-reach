package org.folio.innreach.controller;

import java.util.UUID;

import javax.servlet.http.HttpServletRequest;

import lombok.RequiredArgsConstructor;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import org.folio.innreach.external.service.InnReachExternalService;

@RequiredArgsConstructor
@RestController
public class InnReachProxyController {

  private static final String D2R_API_PREFIX = "d2r";
  private static final int D2R_API_PREFIX_LENGTH = 3;

  private final InnReachExternalService innReachExternalService;

  @GetMapping(
    value = "/inn-reach/central-servers/{centralServerId}/d2r/**",
    produces = MimeTypeUtils.APPLICATION_JSON_VALUE
  )
  public String handleD2RProxyCall(@PathVariable UUID centralServerId, HttpServletRequest request) {
    var d2RRequestUri = getD2RRequestUri(request);
    return innReachExternalService.callInnReachApi(centralServerId, d2RRequestUri);
  }

  private String getD2RRequestUri(HttpServletRequest request) {
    var requestURI = request.getRequestURI();
    var d2rUriBeginIndex = requestURI.indexOf(D2R_API_PREFIX) + D2R_API_PREFIX_LENGTH;
    return requestURI.substring(d2rUriBeginIndex);
  }
}
