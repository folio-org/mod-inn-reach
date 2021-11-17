package org.folio.innreach.external.client.feign.config;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;

import static org.folio.innreach.external.InnReachHeaders.X_FROM_CODE;
import static org.folio.innreach.external.InnReachHeaders.X_TO_CODE;
import static org.folio.innreach.external.util.AuthUtils.buildBearerAuthHeader;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import feign.Client;
import feign.Request;
import feign.Response;
import feign.okhttp.OkHttpClient;

import org.folio.innreach.domain.dto.CentralServerConnectionDetailsDTO;
import org.folio.innreach.domain.service.CentralServerService;
import org.folio.innreach.external.service.InnReachAuthExternalService;

public class D2irFeignClient implements Client {
  private final InnReachAuthExternalService authService;
  private final CentralServerService centralServerService;
  private final OkHttpClient delegate;

  public D2irFeignClient(InnReachAuthExternalService authService,
                         CentralServerService centralServerService,
                         okhttp3.OkHttpClient okHttpClient) {
    this.authService = authService;
    this.centralServerService = centralServerService;
    this.delegate = new OkHttpClient(okHttpClient);
  }

  @Override
  public Response execute(Request request, Request.Options options) throws IOException {
    var centralCode = request.headers().get(X_TO_CODE).stream().findFirst()
      .orElseThrow(() -> new IllegalStateException("Unable to prepare D2IR client request: " + X_TO_CODE + " is not set"));

    var connectionDetails = centralServerService.getConnectionDetailsByCode(centralCode);

    var url = resolveUrl(request, connectionDetails);

    var accessToken = authService.getAccessToken(connectionDetails);
    var bearerAuth = buildBearerAuthHeader(accessToken.getAccessToken());

    Map<String, Collection<String>> allHeaders = new HashMap<>(request.headers());
    allHeaders.put(X_FROM_CODE, List.of(connectionDetails.getLocalCode()));
    allHeaders.put(X_TO_CODE, List.of(centralCode));
    allHeaders.put(AUTHORIZATION, List.of(bearerAuth));

    var requestWithURL = Request.create(request.httpMethod(), url, allHeaders,
      request.body(), request.charset(), request.requestTemplate());

    return delegate.execute(requestWithURL, options);
  }

  private String resolveUrl(Request request, CentralServerConnectionDetailsDTO connectionDetails) {
    var baseUrl = connectionDetails.getConnectionUrl();
    if (!baseUrl.endsWith("/")) {
      baseUrl += "/";
    }
    return request.url().replace("http://", baseUrl);
  }
}
