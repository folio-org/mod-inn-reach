package org.folio.innreach.batch.contribution;

import static java.util.UUID.fromString;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.annotation.BeforeJob;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class ContributionJobContext {

  public static final String TENANT_ID_KEY = "tenantId";
  public static final String CENTRAL_SERVER_ID_KEY = "centralServerId";
  public static final String CONTRIBUTION_ID_KEY = "contributionId";
  public static final String ITERATION_JOB_ID_KEY = "iterationJobId";

  private JobParameters jobParameters;

  @BeforeJob
  public void beforeJob(JobExecution jobExecution) {
    jobParameters = jobExecution.getJobParameters();
  }

  public String getTenantId() {
    return jobParameters.getString(TENANT_ID_KEY);
  }

  public UUID getCentralServerId() {
    return fromString(jobParameters.getString(CENTRAL_SERVER_ID_KEY));
  }

  public UUID getContributionId() {
    return fromString(jobParameters.getString(CONTRIBUTION_ID_KEY));
  }

  public UUID getIterationJobId() {
    return fromString(jobParameters.getString(ITERATION_JOB_ID_KEY));
  }

}
