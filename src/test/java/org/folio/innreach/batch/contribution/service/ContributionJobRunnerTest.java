package org.folio.innreach.batch.contribution.service;

import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.beans.factory.BeanFactory;

class ContributionJobRunnerTest {

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
  private JobOperator jobOperator;
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

    when(beanFactory.getBean(CONTRIBUTION_JOB_NAME, JobLauncher.class)).thenReturn(jobLauncher);
    when(beanFactory.getBean(CONTRIBUTION_JOB_NAME, Job.class)).thenReturn(job);

    jobRunner.run(UUID.randomUUID(), contribution);

    verify(jobLauncher).run(eq(job), any());
  }

  @Test
  void shouldRestartJob() throws Exception {
    var contribution = mapper.toDTO(createContribution());
    contribution.setId(UUID.randomUUID());

    when(beanFactory.getBean(JobExplorer.class)).thenReturn(jobExplorer);
    when(beanFactory.getBean(JobOperator.class)).thenReturn(jobOperator);
    when(beanFactory.getBean(JobRepository.class)).thenReturn(jobRepository);

    var jobExecution = new JobExecution(42L);
    var stepExecution = new StepExecution("test", jobExecution);
    stepExecution.setStatus(BatchStatus.STARTED);
    jobExecution.addStepExecutions(singletonList(stepExecution));

    when(jobExplorer.findRunningJobExecutions(any(String.class)))
      .thenReturn(singleton(jobExecution));

    jobRunner.restart();

    verify(jobOperator).restart(anyLong());
    verify(jobRepository).update(any(JobExecution.class));
    verify(jobRepository).update(any(StepExecution.class));
  }

}
