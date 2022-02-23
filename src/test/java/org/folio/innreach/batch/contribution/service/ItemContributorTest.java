package org.folio.innreach.batch.contribution.service;

import static com.google.common.collect.ImmutableList.of;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import static org.folio.innreach.external.dto.InnReachResponse.okResponse;
import static org.folio.innreach.fixture.ContributionFixture.createContributionJobContext;
import static org.folio.innreach.fixture.ContributionFixture.createItem;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.retry.RetryCallback;
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

  @Mock
  private RetryTemplate retryTemplate;
  @Mock
  private InnReachContributionService irContributionService;
  @Mock
  private RecordTransformationService recordTransformationService;
  @Mock
  private ContributionExceptionListener exceptionListener;

  @InjectMocks
  private RecordContributionServiceImpl service;

  @BeforeEach
  public void init() {
    ContributionJobContextManager.beginContributionJobContext(JOB_CONTEXT);
    when(retryTemplate.execute(any(), any(), any())).thenAnswer(invocation -> {
      RetryCallback retry = invocation.getArgument(0);
      return retry.doWithRetry(null);
    });
  }

  @AfterEach
  public void clear() {
    ContributionJobContextManager.endContributionJobContext();
  }

  @Test
  void shouldContributeItems() {
    when(irContributionService.contributeBibItems(any(), any(), any())).thenReturn(okResponse());
    when(recordTransformationService.getBibItems(any(), any(), any())).thenReturn(List.of(new BibItem()));

    service.contributeItems(JOB_CONTEXT.getCentralServerId(), "test", of(createItem()));

    verify(irContributionService).contributeBibItems(eq(JOB_CONTEXT.getCentralServerId()), any(), any());
  }

}
