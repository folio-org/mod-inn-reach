package org.folio.innreach.batch.contribution.service;

import static com.google.common.collect.ImmutableList.of;
import static org.folio.innreach.external.dto.InnReachResponse.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import static org.folio.innreach.fixture.ContributionFixture.createContributionJobContext;
import static org.folio.innreach.fixture.ContributionFixture.createItem;
import static org.folio.innreach.fixture.TestUtil.createNoRetryTemplate;

import java.util.ArrayList;
import java.util.List;

import org.folio.innreach.external.dto.InnReachResponse;
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
import org.folio.innreach.batch.contribution.listener.ContributionExceptionListener;
import org.folio.innreach.domain.service.RecordTransformationService;
import org.folio.innreach.domain.service.impl.RecordContributionServiceImpl;
import org.folio.innreach.external.dto.BibItem;
import org.folio.innreach.external.service.InnReachContributionService;

@ExtendWith(MockitoExtension.class)
class ItemContributorTest {

  private static final ContributionJobContext JOB_CONTEXT = createContributionJobContext();

  @Spy
  private RetryTemplate retryTemplate = createNoRetryTemplate();
  @Mock
  private InnReachContributionService irContributionService;
  @Mock
  private RecordTransformationService recordTransformationService;
  @Mock
  private ContributionExceptionListener exceptionListener;

  @Mock
  private InnReachResponse response;

  @InjectMocks
  private RecordContributionServiceImpl service;

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
    when(irContributionService.contributeBibItems(any(), any(), any())).thenReturn(response);
    when(recordTransformationService.getBibItems(any(), any(), any())).thenReturn(List.of(new BibItem()));
    when(response.getErrors()).thenReturn(new ArrayList<>());
    when(response.isOk()).thenReturn(true);

    service.contributeItems(JOB_CONTEXT.getCentralServerId(), "test", of(createItem()));

    verify(irContributionService).contributeBibItems(eq(JOB_CONTEXT.getCentralServerId()), any(), any());
  }

}
