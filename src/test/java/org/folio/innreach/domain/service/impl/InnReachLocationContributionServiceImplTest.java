package org.folio.innreach.domain.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import static org.folio.innreach.fixture.InnReachLocationFixture.createInnReachLocation;
import static org.folio.innreach.fixture.MappingFixture.createLibraryMapping;
import static org.folio.innreach.fixture.MappingFixture.createLocationMapping;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Example;

import org.folio.innreach.domain.service.CentralServerService;
import org.folio.innreach.external.dto.InnReachLocationDTO;
import org.folio.innreach.external.service.InnReachLocationExternalService;
import org.folio.innreach.repository.InnReachLocationRepository;
import org.folio.innreach.repository.LibraryMappingRepository;
import org.folio.innreach.repository.LocationMappingRepository;

@ExtendWith(MockitoExtension.class)
class InnReachLocationContributionServiceImplTest {

  private static final UUID CENTRAL_SERVER_ID = UUID.randomUUID();

  @Mock
  private LocationMappingRepository locationMappingRepository;
  @Mock
  private LibraryMappingRepository libraryMappingRepository;
  @Mock
  private CentralServerService centralServerService;
  @Mock
  private InnReachLocationExternalService innReachLocationExternalService;
  @Mock
  private InnReachLocationRepository innReachLocationRepository;

  @InjectMocks
  private InnReachLocationContributionServiceImpl service;

  @Test
  void shouldContributeInnReachLocations() throws ExecutionException, InterruptedException, TimeoutException {
    var irLoc1 = createInnReachLocation();
    var irLoc2 = createInnReachLocation();

    var libMapping = createLibraryMapping();
    libMapping.setInnReachLocation(irLoc1);

    var locMapping = createLocationMapping();
    locMapping.setInnReachLocation(irLoc2);

    when(innReachLocationRepository.findAllById(any())).thenReturn(List.of(irLoc1, irLoc2));
    when(libraryMappingRepository.findAll(any(Example.class))).thenReturn(List.of(libMapping));
    when(locationMappingRepository.findByCentralServerId(any())).thenReturn(List.of(locMapping));

    service.contributeInnReachLocations(CENTRAL_SERVER_ID);

    var irLocationsCaptor = ArgumentCaptor.forClass(List.class);

    verify(innReachLocationExternalService).submitMappedLocationsToInnReach(any(), irLocationsCaptor.capture());

    List<InnReachLocationDTO> submittedLocations = irLocationsCaptor.getValue();

    assertThat(submittedLocations)
      .map(InnReachLocationDTO::getCode)
      .containsExactlyInAnyOrder(irLoc1.getCode(), irLoc2.getCode());
  }

}
