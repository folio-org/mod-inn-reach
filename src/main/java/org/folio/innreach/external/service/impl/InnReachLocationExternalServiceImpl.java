package org.folio.innreach.external.service.impl;

import static org.folio.innreach.external.util.AuthUtils.buildBearerAuthHeader;

import java.net.URI;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.innreach.util.UriHelper;
import org.springframework.stereotype.Service;

import org.folio.innreach.domain.dto.CentralServerConnectionDetailsDTO;
import org.folio.innreach.external.client.InnReachLocationClient;
import org.folio.innreach.external.dto.InnReachLocationDTO;
import org.folio.innreach.external.dto.InnReachLocationsDTO;
import org.folio.innreach.external.service.InnReachAuthExternalService;
import org.folio.innreach.external.service.InnReachLocationExternalService;

@RequiredArgsConstructor
@Log4j2
@Service
public class InnReachLocationExternalServiceImpl implements InnReachLocationExternalService {

  private static final String LOCATIONS_COLLECTION_PATH = "/innreach/v2/contribution/locations";
  private static final String LOCATION_ITEM_PATH = "/innreach/v2/location/{locationKey}";

  private final InnReachLocationClient innReachLocationClient;
  private final InnReachAuthExternalService innReachAuthExternalService;

  @Override
  public void submitMappedLocationsToInnReach(CentralServerConnectionDetailsDTO connectionDetails,
                                              List<InnReachLocationDTO> actualMappedLocations) {
    log.info("Start submitting local server [{}] mapped locations to INN-Reach", connectionDetails.getLocalCode());

    var accessTokenDTO = innReachAuthExternalService.getAccessToken(connectionDetails);
    var connectionUrl = URI.create(connectionDetails.getConnectionUrl());
    var authorizationHeader = buildBearerAuthHeader(accessTokenDTO.getAccessToken());
    var localCode = connectionDetails.getLocalCode();
    var centralCode = connectionDetails.getCentralCode();

    var currentMappedLocations = getMappedLocationsFromInnReach(connectionUrl, authorizationHeader, localCode, centralCode);

    if (currentMappedLocations.isEmpty()) {
      log.info("There are no mapped locations for local server [{}] submitted to INN-Reach",
        connectionDetails.getLocalCode());

      submitAllLocationsToInnReach(connectionDetails, actualMappedLocations, connectionUrl, authorizationHeader, localCode, centralCode);
    } else {
      log.info("There are mapped locations for local server [{}] submitted to INN-Reach",
        connectionDetails.getLocalCode());

      doUpdate(connectionUrl, authorizationHeader, localCode, centralCode, currentMappedLocations, actualMappedLocations);
    }
    log.info("Local server [{}] mapped locations submitted to INN-Reach", connectionDetails.getLocalCode());
  }

  @Override
  public List<InnReachLocationDTO> getAllLocations(CentralServerConnectionDetailsDTO connectionDetails) {
    log.debug("getAllLocations:: parameters connectionDetails: {}", connectionDetails);
    var accessTokenDTO = innReachAuthExternalService.getAccessToken(connectionDetails);
    var connectionUrl = URI.create(connectionDetails.getConnectionUrl());
    var authorizationHeader = buildBearerAuthHeader(accessTokenDTO.getAccessToken());
    var localCode = connectionDetails.getLocalCode();
    var centralCode = connectionDetails.getCentralCode();

    return getMappedLocationsFromInnReach(connectionUrl, authorizationHeader, localCode, centralCode);
  }

  private List<InnReachLocationDTO> getMappedLocationsFromInnReach(URI connectionUrl, String authorizationHeader,
                                                                   String localCode, String centralCode) {
    var fullUri = UriHelper.buildUri(connectionUrl, LOCATIONS_COLLECTION_PATH);
    return innReachLocationClient.getAllLocations(fullUri, authorizationHeader, localCode, centralCode)
      .getLocationList();
  }

  private void submitAllLocationsToInnReach(CentralServerConnectionDetailsDTO connectionDetails,
                                            List<InnReachLocationDTO> actualMappedLocations,
                                            URI connectionUrl, String authorizationHeader, String localCode, String centralCode) {
    log.info("Submit all local server [{}] mapped locations to INN-Reach", connectionDetails.getLocalCode());

    var fullUri = UriHelper.buildUri(connectionUrl, LOCATIONS_COLLECTION_PATH);
    innReachLocationClient.addAllLocations(fullUri, authorizationHeader, localCode,
      centralCode, new InnReachLocationsDTO(actualMappedLocations));
  }

  private void doUpdate(URI centralServerConnectionUrl, String authorizationHeader, String localCode, String centralCode,
                        List<InnReachLocationDTO> currentLocations, List<InnReachLocationDTO> updatedLocations) {
    var locationCodeToLocationMap = updatedLocations.stream()
      .collect(Collectors.toMap(InnReachLocationDTO::getCode, Function.identity()));

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
    log.info("Delete local server [{}] mapped location [{}] from INN-Reach", localCode, deletedLocation.getCode());

    var fullUri = UriHelper.buildUri(centralServerConnectionUrl, LOCATION_ITEM_PATH, deletedLocation.getCode());
    innReachLocationClient.deleteLocation(fullUri, authorizationHeader, localCode, centralCode);
  }

  private void submitUpdatedLocationToInnReach(URI centralServerConnectionUrl, String authorizationHeader,
                                               String localCode, String centralCode, InnReachLocationDTO updatedLocation) {
    log.info("Submit updated local server [{}] mapped location [{}] to INN-Reach", localCode, updatedLocation
      .getCode());

    var fullUri = UriHelper.buildUri(centralServerConnectionUrl, LOCATION_ITEM_PATH, updatedLocation.getCode());
    innReachLocationClient.updateLocation(fullUri, authorizationHeader, localCode, centralCode, updatedLocation);
  }

  private void submitNewLocationToInnReach(URI centralServerConnectionUrl, String authorizationHeader, String localCode,
                                           String centralCode, InnReachLocationDTO newLocation) {
    log.info("Submit the new local server [{}] mapped location [{}] to INN-Reach", localCode, newLocation.getCode());

    var fullUri = UriHelper.buildUri(centralServerConnectionUrl, LOCATION_ITEM_PATH, newLocation.getCode());
    innReachLocationClient.addLocation(fullUri, authorizationHeader, localCode, centralCode, newLocation);
  }
}
