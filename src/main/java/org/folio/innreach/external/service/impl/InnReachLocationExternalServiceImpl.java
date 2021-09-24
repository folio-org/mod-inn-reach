package org.folio.innreach.external.service.impl;

import static org.folio.innreach.external.util.AuthUtils.buildBearerAuthHeader;

import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import org.folio.innreach.domain.dto.CentralServerConnectionDetailsDTO;
import org.folio.innreach.external.client.feign.InnReachLocationClient;
import org.folio.innreach.external.dto.InnReachLocationDTO;
import org.folio.innreach.external.dto.InnReachLocationsDTO;
import org.folio.innreach.external.service.InnReachAuthExternalService;
import org.folio.innreach.external.service.InnReachLocationExternalService;

@RequiredArgsConstructor
@Log4j2
@Service
public class InnReachLocationExternalServiceImpl implements InnReachLocationExternalService {

  private final InnReachLocationClient innReachLocationClient;
  private final InnReachAuthExternalService innReachAuthExternalService;

  @Async
  @Override
  public void submitMappedLocationsToInnReach(CentralServerConnectionDetailsDTO connectionDetails,
                                              List<InnReachLocationDTO> actualMappedLocations) {
    log.debug("Start submitting CentralServer [{}] mapped locations to InnReach API", connectionDetails.getLocalCode());

    var accessTokenDTO = innReachAuthExternalService.getAccessToken(connectionDetails);
    var connectionUrl = URI.create(connectionDetails.getConnectionUrl());
    var authorizationHeader = buildBearerAuthHeader(accessTokenDTO.getAccessToken());
    var localCode = connectionDetails.getLocalCode();
    var centralCode = connectionDetails.getCentralCode();

    var currentMappedLocations = getMappedLocationsFromInnReach(connectionUrl, authorizationHeader, localCode, centralCode);

    if (currentMappedLocations.isEmpty()) {
      log.debug("There are no mapped locations for CentralServer [{}] submitted to InnReach API",
        connectionDetails.getLocalCode());

      submitAllLocationsToInnReach(connectionDetails, actualMappedLocations, connectionUrl, authorizationHeader, localCode, centralCode);
    } else {
      log.debug("There are mapped locations for CentralServer [{}] submitted to InnReach API",
        connectionDetails.getLocalCode());

      doUpdate(connectionUrl, authorizationHeader, localCode, centralCode, currentMappedLocations, actualMappedLocations);
    }
    log.debug("CentralServer [{}] mapped locations submitted to InnReach API", connectionDetails.getLocalCode());

  }

  @Override
  public List<InnReachLocationDTO> getAllLocations(CentralServerConnectionDetailsDTO connectionDetails) {
    var accessTokenDTO = innReachAuthExternalService.getAccessToken(connectionDetails);
    var connectionUrl = URI.create(connectionDetails.getConnectionUrl());
    var authorizationHeader = buildBearerAuthHeader(accessTokenDTO.getAccessToken());
    var localCode = connectionDetails.getLocalCode();
    var centralCode = connectionDetails.getCentralCode();

    return getMappedLocationsFromInnReach(connectionUrl, authorizationHeader, localCode, centralCode);
  }

  private List<InnReachLocationDTO> getMappedLocationsFromInnReach(URI connectionUrl, String authorizationHeader,
                                                                   String localCode, String centralCode) {
    return innReachLocationClient.getAllLocations(connectionUrl, authorizationHeader, localCode, centralCode)
      .getLocationList();
  }

  private void submitAllLocationsToInnReach(CentralServerConnectionDetailsDTO connectionDetails,
                                            List<InnReachLocationDTO> actualMappedLocations,
                                            URI connectionUrl, String authorizationHeader, String localCode, String centralCode) {
    log.debug("Submit all CentralServer [{}] mapped locations to InnReach API", connectionDetails.getLocalCode());

    innReachLocationClient.addAllLocations(connectionUrl, authorizationHeader, localCode,
      centralCode, new InnReachLocationsDTO(actualMappedLocations));
  }

  private void doUpdate(URI centralServerConnectionUrl, String authorizationHeader, String localCode, String centralCode,
                        List<InnReachLocationDTO> currentLocations, List<InnReachLocationDTO> updatedLocations) {
    var locationCodeToLocationMap = updatedLocations.stream()
      .collect(Collectors.toMap(InnReachLocationDTO::getCode, locationDTO -> locationDTO));

    currentLocations.forEach(currentLocation -> {
      var updatedLocation = locationCodeToLocationMap.get(currentLocation.getCode());

      if (updatedLocation == null) {
        deleteLocationFromInnReach(centralServerConnectionUrl, authorizationHeader, localCode, centralCode, currentLocation);
      } else if (!updatedLocation.getDescription().equals(currentLocation.getDescription())) {
        submitUpdatedLocationToInnReach(centralServerConnectionUrl, authorizationHeader, localCode, centralCode, updatedLocation);
      }
      locationCodeToLocationMap.remove(currentLocation.getCode());
    });

    locationCodeToLocationMap.values().forEach(newLocation -> submitNewLocationToInnReach(centralServerConnectionUrl,
      authorizationHeader, localCode, centralCode, newLocation));
  }

  private void deleteLocationFromInnReach(URI centralServerConnectionUrl, String authorizationHeader, String localCode, String centralCode,
                                          InnReachLocationDTO deletedLocation) {
    log.debug("Delete CentralServer [{}] mapped location [{}] from InnReach API", localCode, deletedLocation.getCode());

    innReachLocationClient.deleteLocation(centralServerConnectionUrl, authorizationHeader, localCode,
      centralCode, deletedLocation.getCode());
  }

  private void submitUpdatedLocationToInnReach(URI centralServerConnectionUrl, String authorizationHeader,
                                               String localCode, String centralCode, InnReachLocationDTO updatedLocation) {
    log.debug("Submit updated CentralServer [{}] mapped location [{}] to InnReach API", localCode, updatedLocation
      .getCode());

    innReachLocationClient.updateLocation(centralServerConnectionUrl, authorizationHeader, localCode,
      centralCode, updatedLocation.getCode(), updatedLocation);
  }

  private void submitNewLocationToInnReach(URI centralServerConnectionUrl, String authorizationHeader, String localCode,
                                           String centralCode, InnReachLocationDTO newLocation) {
    log.debug("Submit the new CentralServer [{}] mapped location [{}] to InnReach API", localCode, newLocation.getCode());

    innReachLocationClient.addLocation(centralServerConnectionUrl, authorizationHeader, localCode,
      centralCode, newLocation.getCode(), newLocation);
  }

}
