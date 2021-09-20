package org.folio.innreach.batch.contribution.service;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import static org.folio.innreach.fixture.ContributionFixture.createInstance;
import static org.folio.innreach.fixture.ContributionFixture.createMARCRecord;
import static org.folio.innreach.fixture.ContributionFixture.irErrorResponse;
import static org.folio.innreach.fixture.ContributionFixture.irOkResponse;

import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.Callable;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.folio.innreach.batch.contribution.ContributionJobContext;
import org.folio.innreach.domain.service.ContributionValidationService;
import org.folio.innreach.domain.service.MARCRecordTransformationService;
import org.folio.innreach.domain.service.impl.TenantScopedExecutionService;
import org.folio.innreach.external.service.InnReachContributionService;

@ExtendWith(MockitoExtension.class)
class InstanceContributorTest {

  private static final UUID CENTRAL_SERVER_ID = UUID.randomUUID();

  @Mock
  private TenantScopedExecutionService tenantScopedExecutionService;
  @Mock
  private MARCRecordTransformationService marcService;
  @Mock
  private InnReachContributionService irContributionService;
  @Mock
  private ContributionJobContext jobContext;
  @Mock
  private ContributionValidationService validationService;

  @InjectMocks
  private InstanceContributor instanceContributor;

  @Test
  void shouldContributeAndLookUp() throws Exception {
    when(tenantScopedExecutionService.executeTenantScoped(any(), any()))
      .thenAnswer(invocationOnMock -> {
        var job = (Callable<?>) invocationOnMock.getArgument(1);
        return job.call();
      });

    when(jobContext.getTenantId()).thenReturn("test");
    when(jobContext.getCentralServerId()).thenReturn(CENTRAL_SERVER_ID);
    when(irContributionService.contributeBib(any(), any(), any())).thenReturn(irOkResponse());
    when(irContributionService.lookUpBib(any(), any())).thenReturn(irOkResponse());
    when(marcService.transformRecord(any(), any())).thenReturn(createMARCRecord());

    instanceContributor.write(singletonList(createInstance()));

    verify(irContributionService).contributeBib(eq(CENTRAL_SERVER_ID), any(), any());
    verify(irContributionService).lookUpBib(eq(CENTRAL_SERVER_ID), any());
  }

  @Test
  void shouldFailContribution() {
    when(tenantScopedExecutionService.executeTenantScoped(any(), any()))
      .thenAnswer(invocationOnMock -> {
        var job = (Callable<?>) invocationOnMock.getArgument(1);
        return job.call();
      });

    when(jobContext.getTenantId()).thenReturn("test");
    when(jobContext.getCentralServerId()).thenReturn(CENTRAL_SERVER_ID);
    when(marcService.transformRecord(any(), any())).thenReturn(createMARCRecord());
    when(irContributionService.contributeBib(any(), any(), any())).thenReturn(irErrorResponse());

    assertThatThrownBy(() -> instanceContributor.write(singletonList(createInstance())))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("Unexpected contribution response:");
  }

  @Test
  void shouldFailVerification() {
    when(tenantScopedExecutionService.executeTenantScoped(any(), any()))
      .thenAnswer(invocationOnMock -> {
        var job = (Callable<?>) invocationOnMock.getArgument(1);
        return job.call();
      });

    when(jobContext.getTenantId()).thenReturn("test");
    when(jobContext.getCentralServerId()).thenReturn(CENTRAL_SERVER_ID);
    when(marcService.transformRecord(any(), any())).thenReturn(createMARCRecord());
    when(irContributionService.contributeBib(any(), any(), any())).thenReturn(irOkResponse());
    when(irContributionService.lookUpBib(any(), any())).thenReturn(irErrorResponse());

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
