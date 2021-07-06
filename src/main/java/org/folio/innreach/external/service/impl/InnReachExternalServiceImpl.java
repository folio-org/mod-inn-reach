package org.folio.innreach.external.service.impl;

import java.net.URI;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import org.folio.innreach.domain.entity.CentralServer;
import org.folio.innreach.domain.exception.EntityNotFoundException;
import org.folio.innreach.external.client.feign.InnReachClient;
import org.folio.innreach.external.dto.AccessTokenDTO;
import org.folio.innreach.external.dto.AccessTokenRequestDTO;
import org.folio.innreach.external.service.InnReachAuthService;
import org.folio.innreach.external.service.InnReachExternalService;
import org.folio.innreach.repository.CentralServerRepository;

@RequiredArgsConstructor
@Log4j2
@Service
public class InnReachExternalServiceImpl implements InnReachExternalService {

  public static final String DEFAULT_CENTRAL_SERVER_X_TO_CODE = "d2ir";

  private final CentralServerRepository centralServerRepository;
  private final InnReachAuthService innReachAuthService;
  private final InnReachClient innReachClient;

  @Override
  public String callInnReachApi(UUID centralServerId, String innReachRequestUri) {
    var centralServer = centralServerRepository.fetchOneWithCredentials(centralServerId)
      .orElseThrow(() -> new EntityNotFoundException("Central server with ID: " + centralServerId + " not found"));

    var accessTokenDTO = getAccessTokenForCentralServer(centralServer);

    return innReachClient.callInnReachApi(
      buildInnReachRequestUrl(centralServer, innReachRequestUri),
      buildBearerAuthorizationHeader(accessTokenDTO),
      centralServer.getLocalServerCode(),
      DEFAULT_CENTRAL_SERVER_X_TO_CODE
    );
  }

  private AccessTokenDTO getAccessTokenForCentralServer(CentralServer centralServer) {
    var centralServerCredentials = centralServer.getCentralServerCredentials();

    var accessTokenRequestDTO = new AccessTokenRequestDTO(
      centralServer.getCentralServerAddress(),
      centralServerCredentials.getCentralServerKey(),
      centralServerCredentials.getCentralServerSecret()
    );

    return innReachAuthService.getAccessToken(accessTokenRequestDTO);
  }

  private URI buildInnReachRequestUrl(CentralServer centralServer, String innReachRequestUri) {
    var innReachRequestUrl = String.format("%s/innreach/v2%s",
      centralServer.getCentralServerAddress(), innReachRequestUri);

    return URI.create(innReachRequestUrl);
  }

  private String buildBearerAuthorizationHeader(AccessTokenDTO accessTokenDTO) {
    return String.format("Bearer %s", accessTokenDTO.getAccessToken());
  }
}
