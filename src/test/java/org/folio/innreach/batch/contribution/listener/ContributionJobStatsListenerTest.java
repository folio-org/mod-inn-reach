package org.folio.innreach.batch.contribution.listener;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import static org.folio.innreach.batch.contribution.ContributionJobContext.CENTRAL_SERVER_ID_KEY;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.scope.context.ChunkContext;

import org.folio.innreach.domain.service.ContributionService;

@ExtendWith(MockitoExtension.class)
class ContributionJobStatsListenerTest {

  private static final String CENTRAL_SERVER_ID = UUID.randomUUID().toString();

  @Mock
  private ContributionService contributionService;

  @InjectMocks
  private ContributionJobStatsListener listener;

  @Mock(answer = Answers.RETURNS_DEEP_STUBS)
  private ChunkContext context;

  @Test
  void afterChunk() {
    when(getJobParameters().getString(CENTRAL_SERVER_ID_KEY))
      .thenReturn(CENTRAL_SERVER_ID);

    listener.afterChunk(context);

    verify(contributionService).updateContributionStats(any(), any());
  }

  @Test
  void afterChunkError() {
    when(getJobParameters().getString(CENTRAL_SERVER_ID_KEY))
      .thenReturn(CENTRAL_SERVER_ID);

    listener.afterChunkError(context);

    verify(contributionService).updateContributionStats(any(), any());
  }

  private JobParameters getJobParameters() {
    return context.getStepContext().getStepExecution().getJobParameters();
  }

}
