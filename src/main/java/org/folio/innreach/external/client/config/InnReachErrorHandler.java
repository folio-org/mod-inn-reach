package org.folio.innreach.external.client.config;

import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.innreach.external.exception.InnReachException;
import org.folio.innreach.external.exception.InnReachGatewayException;
import org.folio.innreach.util.JsonHelper;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClientResponseException;

@Log4j2
@RequiredArgsConstructor
@Component
public class InnReachErrorHandler {

  private final JsonHelper jsonHelper;

  public void handle(ClientHttpResponse response) throws IOException {
    var errorMessage = parseErrorMessage(response);
    var statusValue = response.getStatusCode().value();
    if (statusValue == HttpStatus.UNAUTHORIZED.value()) {
      log.debug("Can't get InnReach access token. CentralServer authentication failed with status: {}", statusValue);
      throw new BadCredentialsException("Can't get InnReach access token. Key/Secret pair is not valid");
    }

    if (isGatewayError(response)) {
      log.debug("INN_Reach call failed with status {}", statusValue);
      throw new InnReachGatewayException("INN_Reach call failed with status: %s and message: %s"
        .formatted(statusValue, errorMessage));
    }

    throw new InnReachException("INN-Reach call failed: " + errorMessage);
  }

  private String parseErrorMessage(ClientHttpResponse response) {
    try {
      if (response.getStatusCode().is5xxServerError()) {
        return jsonHelper.fromJson(response.getBody(), HttpServerErrorException.class).getMessage();
      }

      if (response.getStatusCode().is4xxClientError()) {
        return jsonHelper.fromJson(response.getBody(), HttpClientErrorException.class).getMessage();
      }

      return jsonHelper.fromJson(response.getBody(), RestClientResponseException.class).getMessage();
    } catch (IOException | IllegalStateException e) {
      return "";
    }
  }

  private boolean isGatewayError(ClientHttpResponse response) throws IOException {
    return response.getStatusCode().value() == HttpStatus.BAD_GATEWAY.value() ||
      response.getStatusCode().value() == HttpStatus.GATEWAY_TIMEOUT.value();
  }
}
