package org.folio.innreach.external.service.impl;

import java.net.URI;
import java.util.Base64;

import com.google.common.cache.Cache;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import org.folio.innreach.domain.dto.CentralServerConnectionDetailsDTO;
import org.folio.innreach.external.client.feign.InnReachAuthClient;
import org.folio.innreach.external.dto.AccessTokenDTO;
import org.folio.innreach.external.service.InnReachAuthExternalService;

@RequiredArgsConstructor
@Log4j2
@Service
public class InnReachAuthExternalServiceImpl implements InnReachAuthExternalService {

  private final InnReachAuthClient innReachAuthClient;
  private final Cache<String, AccessTokenDTO> accessTokenCache;

  @Override
  public AccessTokenDTO getAccessToken(CentralServerConnectionDetailsDTO connectionDetailsDTO) {
    var cachedAccessToken = accessTokenCache.getIfPresent(connectionDetailsDTO.getConnectionUrl());

    if (cachedAccessToken != null) {
      return cachedAccessToken;
    }

    var responseEntity = innReachAuthClient.getAccessToken(
      URI.create(connectionDetailsDTO.getConnectionUrl()),
      buildBasicAuthorizationHeader(connectionDetailsDTO)
    );

    var accessTokenDTO = responseEntity.getBody();

    accessTokenCache.put(connectionDetailsDTO.getConnectionUrl(), accessTokenDTO);

    return accessTokenDTO;
  }

  private String buildBasicAuthorizationHeader(CentralServerConnectionDetailsDTO connectionDetailsDTO) {
    var keySecret = String.format("%s:%s", connectionDetailsDTO.getKey(), connectionDetailsDTO.getSecret());
    return "Basic " + Base64.getEncoder().encodeToString(keySecret.getBytes());
  }
}
