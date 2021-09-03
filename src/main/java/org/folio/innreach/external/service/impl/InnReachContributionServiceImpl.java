package org.folio.innreach.external.service.impl;

import static org.folio.innreach.external.util.AuthUtils.buildBearerAuthHeader;

import java.net.URI;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import org.folio.innreach.domain.service.CentralServerService;
import org.folio.innreach.external.InnReachHeaders;
import org.folio.innreach.external.client.feign.InnReachContributionClient;
import org.folio.innreach.external.dto.BibContributionRequest;
import org.folio.innreach.external.dto.InnReachResponse;
import org.folio.innreach.external.service.InnReachAuthExternalService;
import org.folio.innreach.external.service.InnReachContributionService;

@RequiredArgsConstructor
@Log4j2
@Service
public class InnReachContributionServiceImpl implements InnReachContributionService {

  private final InnReachContributionClient contributionClient;
  private final InnReachAuthExternalService innReachAuthExternalService;
  private final CentralServerService centralServerService;

  @Override
  public InnReachResponse contributeBib(UUID centralServerId, String bibId, BibContributionRequest bib) {
    var connectionDetails = centralServerService.getCentralServerConnectionDetails(centralServerId);

    var accessTokenDTO = innReachAuthExternalService.getAccessToken(connectionDetails);
    var connectionUrl = URI.create(connectionDetails.getConnectionUrl());
    var authorizationHeader = buildBearerAuthHeader(accessTokenDTO.getAccessToken());
    var localCode = connectionDetails.getLocalCode();

    return contributionClient.contributeBib(connectionUrl, authorizationHeader, localCode, InnReachHeaders.X_TO_CODE_DEFAULT_VALUE, bibId, bib);
  }

  @Override
  public InnReachResponse lookUpBib(UUID centralServerId, String bibId) {
    var connectionDetails = centralServerService.getCentralServerConnectionDetails(centralServerId);

    var accessTokenDTO = innReachAuthExternalService.getAccessToken(connectionDetails);
    var connectionUrl = URI.create(connectionDetails.getConnectionUrl());
    var authorizationHeader = buildBearerAuthHeader(accessTokenDTO.getAccessToken());
    var localCode = connectionDetails.getLocalCode();

    return contributionClient.lookUpBib(connectionUrl, authorizationHeader, localCode, InnReachHeaders.X_TO_CODE_DEFAULT_VALUE, localCode, bibId);
  }
}
