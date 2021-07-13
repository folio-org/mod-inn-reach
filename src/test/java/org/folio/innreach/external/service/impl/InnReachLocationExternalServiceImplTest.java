package org.folio.innreach.external.service.impl;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import static org.folio.innreach.fixture.AccessTokenFixture.createAccessToken;
import static org.folio.innreach.fixture.CentralServerFixture.createCentralServerConnectionDetailsDTO;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import org.folio.innreach.external.client.feign.InnReachLocationClient;
import org.folio.innreach.external.dto.InnReachLocationDTO;
import org.folio.innreach.external.dto.InnReachLocationsDTO;
import org.folio.innreach.external.service.InnReachAuthExternalService;

class InnReachLocationExternalServiceImplTest {

  @Mock
  private InnReachLocationClient innReachLocationClient;

  @Mock
  private InnReachAuthExternalService innReachAuthExternalService;

  @InjectMocks
  private InnReachLocationExternalServiceImpl innReachLocationExternalService;

  @BeforeEach
  public void setUp(){
    MockitoAnnotations.initMocks(this);
  }

  @Test
  void addAllLocationsToInnReach_when_thereAreNoSubmittedLocationsToInnReach() {
    when(innReachAuthExternalService.getAccessToken(any())).thenReturn(createAccessToken());

    when(innReachLocationClient.getAllLocations(any(), any(), any(), any()))
      .thenReturn(new InnReachLocationsDTO(Collections.emptyList()));

    innReachLocationExternalService.updateAllLocations(createCentralServerConnectionDetailsDTO(), List.of(
      new InnReachLocationDTO("qwe12", "qwe12 description")
    ));

    verify(innReachLocationClient).addAllLocations(any(), any(), any(), any(), any());
    verify(innReachLocationClient, never()).addLocation(any(), any(), any(), any(), any(), any());
    verify(innReachLocationClient, never()).deleteLocation(any(), any(), any(), any(), any());
    verify(innReachLocationClient, never()).updateLocation(any(), any(), any(), any(), any(), any());
  }

  @Test
  void deleteLocationFromInnReach_when_locationMappingIsDeletedFromFolio() {
    when(innReachAuthExternalService.getAccessToken(any())).thenReturn(createAccessToken());

    when(innReachLocationClient.getAllLocations(any(), any(), any(), any()))
      .thenReturn(new InnReachLocationsDTO(List.of(
        new InnReachLocationDTO("qwe12", "qwe12 description")
      )));

    innReachLocationExternalService.updateAllLocations(createCentralServerConnectionDetailsDTO(), List.of(
      new InnReachLocationDTO("asd12", "asd12 description")
    ));

    verify(innReachLocationClient).deleteLocation(any(), any(), any(), any(), any());
    verify(innReachLocationClient).addLocation(any(), any(), any(), any(), any(), any());
    verify(innReachLocationClient, never()).addAllLocations(any(), any(), any(), any(), any());
    verify(innReachLocationClient, never()).updateLocation(any(), any(), any(), any(), any(), any());
  }

  @Test
  void updateLocation_when_locationIsModifiedOnFolioSide() {
    when(innReachAuthExternalService.getAccessToken(any())).thenReturn(createAccessToken());

    when(innReachLocationClient.getAllLocations(any(), any(), any(), any()))
      .thenReturn(new InnReachLocationsDTO(List.of(
        new InnReachLocationDTO("qwe12", "qwe12 description")
      )));

    innReachLocationExternalService.updateAllLocations(createCentralServerConnectionDetailsDTO(), List.of(
      new InnReachLocationDTO("qwe12", "qwe12 modified description")
    ));

    verify(innReachLocationClient).updateLocation(any(), any(), any(), any(), any(), any());
    verify(innReachLocationClient, never()).deleteLocation(any(), any(), any(), any(), any());
    verify(innReachLocationClient, never()).addLocation(any(), any(), any(), any(), any(), any());
    verify(innReachLocationClient, never()).addAllLocations(any(), any(), any(), any(), any());
  }
}
