package org.folio.innreach.external.client.feign.error;

import static feign.FeignException.errorStatus;

import feign.Response;
import feign.codec.ErrorDecoder;
import lombok.extern.log4j.Log4j2;
import org.folio.innreach.external.exception.InnReachGatewayException;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;

import org.folio.innreach.external.exception.InnReachException;

@Log4j2
public class InnReachFeignErrorDecoder implements ErrorDecoder {

  @Override
  public Exception decode(String methodKey, Response response) {
    if (HttpStatus.valueOf(response.status()).equals(HttpStatus.UNAUTHORIZED)) {
      log.debug("Can't get InnReach access token. CentralServer authentication failed with status: {}", response.status());
      return new BadCredentialsException("Can't get InnReach access token. Key/Secret pair is not valid");
    }
    if(HttpStatus.valueOf(response.status()).equals(HttpStatus.BAD_GATEWAY) ||
      HttpStatus.valueOf(response.status()).equals(HttpStatus.GATEWAY_TIMEOUT)) {
      log.debug("INN_Reach call failed with status {}", response.status());
      return new InnReachGatewayException("INN_Reach call failed with status: " + errorStatus(methodKey, response).getMessage());
    }
    var e = errorStatus(methodKey, response);
    return new InnReachException("INN-Reach call failed: " + e.getMessage());
  }
}
