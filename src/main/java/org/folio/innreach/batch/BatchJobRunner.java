package org.folio.innreach.batch;

import static org.springframework.batch.core.BatchStatus.STOPPED;
import static org.springframework.batch.core.BatchStatus.STOPPING;

import java.util.Date;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
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

  public abstract void run(UUID centralServerId, T jobConfig);

  public abstract String getJobName();

  public void restart() {
    try {
      var jobExecutions = jobExplorer.findRunningJobExecutions(getJobName());

      for (JobExecution jobExecution : jobExecutions) {
        log.info("Restarting job execution: {}", jobExecution);

        for (StepExecution stepExecution : jobExecution.getStepExecutions()) {
          var status = stepExecution.getStatus();
          if (status.isRunning() || status == STOPPING) {
            stopStepExecution(stepExecution);
          }
        }

        stopJobExecution(jobExecution);

        var jobExecutionId = jobExecution.getId();

        jobOperator.restart(jobExecutionId);
      }
    } catch (Exception e) {
      log.warn("Unable to restart job: {}", getJobName(), e);
    }
  }

  private void stopJobExecution(JobExecution jobExecution) {
    jobExecution.setStatus(STOPPED);
    jobExecution.setEndTime(new Date());
    jobRepository.update(jobExecution);
  }

  private void stopStepExecution(StepExecution stepExecution) {
    stepExecution.setStatus(STOPPED);
    stepExecution.setEndTime(new Date());
    jobRepository.update(stepExecution);
  }

}
