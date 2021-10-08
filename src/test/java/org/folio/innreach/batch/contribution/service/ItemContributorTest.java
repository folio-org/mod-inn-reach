package org.folio.innreach.batch.contribution.service;

import static com.google.common.collect.ImmutableList.of;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import static org.folio.innreach.external.dto.InnReachResponse.okResponse;
import static org.folio.innreach.fixture.ContributionFixture.createContributionJobContext;
import static org.folio.innreach.fixture.ContributionFixture.createItem;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.folio.innreach.batch.contribution.ContributionJobContext;
import org.folio.innreach.batch.contribution.ContributionJobContextManager;
import org.folio.innreach.batch.contribution.listener.ContributionExceptionListener;
import org.folio.innreach.domain.dto.folio.ContributionItemCirculationStatus;
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

  private static final ContributionJobContext JOB_CONTEXT = createContributionJobContext();

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
  private ContributionExceptionListener exceptionListener;

  @InjectMocks
  private ItemContributor service;

  @BeforeEach
  public void init() {
    ContributionJobContextManager.beginContributionJobContext(JOB_CONTEXT);
  }

  @AfterEach
  public void clear() {
    ContributionJobContextManager.endContributionJobContext();
  }

  @Test
  void shouldContributeItems() {
    when(irContributionService.contributeBibItems(any(), any(), any())).thenReturn(okResponse());
    when(irLocationService.getAllInnReachLocations(any(), any())).thenReturn(new InnReachLocationsDTO());
    when(typeMappingService.getAllMappings(any(), anyInt(), anyInt())).thenReturn(new MaterialTypeMappingsDTO());
    when(libraryMappingService.getAllMappings(any(), anyInt(), anyInt())).thenReturn(new LibraryMappingsDTO());
    when(centralServerService.getCentralServer(any())).thenReturn(new CentralServerDTO());
    when(validationService.getItemCirculationStatus(any(), any())).thenReturn(ContributionItemCirculationStatus.AVAILABLE);

    service.contributeItems("test", of(createItem()));

    verify(irContributionService).contributeBibItems(eq(JOB_CONTEXT.getCentralServerId()), any(), any());
  }

}
