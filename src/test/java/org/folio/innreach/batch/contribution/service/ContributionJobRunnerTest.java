package org.folio.innreach.batch.contribution.service;

import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static java.util.Map.of;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import static org.folio.innreach.batch.contribution.ContributionJobContext.TENANT_ID_KEY;
import static org.folio.innreach.config.ContributionJobConfig.CONTRIBUTION_JOB_LAUNCHER_NAME;
import static org.folio.innreach.config.ContributionJobConfig.CONTRIBUTION_JOB_NAME;
import static org.folio.innreach.fixture.ContributionFixture.createContribution;
import static org.folio.innreach.fixture.ContributionFixture.mapper;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameter;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.support.SimpleJobOperator;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.beans.factory.BeanFactory;

class ContributionJobRunnerTest {

  private static final String TENANT_ID = "test";

  @Mock
  private BeanFactory beanFactory;

  @InjectMocks
  private ContributionJobRunner jobRunner;

  @Mock
  private Job job;
  @Mock
  private JobLauncher jobLauncher;
  @Mock
  private JobExplorer jobExplorer;
  @Mock
  private SimpleJobOperator jobOperator;
  @Mock
  private JobRepository jobRepository;

  @BeforeEach
  public void beforeEachSetup() {
    MockitoAnnotations.openMocks(this);
  }

  @Test
  void shouldRunJob() throws Exception {
    var contribution = mapper.toDTO(createContribution());
    contribution.setId(UUID.randomUUID());

    when(beanFactory.getBean(CONTRIBUTION_JOB_LAUNCHER_NAME, JobLauncher.class)).thenReturn(jobLauncher);
    when(beanFactory.getBean(CONTRIBUTION_JOB_NAME, Job.class)).thenReturn(job);

    jobRunner.run(UUID.randomUUID(), "test", contribution);

    verify(jobLauncher).run(eq(job), any());
  }

  @Test
  void shouldRestartJob() throws Exception {
    var contribution = mapper.toDTO(createContribution());
    contribution.setId(UUID.randomUUID());

    when(beanFactory.getBean(JobExplorer.class)).thenReturn(jobExplorer);
    when(beanFactory.getBean(SimpleJobOperator.class)).thenReturn(jobOperator);
    when(beanFactory.getBean(any(), eq(JobRepository.class))).thenReturn(jobRepository);

    var jobParameters = new JobParameters(of(TENANT_ID_KEY, new JobParameter(TENANT_ID)));
    var jobExecution = new JobExecution(42L, jobParameters);
    var stepExecution = new StepExecution("test", jobExecution);
    stepExecution.setStatus(BatchStatus.STARTED);
    jobExecution.addStepExecutions(singletonList(stepExecution));

    when(jobExplorer.findRunningJobExecutions(any(String.class)))
      .thenReturn(singleton(jobExecution));

    jobRunner.restart(TENANT_ID);

    verify(jobOperator).restart(anyLong());
    verify(jobRepository).update(any(JobExecution.class));
    verify(jobRepository).update(any(StepExecution.class));
  }

}
