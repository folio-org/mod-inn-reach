package org.folio.innreach.batch.contribution.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import static org.folio.innreach.fixture.ContributionFixture.createItem;
import static org.folio.innreach.fixture.ContributionFixture.irOkResponse;

import java.util.Collections;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.folio.innreach.batch.contribution.ContributionJobContext;
import org.folio.innreach.domain.service.CentralServerService;
import org.folio.innreach.domain.service.ContributionValidationService;
import org.folio.innreach.domain.service.InnReachLocationService;
import org.folio.innreach.domain.service.LibraryMappingService;
import org.folio.innreach.domain.service.LocationMappingService;
import org.folio.innreach.domain.service.MaterialTypeMappingService;
import org.folio.innreach.dto.CentralServerDTO;
import org.folio.innreach.dto.InnReachLocationsDTO;
import org.folio.innreach.dto.LibraryMappingsDTO;
import org.folio.innreach.dto.MaterialTypeMappingsDTO;
import org.folio.innreach.external.service.InnReachContributionService;

@ExtendWith(MockitoExtension.class)
class ItemContributorTest {

  private static final UUID CENTRAL_SERVER_ID = UUID.randomUUID();
  public static final String TENANT_ID = "test";

  @Mock
  private InnReachContributionService irContributionService;
  @Mock
  private ContributionValidationService validationService;
  @Mock
  private MaterialTypeMappingService typeMappingService;
  @Mock
  private LibraryMappingService libraryMappingService;
  @Mock
  private InnReachLocationService irLocationService;
  @Mock
  private CentralServerService centralServerService;
  @Mock
  private LocationMappingService locationMappingService;
  @Mock
  private ContributionJobContext jobContext;

  @InjectMocks
  private ItemContributor service;

  @Test
  void shouldContributeItems() {
    when(jobContext.getCentralServerId()).thenReturn(CENTRAL_SERVER_ID);
    when(irContributionService.contributeBibItems(any(), any(), any())).thenReturn(irOkResponse());
    when(irLocationService.getAllInnReachLocations(any(), any())).thenReturn(new InnReachLocationsDTO());
    when(typeMappingService.getAllMappings(any(), anyInt(), anyInt())).thenReturn(new MaterialTypeMappingsDTO());
    when(libraryMappingService.getAllMappings(any(), anyInt(), anyInt())).thenReturn(new LibraryMappingsDTO());
    when(centralServerService.getCentralServer(any())).thenReturn(new CentralServerDTO());

    service.write(Collections.singletonList(createItem()));

    verify(irContributionService).contributeBibItems(eq(CENTRAL_SERVER_ID), any(), any());
  }

}
