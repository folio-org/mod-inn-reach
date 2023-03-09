package org.folio.innreach.external.service.impl;

import static java.util.Collections.emptyList;

import static org.folio.innreach.external.util.AuthUtils.buildBearerAuthHeader;

import java.net.URI;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.innreach.external.exception.ServiceSuspendedException;
import org.springframework.stereotype.Service;

import org.folio.innreach.domain.dto.CentralServerConnectionDetailsDTO;
import org.folio.innreach.domain.service.CentralServerService;
import org.folio.innreach.dto.BibInfo;
import org.folio.innreach.external.client.feign.InnReachContributionClient;
import org.folio.innreach.external.dto.BibItemsInfo;
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
  public InnReachResponse contributeBib(UUID centralServerId, String bibId, BibInfo bib) {
    log.debug("contributeBib:: parameters centralServerId: {}, bibId: {}, bib: {}", centralServerId, bibId, bib);
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
  public InnReachResponse deContributeBib(UUID centralServerId, String bibId) {
    try {
      log.debug("deContributeBib:: parameters centralServerId: {}, bibId: {}", centralServerId, bibId);
      var connectionDetails = getConnectionDetails(centralServerId);

      var accessTokenDTO = innReachAuthExternalService.getAccessToken(connectionDetails);
      var connectionUrl = URI.create(connectionDetails.getConnectionUrl());
      var authorizationHeader = buildBearerAuthHeader(accessTokenDTO.getAccessToken());
      var localCode = connectionDetails.getLocalCode();
      var centralCode = connectionDetails.getCentralCode();

      var response = contributionClient.deContributeBib(connectionUrl, authorizationHeader, localCode,
        centralCode, bibId);
      if (response!=null && response.getErrors()!=null && !response.getErrors().isEmpty()) {
        InnReachResponse.Error errorResponse = response.getErrors().get(0);
        var error = errorResponse!=null ? errorResponse.getReason() : "";
        log.info("checkServiceSuspension:: error is : {}",error);
        if (error.contains("Contribution to d2irm is currently suspended")) {
          throw new ServiceSuspendedException("Contribution to d2irm is currently suspended");
        }
      }
      return  response;
    } catch (ServiceSuspendedException ex) {
      throw new ServiceSuspendedException(ex.getMessage());
    }
  }

  @Override
  public InnReachResponse deContributeBibItem(UUID centralServerId, String itemId) {
    try {
      log.debug("deContributeBibItem:: parameters centralServerId: {}, itemId: {}", centralServerId, itemId);
      var connectionDetails = getConnectionDetails(centralServerId);

      var accessTokenDTO = innReachAuthExternalService.getAccessToken(connectionDetails);
      var connectionUrl = URI.create(connectionDetails.getConnectionUrl());
      var authorizationHeader = buildBearerAuthHeader(accessTokenDTO.getAccessToken());
      var localCode = connectionDetails.getLocalCode();
      var centralCode = connectionDetails.getCentralCode();

      var response = contributionClient.deContributeBibItem(connectionUrl, authorizationHeader, localCode,
        centralCode, itemId);
      if (!response.getErrors().isEmpty()) {
        var error = response.getErrors().get(0).getReason();
        if (error.contains("Contribution to d2irm is currently suspended")) {
          throw new ServiceSuspendedException("Contribution to d2irm is currently suspended");
        }
      }
      return response;
    } catch (ServiceSuspendedException ex) {
      throw new ServiceSuspendedException(ex.getMessage());
    }
  }

  @Override
  public InnReachResponse contributeBibItems(UUID centralServerId, String bibId, BibItemsInfo bibItems) {
    log.debug("contributeBibItems:: parameters centralServerId: {}, bibId: {}, bibItems: {}", centralServerId, bibId, bibItems);
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
    log.debug("lookUpBib:: parameters centralServerId: {}, bibId: {}", centralServerId, bibId);
    var connectionDetails = getConnectionDetails(centralServerId);

    var accessTokenDTO = innReachAuthExternalService.getAccessToken(connectionDetails);
    var connectionUrl = URI.create(connectionDetails.getConnectionUrl());
    var authorizationHeader = buildBearerAuthHeader(accessTokenDTO.getAccessToken());
    var localCode = connectionDetails.getLocalCode();
    var centralCode = connectionDetails.getCentralCode();

    try {
      return contributionClient.lookUpBib(connectionUrl, authorizationHeader, localCode,
        centralCode, localCode, bibId);
    } catch (Exception e) {
      return InnReachResponse.errorResponse(e.getMessage(), emptyList());
    }
  }

  @Override
  public InnReachResponse lookUpBibItem(UUID centralServerId, String bibId, String itemId) {
    log.debug("lookUpBibItem:: parameters centralServerId: {}, bibId: {}, itemId: {}", centralServerId, bibId, itemId);
    var connectionDetails = getConnectionDetails(centralServerId);

    var accessTokenDTO = innReachAuthExternalService.getAccessToken(connectionDetails);
    var connectionUrl = URI.create(connectionDetails.getConnectionUrl());
    var authorizationHeader = buildBearerAuthHeader(accessTokenDTO.getAccessToken());
    var localCode = connectionDetails.getLocalCode();
    var centralCode = connectionDetails.getCentralCode();

    try {
      return contributionClient.lookUpBibItem(connectionUrl, authorizationHeader, localCode,
        centralCode, localCode, bibId, itemId);
    } catch (Exception e) {
      return InnReachResponse.errorResponse(e.getMessage(), emptyList());
    }
  }

  private CentralServerConnectionDetailsDTO getConnectionDetails(UUID centralServerId) {
    return centralServerService.getCentralServerConnectionDetails(centralServerId);
  }
}
