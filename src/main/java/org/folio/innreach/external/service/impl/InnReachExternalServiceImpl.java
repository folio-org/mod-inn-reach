package org.folio.innreach.external.service.impl;

import static org.folio.innreach.external.util.AuthUtils.buildBearerAuthHeader;

import java.net.URI;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import org.folio.innreach.domain.service.CentralServerService;
import org.folio.innreach.external.InnReachHeaders;
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
      connectionDetailsDTO.getCentralCode()
    );
  }

  private URI buildInnReachRequestUrl(String centralServerUrl, String innReachRequestUri) {
    var innReachRequestUrl = String.format("%s/innreach/v2%s", centralServerUrl, innReachRequestUri);
    return URI.create(innReachRequestUrl);
  }
}
