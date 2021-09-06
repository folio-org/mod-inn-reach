package org.folio.innreach.batch.contribution.service;

import static org.mockito.ArgumentMatchers.any;
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
import org.springframework.batch.core.Job;
import org.springframework.batch.core.launch.JobLauncher;
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

}
