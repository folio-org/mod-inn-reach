package org.folio.innreach.domain.service.impl;

import static org.folio.innreach.fixture.AgencyLocationMappingFixture.deserializeMapping2;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import static org.folio.innreach.fixture.AgencyLocationMappingFixture.deserializeMapping;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import org.folio.innreach.domain.service.CentralServerConfigurationService;
import org.folio.innreach.dto.Agency;
import org.folio.innreach.dto.LocalServer;
import org.folio.innreach.mapper.AgencyLocationMappingMapper;
import org.folio.innreach.mapper.AgencyLocationMappingMapperImpl;
import org.folio.innreach.mapper.MappingMethods;
import org.folio.innreach.repository.AgencyLocationMappingRepository;

@ExtendWith(MockitoExtension.class)
class AgencyMappingServiceImplTest {

  private static final UUID DEFAULT_LOCATION_ID = UUID.fromString("ab6a8c69-ee1a-4b52-a881-dc86da6be857");
  private static final UUID LOCAL_SERVER_LOCATION_ID = UUID.fromString("edb00dae-2735-4e38-bd41-69427295fdbe");
  private static final UUID AGENCY_LOCATION_ID = UUID.fromString("9b0317f2-8409-455c-9cb6-4fb11395d04b");

  private static final String MAPPED_AGENCY_CODE = "5qwer";
  private static final String MAPPED_LOCAL_CODE = "5htxv";
  private static final String UNMAPPED_AGENCY_CODE = "abcd1";
  private static final String UNMAPPED_LOCAL_CODE = "fdsq1";

  @InjectMocks
  private AgencyMappingServiceImpl service;

  @Mock
  private AgencyLocationMappingRepository repository;
  @Spy
  private AgencyLocationMappingMapper mapper = new AgencyLocationMappingMapperImpl(new MappingMethods());
  @Mock
  private CentralServerConfigurationService configurationService;

  @Test
  void getLocationIdByAgencyCode_usingAgencyCodeMapping() {
    var mappingDto = deserializeMapping();
    var mapping = mapper.toEntity(mappingDto);

    when(repository.fetchOneByCsId(any())).thenReturn(Optional.of(mapping));

    var locationId = service.getLocationIdByAgencyCode(UUID.randomUUID(), MAPPED_AGENCY_CODE);

    assertNotNull(locationId);
    assertEquals(AGENCY_LOCATION_ID, locationId);
  }


  @Test
  void getLocationIdByAgencyCode_usingLocalCodeMapping() {
    var mappingDto = deserializeMapping();
    var mapping = mapper.toEntity(mappingDto);

    var unmappedAgency = new Agency().agencyCode(UNMAPPED_AGENCY_CODE);
    var localServer = new LocalServer().localCode(MAPPED_LOCAL_CODE).addAgencyListItem(unmappedAgency);

    when(repository.fetchOneByCsId(any())).thenReturn(Optional.of(mapping));
    when(configurationService.getLocalServers(any())).thenReturn(List.of(localServer));

    var locationId = service.getLocationIdByAgencyCode(UUID.randomUUID(), UNMAPPED_AGENCY_CODE);

    assertNotNull(locationId);
    assertEquals(LOCAL_SERVER_LOCATION_ID, locationId);
  }

  @Test
  void getLocationIdByAgencyCode_usingNullLocationId() {
    var mappingDto = deserializeMapping2();
    var mapping = mapper.toEntity(mappingDto);

    var unmappedAgency = new Agency().agencyCode(UNMAPPED_AGENCY_CODE);
    var localServer = new LocalServer().localCode(MAPPED_LOCAL_CODE).addAgencyListItem(unmappedAgency);

    when(repository.fetchOneByCsId(any())).thenReturn(Optional.of(mapping));
    when(configurationService.getLocalServers(any())).thenReturn(List.of(localServer));

    var locationId = service.getLocationIdByAgencyCode(UUID.randomUUID(), UNMAPPED_AGENCY_CODE);

    assertNotNull(locationId);
    assertEquals(LOCAL_SERVER_LOCATION_ID, locationId);
  }


  @Test
  void getLocationIdByAgencyCode_usingDefaultMapping() {
    var mappingDto = deserializeMapping();
    var mapping = mapper.toEntity(mappingDto);

    var unmappedAgency = new Agency().agencyCode(UNMAPPED_AGENCY_CODE);
    var unmappedLocalServer = new LocalServer().localCode(UNMAPPED_LOCAL_CODE).addAgencyListItem(unmappedAgency);

    when(repository.fetchOneByCsId(any())).thenReturn(Optional.of(mapping));
    when(configurationService.getLocalServers(any())).thenReturn(List.of(unmappedLocalServer));

    var locationId = service.getLocationIdByAgencyCode(UUID.randomUUID(), UNMAPPED_AGENCY_CODE);

    assertNotNull(locationId);
    assertEquals(DEFAULT_LOCATION_ID, locationId);
  }

}
