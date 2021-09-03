package org.folio.innreach.batch.contribution.listener;

import static org.folio.innreach.dto.ContributionDTO.StatusEnum.IN_PROGRESS;
import static org.folio.innreach.dto.MappingValidationStatusDTO.VALID;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.listener.JobExecutionListenerSupport;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import org.folio.innreach.batch.contribution.ContributionJobContext;
import org.folio.innreach.domain.service.ContributionService;
import org.folio.innreach.dto.ContributionDTO;

@Log4j2
@Component
@RequiredArgsConstructor
public class ContributionJobExecutionListener extends JobExecutionListenerSupport {

  private final ContributionService contributionService;

  @Override
  public void beforeJob(JobExecution jobExecution) {
    log.info("Starting contribution job execution: {}", jobExecution);

    var context = toContributionContext(jobExecution);
    var current = contributionService.getCurrent(context.getCentralServerId());

    Assert.isTrue(current.getStatus() == IN_PROGRESS, "Initial contribution is not found");
  }

  @Override
  public void afterJob(JobExecution jobExecution) {
    log.info("Finished contribution job execution: {}", jobExecution);

    var context = toContributionContext(jobExecution);

    contributionService.completeContribution(context.getCentralServerId());
  }

  private ContributionJobContext toContributionContext(JobExecution jobExecution) {
    return new ContributionJobContext(jobExecution.getJobParameters());
  }

}
