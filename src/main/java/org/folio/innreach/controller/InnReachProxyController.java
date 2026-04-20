package org.folio.innreach.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import org.folio.innreach.external.service.InnReachExternalService;

@Log4j2
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
  public void handleD2RProxyCall(@PathVariable UUID centralServerId,
                                 HttpServletRequest request,
                                 HttpServletResponse response) throws IOException {
    var d2RRequestUri = getD2RRequestUri(request);
    var innReachResponse = innReachExternalService.callInnReachApi(centralServerId, d2RRequestUri);

    log.info("callInnReachApi:: d2RRequestUri: {}, result response: {}", d2RRequestUri, innReachResponse);

    var body = innReachResponse == null ? "" : innReachResponse;
    var bytes = body.getBytes(StandardCharsets.UTF_8);

    response.setStatus(HttpStatus.OK.value());
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    response.setCharacterEncoding(StandardCharsets.UTF_8.name());
    response.setContentLength(bytes.length);
    response.getOutputStream().write(bytes);
    response.getOutputStream().flush();
  }

  private String getD2RRequestUri(HttpServletRequest request) {
    var requestURI = request.getRequestURI();
    var d2rUriBeginIndex = requestURI.indexOf(D2R_API_PREFIX) + D2R_API_PREFIX_LENGTH;
    return requestURI.substring(d2rUriBeginIndex);
  }
}
