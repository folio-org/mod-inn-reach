package org.folio.innreach.external.service.impl;

import java.net.URI;
import java.util.Base64;

import com.google.common.cache.Cache;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import org.folio.innreach.external.client.feign.InnReachAuthClient;
import org.folio.innreach.external.dto.AccessTokenDTO;
import org.folio.innreach.external.dto.AccessTokenRequestDTO;
import org.folio.innreach.external.service.InnReachAuthService;

@RequiredArgsConstructor
@Log4j2
@Service
public class InnReachAuthServiceImpl implements InnReachAuthService {

  private final InnReachAuthClient innReachAuthClient;
  private final Cache<String, AccessTokenDTO> accessTokenCache;

  @Override
  public AccessTokenDTO getAccessToken(AccessTokenRequestDTO tokenRequestDTO) {
    var cachedAccessToken = accessTokenCache.getIfPresent(tokenRequestDTO.getCentralServerUri());

    if (cachedAccessToken != null) {
      return cachedAccessToken;
    }

    var responseEntity = innReachAuthClient.getAccessToken(
      URI.create(tokenRequestDTO.getCentralServerUri()),
      buildBasicAuthorizationHeader(tokenRequestDTO)
    );

    var accessTokenDTO = responseEntity.getBody();

    accessTokenCache.put(tokenRequestDTO.getCentralServerUri(), accessTokenDTO);

    return accessTokenDTO;
  }

  private String buildBasicAuthorizationHeader(AccessTokenRequestDTO tokenRequestDTO) {
    var keySecret = String.format("%s:%s", tokenRequestDTO.getKey(), tokenRequestDTO.getSecret());
    return "Basic " + Base64.getEncoder().encodeToString(keySecret.getBytes());
  }
}
