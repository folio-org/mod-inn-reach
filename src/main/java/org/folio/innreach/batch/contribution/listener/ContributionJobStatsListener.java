package org.folio.innreach.batch.contribution.listener;

import static java.util.UUID.fromString;

import static org.folio.innreach.batch.contribution.ContributionJobContext.CENTRAL_SERVER_ID_KEY;

import java.util.UUID;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.listener.ChunkListenerSupport;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.stereotype.Component;

import org.folio.innreach.domain.service.ContributionService;
import org.folio.innreach.dto.ContributionDTO;

@Log4j2
@Component
@RequiredArgsConstructor
public class ContributionJobStatsListener extends ChunkListenerSupport {

  private final ContributionService contributionService;

  @Override
  public void afterChunk(ChunkContext chunkContext) {
    var stepExecution = chunkContext.getStepContext().getStepExecution();
    var centralServerId = getCentralServerId(stepExecution);
    var stats = collectStats(stepExecution);
    contributionService.updateContributionStats(centralServerId, stats);
  }

  @Override
  public void afterChunkError(ChunkContext context) {
    afterChunk(context);
  }

  private ContributionDTO collectStats(StepExecution stepContext) {
    var contribution = new ContributionDTO();
    contribution.setRecordsContributed((long) stepContext.getWriteCount());
    contribution.setRecordsProcessed(contribution.getRecordsContributed() + stepContext.getSkipCount());
    contribution.setRecordsTotal(null);
    contribution.setRecordsDecontributed(null);
    contribution.setRecordsUpdated(null);
    return contribution;
  }

  private static UUID getCentralServerId(StepExecution stepExecution) {
    var jobParameters = stepExecution.getJobParameters();
    return fromString(jobParameters.getString(CENTRAL_SERVER_ID_KEY));
  }

}
