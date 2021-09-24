package org.folio.innreach.external.service.impl;

import static org.folio.innreach.external.util.AuthUtils.buildBearerAuthHeader;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import org.folio.innreach.domain.dto.CentralServerConnectionDetailsDTO;
import org.folio.innreach.domain.service.CentralServerService;
import org.folio.innreach.external.client.feign.InnReachContributionClient;
import org.folio.innreach.external.dto.BibItem;
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
  public InnReachResponse contributeBib(UUID centralServerId, String bibId, Bib bib) {
    var connectionDetails = getConnectionDetails(centralServerId);

    var accessTokenDTO = innReachAuthExternalService.getAccessToken(connectionDetails);
    var connectionUrl = URI.create(connectionDetails.getConnectionUrl());
    var authorizationHeader = buildBearerAuthHeader(accessTokenDTO.getAccessToken());
    var localCode = connectionDetails.getLocalCode();
    var centralCode = connectionDetails.getCentralCode();

    return contributionClient.contributeBib(connectionUrl, authorizationHeader, localCode,
      centralCode, bibId, bib);
  }

  @Override
  public InnReachResponse contributeBibItems(UUID centralServerId, String bibId, List<BibItem> bibItems) {
    var connectionDetails = getConnectionDetails(centralServerId);

    var accessTokenDTO = innReachAuthExternalService.getAccessToken(connectionDetails);
    var connectionUrl = URI.create(connectionDetails.getConnectionUrl());
    var authorizationHeader = buildBearerAuthHeader(accessTokenDTO.getAccessToken());
    var localCode = connectionDetails.getLocalCode();
    var centralCode = connectionDetails.getCentralCode();

    return contributionClient.contributeBibItems(connectionUrl, authorizationHeader, localCode,
      centralCode, bibId, bibItems);
  }

  @Override
  public InnReachResponse lookUpBib(UUID centralServerId, String bibId) {
    var connectionDetails = getConnectionDetails(centralServerId);

    var accessTokenDTO = innReachAuthExternalService.getAccessToken(connectionDetails);
    var connectionUrl = URI.create(connectionDetails.getConnectionUrl());
    var authorizationHeader = buildBearerAuthHeader(accessTokenDTO.getAccessToken());
    var localCode = connectionDetails.getLocalCode();
    var centralCode = connectionDetails.getCentralCode();

    return contributionClient.lookUpBib(connectionUrl, authorizationHeader, localCode,
      centralCode, localCode, bibId);
  }

  private CentralServerConnectionDetailsDTO getConnectionDetails(UUID centralServerId) {
    return centralServerService.getCentralServerConnectionDetails(centralServerId);
  }
}
