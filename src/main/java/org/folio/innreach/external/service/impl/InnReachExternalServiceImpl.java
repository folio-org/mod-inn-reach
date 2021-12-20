package org.folio.innreach.external.service.impl;

import static org.folio.innreach.external.util.AuthUtils.buildBearerAuthHeader;

import java.net.URI;
import java.time.OffsetDateTime;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import org.folio.innreach.domain.service.CentralServerService;
import org.folio.innreach.external.client.feign.InnReachClient;
import org.folio.innreach.external.service.InnReachAuthExternalService;
import org.folio.innreach.external.service.InnReachExternalService;

@RequiredArgsConstructor
@Log4j2
@Service
public class InnReachExternalServiceImpl implements InnReachExternalService {

  private final CentralServerService centralServerService;
  private final InnReachAuthExternalService innReachAuthExternalService;
  private final InnReachClient innReachClient;

  @Override
  public String callInnReachApi(UUID centralServerId, String innReachRequestUri) {
    var connectionDetailsDTO = centralServerService.getCentralServerConnectionDetails(centralServerId);

    var accessTokenDTO = innReachAuthExternalService.getAccessToken(connectionDetailsDTO);

    return innReachClient.callInnReachApi(
      buildInnReachRequestUrl(connectionDetailsDTO.getConnectionUrl(), innReachRequestUri),
      buildBearerAuthHeader(accessTokenDTO.getAccessToken()),
      connectionDetailsDTO.getLocalCode(),
      connectionDetailsDTO.getCentralCode(),
      OffsetDateTime.now().toEpochSecond()
    );
  }

  @Override
  public String postInnReachApi(String centralCode, String innReachRequestUri, Object payload) {
    var connectionDetails = centralServerService.getConnectionDetailsByCode(centralCode);

    var accessTokenDTO = innReachAuthExternalService.getAccessToken(connectionDetails);

    return innReachClient.postInnReachApi(
      buildInnReachRequestUrl(connectionDetails.getConnectionUrl(), innReachRequestUri),
      buildBearerAuthHeader(accessTokenDTO.getAccessToken()),
      connectionDetails.getLocalCode(),
      connectionDetails.getCentralCode(),
      OffsetDateTime.now().toEpochSecond(),
      payload
    );
  }

  @Override
  public String postInnReachApi(String centralCode, String innReachRequestUri) {
    var connectionDetails = centralServerService.getConnectionDetailsByCode(centralCode);

    var accessTokenDTO = innReachAuthExternalService.getAccessToken(connectionDetails);

    return innReachClient.postInnReachApi(
      buildInnReachRequestUrl(connectionDetails.getConnectionUrl(), innReachRequestUri),
      buildBearerAuthHeader(accessTokenDTO.getAccessToken()),
      connectionDetails.getLocalCode(),
      connectionDetails.getCentralCode(),
      OffsetDateTime.now().toEpochSecond()
    );
  }

  private URI buildInnReachRequestUrl(String centralServerUrl, String innReachRequestUri) {
    var innReachRequestUrl = String.format("%s/innreach/v2%s", centralServerUrl, innReachRequestUri);
    return URI.create(innReachRequestUrl);
  }
}
