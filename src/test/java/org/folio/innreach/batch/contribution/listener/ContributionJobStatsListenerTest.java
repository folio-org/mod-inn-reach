package org.folio.innreach.batch.contribution.listener;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

import static org.folio.innreach.fixture.ContributionFixture.createContributionJobContext;

import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.scope.context.ChunkContext;

import org.folio.innreach.batch.contribution.ContributionJobContext;
import org.folio.innreach.batch.contribution.ContributionJobContext.Statistics;
import org.folio.innreach.batch.contribution.ContributionJobContextManager;
import org.folio.innreach.domain.service.ContributionService;

@ExtendWith(MockitoExtension.class)
class ContributionJobStatsListenerTest {

  private static final ContributionJobContext JOB_CONTEXT = createContributionJobContext();

  @Mock
  private ContributionService contributionService;

  @InjectMocks
  private ContributionJobStatsListener listener;

  @BeforeEach
  public void init() {
    ContributionJobContextManager.beginContributionJobContext(JOB_CONTEXT);
  }

  @AfterEach
  public void clear() {
    ContributionJobContextManager.endContributionJobContext();
  }

  @Test
  void updateStats() {
    listener.updateStats(new Statistics());

    verify(contributionService).updateContributionStats(any(), any());
  }

}
