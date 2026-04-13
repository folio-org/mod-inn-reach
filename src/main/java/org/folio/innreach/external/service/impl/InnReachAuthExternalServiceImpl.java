package org.folio.innreach.external.service.impl;

import static java.lang.String.format;

import java.net.URI;
import java.util.Base64;

import com.google.common.cache.Cache;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import org.folio.innreach.domain.dto.CentralServerConnectionDetailsDTO;
import org.folio.innreach.external.client.InnReachAuthClient;
import org.folio.innreach.external.dto.AccessTokenDTO;
import org.folio.innreach.external.service.InnReachAuthExternalService;
import org.folio.innreach.external.util.AuthUtils;

@RequiredArgsConstructor
@Log4j2
@Service
public class InnReachAuthExternalServiceImpl implements InnReachAuthExternalService {

  private static final String INN_REACH_ACCESS_TOKEN_PATH = "/auth/v1/oauth2/token?grant_type=client_credentials&scope=innreach_tp";
  private final InnReachAuthClient innReachAuthClient;
  private final Cache<String, AccessTokenDTO> accessTokenCache;

  @Override
  public AccessTokenDTO getAccessToken(CentralServerConnectionDetailsDTO connectionDetailsDTO) {
    var centralServerBaseUrl = connectionDetailsDTO.getConnectionUrl();
    log.debug("getAccessToken:: parameters connectionUrl: {}, localCode: {}, centralCode: {}",
      connectionDetailsDTO.getConnectionUrl(), connectionDetailsDTO.getLocalCode(), connectionDetailsDTO.getCentralCode());
    var cachedAccessToken = accessTokenCache.getIfPresent(centralServerBaseUrl);

    if (cachedAccessToken != null) {
      return cachedAccessToken;
    }

    var accessTokenUrl = format("%s/innreach/v2%s", centralServerBaseUrl, INN_REACH_ACCESS_TOKEN_PATH);
    var authorizationHeader = buildBasicAuthorizationHeader(connectionDetailsDTO);
    var responseEntity = innReachAuthClient.getAccessToken(URI.create(accessTokenUrl), authorizationHeader);

    var accessTokenDTO = responseEntity.getBody();

    accessTokenCache.put(centralServerBaseUrl, accessTokenDTO);

    log.info("getAccessToken:: result tokenType: {}, expiresIn: {}",
      accessTokenDTO == null ? null : accessTokenDTO.getTokenType(),
      accessTokenDTO == null ? null : accessTokenDTO.getExpiresIn());
    return accessTokenDTO;
  }

  private String buildBasicAuthorizationHeader(CentralServerConnectionDetailsDTO connectionDetailsDTO) {
    var keySecret = format("%s:%s", connectionDetailsDTO.getKey(), connectionDetailsDTO.getSecret());
    var base64EncodedKeySecret = Base64.getEncoder().encodeToString(keySecret.getBytes());
    return AuthUtils.buildBasicAuthHeader(base64EncodedKeySecret);
  }
}
