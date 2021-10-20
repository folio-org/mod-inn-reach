package org.folio.innreach.domain.service.impl;

import static java.util.Collections.emptyList;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;

import static org.folio.innreach.external.dto.InnReachResponse.OK_STATUS;
import static org.folio.innreach.util.ListUtils.flatMapItems;
import static org.folio.innreach.util.ListUtils.mapItemsWithFilter;
import static org.folio.innreach.util.ListUtils.toStream;

import java.util.List;
import java.util.Objects;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.folio.innreach.domain.service.AgencyService;
import org.folio.innreach.domain.service.CentralServerService;
import org.folio.innreach.dto.AgenciesPerCentralServerDTO;
import org.folio.innreach.dto.Agency;
import org.folio.innreach.dto.CentralServerAgenciesDTO;
import org.folio.innreach.dto.CentralServerDTO;
import org.folio.innreach.dto.InnReachResponseDTO;
import org.folio.innreach.dto.LocalServerAgenciesDTO;
import org.folio.innreach.external.exception.InnReachException;
import org.folio.innreach.external.service.InnReachExternalService;
import org.folio.innreach.util.JsonHelper;

@Log4j2
@RequiredArgsConstructor
@Service
public class AgencyServiceImpl implements AgencyService {

  private static final String INN_REACH_LOCAL_SERVERS_URI = "/contribution/localservers";

  private final CentralServerService centralServerService;
  private final InnReachExternalService innReachService;
  private final JsonHelper jsonHelper;


  @Override
  @Transactional(readOnly = true)
  public CentralServerAgenciesDTO getAllAgencies() {
    var servers = centralServerService.getAllCentralServers(0, Integer.MAX_VALUE).getCentralServers();

    var agencies = mapItemsWithFilter(servers, this::retrieveAllAgenciesFromCentralServer, Objects::nonNull);

    return new CentralServerAgenciesDTO()
        .centralServerAgencies(agencies)
        .totalRecords(agencies.size());
  }

  private AgenciesPerCentralServerDTO retrieveAllAgenciesFromCentralServer(CentralServerDTO centralServer) {
    log.info("Retrieving local server agencies from central server: code = {}", centralServer.getCentralServerCode());

    LocalServerAgenciesDTO lsaResponse;
    try {
      String response = innReachService.callInnReachApi(centralServer.getId(), INN_REACH_LOCAL_SERVERS_URI);

      lsaResponse = jsonHelper.fromJson(response, LocalServerAgenciesDTO.class);
    } catch (InnReachException | BadCredentialsException | IllegalStateException e) {
      log.warn("Failed to get local server agencies from central server: code = {}",
          centralServer.getCentralServerCode(), e);

      return null;
    }

    List<Agency> agencies = emptyList();
    if (isOk(lsaResponse)) {
      agencies = flatMapItems(lsaResponse.getLocalServerList(), localServer -> toStream(localServer.getAgencyList()));
    } else {
      log.warn("Failed to get local server agencies from central server: code = {}. Inn-reach response: {}",
          centralServer.getCentralServerCode(), lsaResponse);
    }

    log.info("Number of agencies received: {}", agencies.size());

    return isNotEmpty(agencies) ? createAgencies(centralServer, agencies) : null;
  }

  private AgenciesPerCentralServerDTO createAgencies(CentralServerDTO centralServer, List<Agency> agencies) {
    return new AgenciesPerCentralServerDTO()
        .centralServerId(centralServer.getId())
        .centralServerCode(centralServer.getCentralServerCode())
        .agencies(agencies);
  }

  private static boolean isOk(InnReachResponseDTO innReachResponse) {
    return OK_STATUS.equals(innReachResponse.getStatus()) && CollectionUtils.isEmpty(innReachResponse.getErrors());
  }

}
