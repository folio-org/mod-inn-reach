package org.folio.innreach.batch.contribution.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import static org.folio.innreach.external.dto.InnReachResponse.errorResponse;
import static org.folio.innreach.external.dto.InnReachResponse.okResponse;
import static org.folio.innreach.fixture.ContributionFixture.createContributionJobContext;
import static org.folio.innreach.fixture.ContributionFixture.createInstance;
import static org.folio.innreach.fixture.TestUtil.createNoRetryTemplate;

import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.retry.support.RetryTemplate;

import org.folio.innreach.batch.contribution.ContributionJobContext;
import org.folio.innreach.batch.contribution.ContributionJobContextManager;
import org.folio.innreach.domain.service.RecordTransformationService;
import org.folio.innreach.domain.service.impl.RecordContributionServiceImpl;
import org.folio.innreach.dto.BibInfo;
import org.folio.innreach.external.service.InnReachContributionService;

@ExtendWith(MockitoExtension.class)
class InstanceContributorTest {

  private static final ContributionJobContext JOB_CONTEXT = createContributionJobContext();
  private static final UUID CENTRAL_SERVER_ID = JOB_CONTEXT.getCentralServerId();

  @Mock
  private InnReachContributionService irContributionService;
  @Mock
  private RecordTransformationService instanceTransformationService;
  @Spy
  private RetryTemplate retryTemplate = createNoRetryTemplate();

  @InjectMocks
  private RecordContributionServiceImpl instanceContributor;

  @BeforeEach
  public void init() {
    ContributionJobContextManager.beginContributionJobContext(JOB_CONTEXT);
  }

  @AfterEach
  public void clear() {
    ContributionJobContextManager.endContributionJobContext();
  }

  @Test
  void shouldContributeAndLookUp() {
    when(instanceTransformationService.getBibInfo(any(), any())).thenReturn(new BibInfo());
    when(irContributionService.contributeBib(any(), any(), any())).thenReturn(okResponse());
    when(irContributionService.lookUpBib(any(), any())).thenReturn(okResponse());

    instanceContributor.contributeInstance(CENTRAL_SERVER_ID, createInstance());

    verify(irContributionService).contributeBib(eq(CENTRAL_SERVER_ID), any(), any());
    verify(irContributionService).lookUpBib(eq(CENTRAL_SERVER_ID), any());
  }

  @Test
  void shouldFailContribution() {
    var instance = createInstance();

    when(instanceTransformationService.getBibInfo(any(), any())).thenReturn(new BibInfo());
    when(irContributionService.contributeBib(any(), any(), any())).thenReturn(errorResponse());

    assertThatThrownBy(() -> instanceContributor.contributeInstance(CENTRAL_SERVER_ID, instance))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("Unexpected contribution response:");
  }

  @Test
  void shouldFailVerification() {
    var instance = createInstance();

    when(instanceTransformationService.getBibInfo(any(), any())).thenReturn(new BibInfo());
    when(irContributionService.contributeBib(any(), any(), any())).thenReturn(okResponse());
    when(irContributionService.lookUpBib(any(), any())).thenReturn(errorResponse());

    assertThatThrownBy(() -> instanceContributor.contributeInstance(CENTRAL_SERVER_ID, instance))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("Unexpected verification response:");
  }

}
