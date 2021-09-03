package org.folio.innreach.batch;

import java.util.Collection;
import java.util.Date;
import java.util.Set;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.repository.JobRepository;

@Log4j2
@RequiredArgsConstructor
public abstract class BatchJobRunner<T> {

  protected final JobOperator jobOperator;
  protected final JobExplorer jobExplorer;
  protected final JobRepository jobRepository;

  public void restart() {
    try {
      Set<JobExecution> jobExecutions = jobExplorer.findRunningJobExecutions(getJobName());

      for (JobExecution jobExecution : jobExecutions) {

        Collection<StepExecution> stepExecutions = jobExecution.getStepExecutions();
        for (StepExecution stepExecution : stepExecutions) {
          BatchStatus status = stepExecution.getStatus();
          if (status.isRunning() || status == BatchStatus.STOPPING) {
            stepExecution.setStatus(BatchStatus.STOPPED);
            stepExecution.setEndTime(new Date());
            jobRepository.update(stepExecution);
          }
        }

        jobExecution.setStatus(BatchStatus.STOPPED);
        jobExecution.setEndTime(new Date());
        jobRepository.update(jobExecution);

        Long jobExecutionId = jobExecution.getId();

        log.info("Restarting job execution ", jobExecution);
        jobOperator.restart(jobExecutionId);
      }
    } catch (Exception e) {
      log.warn("Unable to restart job: {}", getJobName(), e);
    }
  }

  public abstract void run(UUID centralServerId, T jobConfig);

  public abstract String getJobName();

}
