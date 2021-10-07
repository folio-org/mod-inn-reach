package org.folio.innreach.batch.contribution.service;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import static org.folio.innreach.external.dto.InnReachResponse.errorResponse;
import static org.folio.innreach.external.dto.InnReachResponse.okResponse;
import static org.folio.innreach.fixture.ContributionFixture.createInstance;
import static org.folio.innreach.fixture.FolioContextFixture.createTenantExecutionService;

import java.util.Collections;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import org.folio.innreach.batch.contribution.ContributionJobContext;
import org.folio.innreach.domain.service.InstanceTransformationService;
import org.folio.innreach.domain.service.impl.TenantScopedExecutionService;
import org.folio.innreach.dto.BibInfo;
import org.folio.innreach.external.service.InnReachContributionService;

@ExtendWith(MockitoExtension.class)
class InstanceContributorTest {

  private static final UUID CENTRAL_SERVER_ID = UUID.randomUUID();

  @Mock
  private InnReachContributionService irContributionService;
  @Mock
  private ContributionJobContext jobContext;
  @Mock
  private InstanceTransformationService instanceTransformationService;
  @Spy
  private TenantScopedExecutionService tenantScopedExecutionService = createTenantExecutionService();

  @InjectMocks
  private InstanceContributor instanceContributor;

  @Test
  void shouldContributeAndLookUp() throws Exception {
    when(jobContext.getTenantId()).thenReturn("test");
    when(jobContext.getCentralServerId()).thenReturn(CENTRAL_SERVER_ID);
    when(instanceTransformationService.getBibInfo(any(), any())).thenReturn(new BibInfo());
    when(irContributionService.contributeBib(any(), any(), any())).thenReturn(okResponse());
    when(irContributionService.lookUpBib(any(), any())).thenReturn(okResponse());

    instanceContributor.write(singletonList(createInstance()));

    verify(irContributionService).contributeBib(eq(CENTRAL_SERVER_ID), any(), any());
    verify(irContributionService).lookUpBib(eq(CENTRAL_SERVER_ID), any());
  }

  @Test
  void shouldFailContribution() {
    when(jobContext.getTenantId()).thenReturn("test");
    when(jobContext.getCentralServerId()).thenReturn(CENTRAL_SERVER_ID);
    when(instanceTransformationService.getBibInfo(any(), any())).thenReturn(new BibInfo());
    when(irContributionService.contributeBib(any(), any(), any())).thenReturn(errorResponse());

    assertThatThrownBy(() -> instanceContributor.write(singletonList(createInstance())))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("Unexpected contribution response:");
  }

  @Test
  void shouldFailVerification() {

    when(jobContext.getTenantId()).thenReturn("test");
    when(jobContext.getCentralServerId()).thenReturn(CENTRAL_SERVER_ID);
    when(instanceTransformationService.getBibInfo(any(), any())).thenReturn(new BibInfo());
    when(irContributionService.contributeBib(any(), any(), any())).thenReturn(okResponse());
    when(irContributionService.lookUpBib(any(), any())).thenReturn(errorResponse());

    assertThatThrownBy(() -> instanceContributor.write(singletonList(createInstance())))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("Unexpected verification response:");
  }

  @Test
  void shouldDoNothingWhenNoItems() throws Exception {
    instanceContributor.write(Collections.emptyList());

    verifyNoMoreInteractions(tenantScopedExecutionService);
    verifyNoMoreInteractions(irContributionService);
  }

}
