package org.folio.innreach.batch.contribution.service;

import static org.folio.innreach.batch.contribution.ContributionJobContext.CENTRAL_SERVER_ID_KEY;
import static org.folio.innreach.batch.contribution.ContributionJobContext.CONTRIBUTION_ID_KEY;
import static org.folio.innreach.batch.contribution.ContributionJobContext.ITERATION_JOB_ID_KEY;
import static org.folio.innreach.batch.contribution.ContributionJobContext.TENANT_ID_KEY;
import static org.folio.innreach.config.ContributionJobConfig.CONTRIBUTION_JOB_LAUNCHER_NAME;
import static org.folio.innreach.config.ContributionJobConfig.CONTRIBUTION_JOB_NAME;
import static org.folio.innreach.config.ContributionJobConfig.CONTRIBUTION_JOB_RUNNER_NAME;

import java.util.UUID;

import lombok.extern.log4j.Log4j2;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import org.folio.innreach.batch.BatchJobRunner;
import org.folio.innreach.dto.ContributionDTO;
import org.folio.spring.FolioExecutionContext;

@Log4j2
@Service
@Qualifier(CONTRIBUTION_JOB_RUNNER_NAME)
public class ContributionJobRunner extends BatchJobRunner<ContributionDTO> {

  private final FolioExecutionContext context;
  private final BeanFactory beanFactory;

  public ContributionJobRunner(JobOperator jobOperator, JobExplorer jobExplorer, JobRepository jobRepository, FolioExecutionContext context, BeanFactory beanFactory) {
    super(jobOperator, jobExplorer, jobRepository);
    this.context = context;
    this.beanFactory = beanFactory;
  }

  @Override
  public void run(UUID centralServerId, ContributionDTO contribution) {
    try {
      var jobLauncher = beanFactory.getBean(CONTRIBUTION_JOB_LAUNCHER_NAME, JobLauncher.class);
      var contributionJob = beanFactory.getBean(CONTRIBUTION_JOB_NAME, Job.class);

      var jobParameters = getJobParameters(centralServerId, contribution);

      jobLauncher.run(contributionJob, jobParameters);
    } catch (Exception e) {
      throw new RuntimeException("Unable to start contribution job", e);
    }
  }

  @Override
  public void restart() {
    super.restart();
  }

  @Override
  public String getJobName() {
    return CONTRIBUTION_JOB_NAME;
  }

  private JobParameters getJobParameters(UUID centralServerId, ContributionDTO contribution) {
    return new JobParametersBuilder()
      .addString(TENANT_ID_KEY, context.getTenantId())
      .addString(CENTRAL_SERVER_ID_KEY, centralServerId.toString())
      .addString(CONTRIBUTION_ID_KEY, contribution.getId().toString())
      .addString(ITERATION_JOB_ID_KEY, contribution.getJobId().toString())
      .toJobParameters();
  }

}
