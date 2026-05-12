package org.folio.innreach.external.service.impl;

import static java.util.Collections.emptyList;
import static org.folio.innreach.external.util.AuthUtils.buildBearerAuthHeader;

import java.net.URI;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.innreach.external.exception.InnReachTimeOutException;
import org.springframework.stereotype.Service;

import org.folio.innreach.domain.dto.CentralServerConnectionDetailsDTO;
import org.folio.innreach.domain.service.CentralServerService;
import org.folio.innreach.dto.BibInfo;
import org.folio.innreach.external.client.InnReachContributionClient;
import org.folio.innreach.external.dto.BibItemsInfo;
import org.folio.innreach.external.dto.InnReachResponse;
import org.folio.innreach.external.service.InnReachAuthExternalService;
import org.folio.innreach.external.service.InnReachContributionService;
import org.springframework.web.client.ResourceAccessException;

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

    try {
      var accessTokenDTO = innReachAuthExternalService.getAccessToken(connectionDetails);
      var connectionUrl = URI.create(connectionDetails.getConnectionUrl());
      var authorizationHeader = buildBearerAuthHeader(accessTokenDTO.getAccessToken());
      var localCode = connectionDetails.getLocalCode();
      var centralCode = connectionDetails.getCentralCode();

      return contributionClient.contributeBib(connectionUrl, authorizationHeader, localCode,
        centralCode, bibId, bib);
    } catch (ResourceAccessException ex) {
      logTimeOutException("contributeBib", "bibId: " + bibId, ex);
      throw new InnReachTimeOutException("Bib contribution request to InnReach Server is timed out");
    }
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

      return contributionClient.deContributeBib(connectionUrl, authorizationHeader, localCode, centralCode, bibId);
    } catch (ResourceAccessException ex) {
      logTimeOutException("deContributeBib", "bibId: " + bibId, ex);
      throw new InnReachTimeOutException("Bib de-contribution request to InnReach Server is timed out");
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

      return contributionClient.deContributeBibItem(connectionUrl, authorizationHeader, localCode, centralCode, itemId);
    } catch (ResourceAccessException ex) {
      logTimeOutException("deContributeBibItem", "itemId: " + itemId, ex);
      throw new InnReachTimeOutException("Bib Item de-contribution request to InnReach Server is timed out");
    }
  }

  @Override
  public InnReachResponse contributeBibItems(UUID centralServerId, String bibId, BibItemsInfo bibItems) {
    log.debug("contributeBibItems:: parameters centralServerId: {}, bibId: {}, bibItems: {}", centralServerId, bibId, bibItems);
    var connectionDetails = getConnectionDetails(centralServerId);

    try {
      var accessTokenDTO = innReachAuthExternalService.getAccessToken(connectionDetails);
      var connectionUrl = URI.create(connectionDetails.getConnectionUrl());
      var authorizationHeader = buildBearerAuthHeader(accessTokenDTO.getAccessToken());
      var localCode = connectionDetails.getLocalCode();
      var centralCode = connectionDetails.getCentralCode();

      return contributionClient.contributeBibItems(connectionUrl, authorizationHeader, localCode,
        centralCode, bibId, bibItems);
    } catch (ResourceAccessException ex) {
      logTimeOutException("contributeBibItems", "bibId: " + bibId, ex);
      throw new InnReachTimeOutException("Bib Items contribution request to InnReach Server is timed out");
    }
  }

  @Override
  public InnReachResponse lookUpBib(UUID centralServerId, String bibId) {
    log.debug("lookUpBib:: parameters centralServerId: {}, bibId: {}", centralServerId, bibId);
    var connectionDetails = getConnectionDetails(centralServerId);

    try {
      var accessTokenDTO = innReachAuthExternalService.getAccessToken(connectionDetails);
      var connectionUrl = URI.create(connectionDetails.getConnectionUrl());
      var authorizationHeader = buildBearerAuthHeader(accessTokenDTO.getAccessToken());
      var localCode = connectionDetails.getLocalCode();
      var centralCode = connectionDetails.getCentralCode();

      return contributionClient.lookUpBib(connectionUrl, authorizationHeader, localCode,
        centralCode, localCode, bibId);
    } catch (ResourceAccessException ex) {
      logTimeOutException("lookUpBib", "bibId: " + bibId, ex);
      throw new InnReachTimeOutException("Look-up Bib request to InnReach Server is timed out");
    } catch (Exception e) {
      return InnReachResponse.errorResponse(e.getMessage(), emptyList());
    }
  }

  @Override
  public InnReachResponse lookUpBibItem(UUID centralServerId, String bibId, String itemId) {
    log.debug("lookUpBibItem:: parameters centralServerId: {}, bibId: {}, itemId: {}", centralServerId, bibId, itemId);
    var connectionDetails = getConnectionDetails(centralServerId);

    try {
      var accessTokenDTO = innReachAuthExternalService.getAccessToken(connectionDetails);
      var connectionUrl = URI.create(connectionDetails.getConnectionUrl());
      var authorizationHeader = buildBearerAuthHeader(accessTokenDTO.getAccessToken());
      var localCode = connectionDetails.getLocalCode();
      var centralCode = connectionDetails.getCentralCode();

      return contributionClient.lookUpBibItem(connectionUrl, authorizationHeader, localCode,
        centralCode, localCode, bibId, itemId);
    } catch (ResourceAccessException ex) {
      logTimeOutException("lookUpBibItem", "bibId: " + bibId + " itemId: " + itemId, ex);
      throw new InnReachTimeOutException("Look-up Bib Item request to InnReach Server is timed out");
    } catch (Exception e) {
      return InnReachResponse.errorResponse(e.getMessage(), emptyList());
    }
  }

  private CentralServerConnectionDetailsDTO getConnectionDetails(UUID centralServerId) {
    return centralServerService.getCentralServerConnectionDetails(centralServerId);
  }

  private void logTimeOutException(String methodName, String resource, Exception ex) {
    Throwable cause = ex.getCause();

    if (cause instanceof java.net.ConnectException || cause instanceof java.net.http.HttpConnectTimeoutException) {
      // connect timeout
      log.error("{}:: Connection Time-Out occurred while processing {}, error: {}", methodName, resource, ex.getMessage());
    } else if (cause instanceof java.net.SocketTimeoutException || cause instanceof java.net.http.HttpTimeoutException) {
      // read timeout
      log.error("{}:: Read Time-Out occurred while processing {}, error: {}", methodName, resource, ex.getMessage());
    } else {
      log.error("{}:: ResourceAccessException occurred while processing {}, error: {}", methodName, resource, ex.getMessage());
    }
  }
}
