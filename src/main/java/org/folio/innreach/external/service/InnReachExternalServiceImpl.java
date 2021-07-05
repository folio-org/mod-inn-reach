package org.folio.innreach.external.service;

import java.net.URI;
import java.util.Base64;

import feign.FeignException.FeignClientException;
import feign.RetryableException;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import org.folio.innreach.external.client.feign.InnReachFeignClient;
import org.folio.innreach.external.dto.AccessTokenDTO;
import org.folio.innreach.external.dto.AccessTokenRequestDTO;
import org.folio.innreach.external.exception.InnReachException;

@RequiredArgsConstructor
@Log4j2
@Service
public class InnReachExternalServiceImpl implements InnReachExternalService {

  private static final String ACCESS_TOKEN_RETRIEVAL_ERR_MSG = "Can't get InnReach access token";
  
  private final InnReachFeignClient innReachClient;

  @Override
  public AccessTokenDTO getAccessToken(AccessTokenRequestDTO tokenRequestDTO) {
    var centralServerUri = URI.create(tokenRequestDTO.getCentralServerUri());
    var basicAuthorizationHeader = getBasicAuthorizationHeader(tokenRequestDTO);

    try {
      var responseEntity = innReachClient.getAccessToken(centralServerUri, basicAuthorizationHeader);

      return responseEntity.getBody();

    } catch (FeignClientException e) {
      log.error(ACCESS_TOKEN_RETRIEVAL_ERR_MSG, e);
      throw new InnReachException("Can't get InnReach access token. Key/Secret pair is not valid");
    } catch (RetryableException e) {
      log.error(ACCESS_TOKEN_RETRIEVAL_ERR_MSG, e);
      throw new InnReachException("Can't get InnReach access token. the Central server with URI: "
        + tokenRequestDTO.getCentralServerUri() + " is not available");
    } catch (IllegalArgumentException e) {
      log.error(ACCESS_TOKEN_RETRIEVAL_ERR_MSG, e);
      throw new InnReachException("Can't get InnReach access token. The Central server URI must be absolute");
  }
  }

  private String getBasicAuthorizationHeader(AccessTokenRequestDTO tokenRequestDTO) {
    var keySecret = String.format("%s:%s", tokenRequestDTO.getKey(), tokenRequestDTO.getSecret());
    return "Basic " + Base64.getEncoder().encodeToString(keySecret.getBytes());
  }
}
