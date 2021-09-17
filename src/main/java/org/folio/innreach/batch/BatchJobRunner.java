package org.folio.innreach.batch;

import static org.springframework.batch.core.BatchStatus.STOPPED;
import static org.springframework.batch.core.BatchStatus.STOPPING;

import static org.folio.innreach.batch.contribution.ContributionJobContext.TENANT_ID_KEY;

import java.util.Date;
import java.util.UUID;

import lombok.extern.log4j.Log4j2;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;

@Log4j2
public abstract class BatchJobRunner<T> implements BeanFactoryAware {

  private BeanFactory beanFactory;

  public abstract void run(UUID centralServerId, String tenantId, T jobConfig);

  public abstract String getJobName();

  public abstract String getJobLauncherName();

  @Override
  public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
    this.beanFactory = beanFactory;
  }

  public void restart(String tenantId) {
    try {
      var jobExplorer = beanFactory.getBean(JobExplorer.class);
      var jobOperator = beanFactory.getBean(JobOperator.class);
      var jobRepository = beanFactory.getBean(JobRepository.class);

      var jobExecutions = jobExplorer.findRunningJobExecutions(getJobName());
      for (var jobExecution : jobExecutions) {
        var jobTenantId = getJobTenantId(jobExecution);
        if (!tenantId.equals(jobTenantId)) {
          continue;
        }

        log.info("Restarting job execution: {}", jobExecution);
        for (var stepExecution : jobExecution.getStepExecutions()) {
          var status = stepExecution.getStatus();
          if (status.isRunning() || status == STOPPING) {
            stopStepExecution(jobRepository, stepExecution);
          }
        }

        stopJobExecution(jobRepository, jobExecution);

        var jobExecutionId = jobExecution.getId();

        jobOperator.restart(jobExecutionId);
      }
    } catch (Exception e) {
      log.warn("Unable to restart job: {}", getJobName(), e);
    }
  }

  protected void launch(JobParameters jobParameters) throws Exception {
    var jobLauncher = beanFactory.getBean(getJobLauncherName(), JobLauncher.class);
    var job = beanFactory.getBean(getJobName(), Job.class);

    jobLauncher.run(job, jobParameters);
  }

  private void stopJobExecution(JobRepository jobRepository, JobExecution jobExecution) {
    jobExecution.setStatus(STOPPED);
    jobExecution.setEndTime(new Date());
    jobRepository.update(jobExecution);
  }

  private void stopStepExecution(JobRepository jobRepository, StepExecution stepExecution) {
    stepExecution.setStatus(STOPPED);
    stepExecution.setEndTime(new Date());
    jobRepository.update(stepExecution);
  }

  private String getJobTenantId(JobExecution jobExecution) {
    return jobExecution.getJobParameters().getString(TENANT_ID_KEY);
  }

}
