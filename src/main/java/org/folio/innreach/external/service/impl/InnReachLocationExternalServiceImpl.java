package org.folio.innreach.external.service.impl;

import static org.folio.innreach.external.util.AuthUtils.buildBearerAuthHeader;

import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import org.folio.innreach.domain.dto.CentralServerConnectionDetailsDTO;
import org.folio.innreach.external.InnReachHeaders;
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

  @Override
  public void updateAllLocations(CentralServerConnectionDetailsDTO connectionDetails,
      List<InnReachLocationDTO> updatedLocations) {
    var accessTokenDTO = innReachAuthExternalService.getAccessToken(connectionDetails);
    var connectionUrl = URI.create(connectionDetails.getConnectionUrl());
    var authorizationHeader = buildBearerAuthHeader(accessTokenDTO.getAccessToken());
    var localCode = connectionDetails.getLocalCode();

    var currentLocations = innReachLocationClient
      .getAllLocations(connectionUrl, authorizationHeader, localCode, InnReachHeaders.X_TO_CODE_DEFAULT_VALUE)
      .getLocationList();

    if (currentLocations.isEmpty()) {
      innReachLocationClient.addAllLocations(connectionUrl, authorizationHeader, localCode,
        InnReachHeaders.X_TO_CODE_DEFAULT_VALUE, new InnReachLocationsDTO(updatedLocations));
    } else {
      doUpdate(connectionUrl, authorizationHeader, localCode, currentLocations, updatedLocations);
    }
  }

  private void doUpdate(URI centralServerConnectionUrl, String authorizationHeader, String localCode,
      List<InnReachLocationDTO> currentLocations, List<InnReachLocationDTO> updatedLocations) {

    var updatedLocationsMap = updatedLocations.stream()
      .collect(Collectors.toMap(InnReachLocationDTO::getCode, locationDTO -> locationDTO));

    currentLocations.forEach(currentLocation -> {
      var updatedLocation = updatedLocationsMap.get(currentLocation.getCode());

      if (updatedLocation == null) {
        innReachLocationClient.deleteLocation(centralServerConnectionUrl, authorizationHeader, localCode,
            InnReachHeaders.X_TO_CODE_DEFAULT_VALUE, currentLocation.getCode());

      } else if (!updatedLocation.getDescription().equals(currentLocation.getDescription())) {
        innReachLocationClient.updateLocation(centralServerConnectionUrl, authorizationHeader, localCode,
            InnReachHeaders.X_TO_CODE_DEFAULT_VALUE, currentLocation.getCode(), currentLocation);
      }

      updatedLocationsMap.remove(currentLocation.getCode());
    });

    updatedLocationsMap.values().forEach(updatedLocation -> innReachLocationClient.addLocation(
        centralServerConnectionUrl, authorizationHeader, localCode, InnReachHeaders.X_TO_CODE_DEFAULT_VALUE,
        updatedLocation.getCode(), updatedLocation));
  }

}
