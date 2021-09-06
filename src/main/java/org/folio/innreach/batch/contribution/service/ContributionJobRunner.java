package org.folio.innreach.batch.contribution.service;

import static org.folio.innreach.batch.contribution.ContributionJobContext.CENTRAL_SERVER_ID_KEY;
import static org.folio.innreach.batch.contribution.ContributionJobContext.CONTRIBUTION_ID_KEY;
import static org.folio.innreach.batch.contribution.ContributionJobContext.ITERATION_JOB_ID_KEY;
import static org.folio.innreach.config.ContributionJobConfig.CONTRIBUTION_JOB_NAME;
import static org.folio.innreach.config.ContributionJobConfig.CONTRIBUTION_JOB_RUNNER_NAME;

import java.util.UUID;

import lombok.extern.log4j.Log4j2;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import org.folio.innreach.batch.BatchJobRunner;
import org.folio.innreach.dto.ContributionDTO;

@Log4j2
@Service
@Qualifier(CONTRIBUTION_JOB_RUNNER_NAME)
public class ContributionJobRunner extends BatchJobRunner<ContributionDTO> {

  @Override
  public void run(UUID centralServerId, ContributionDTO contribution) {
    try {
      var jobParameters = getJobParameters(centralServerId, contribution);

      launch(jobParameters);
    } catch (Exception e) {
      throw new IllegalStateException("Unable to start contribution job", e);
    }
  }

  @Override
  public String getJobName() {
    return CONTRIBUTION_JOB_NAME;
  }

  private JobParameters getJobParameters(UUID centralServerId, ContributionDTO contribution) {
    return new JobParametersBuilder()
      .addString(CENTRAL_SERVER_ID_KEY, centralServerId.toString())
      .addString(CONTRIBUTION_ID_KEY, contribution.getId().toString())
      .addString(ITERATION_JOB_ID_KEY, contribution.getJobId().toString())
      .toJobParameters();
  }

}
