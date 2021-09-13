package org.folio.innreach.batch.contribution.listener;

import static org.folio.innreach.dto.ContributionDTO.StatusEnum.IN_PROGRESS;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.listener.JobExecutionListenerSupport;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import org.folio.innreach.batch.contribution.ContributionJobContext;
import org.folio.innreach.domain.service.ContributionService;

@Log4j2
@Component
@RequiredArgsConstructor
public class ContributionJobExecutionListener extends JobExecutionListenerSupport {

  private final ContributionService contributionService;

  @Override
  public void beforeJob(JobExecution jobExecution) {
    log.info("Starting contribution job execution: {}", jobExecution);

    var context = ContributionJobContext.of(jobExecution);
    var centralServerId = context.getCentralServerId();
    var contribution = contributionService.getCurrent(centralServerId);

    Assert.isTrue(contribution.getStatus() == IN_PROGRESS, "Initial contribution is not running for central server = " + centralServerId);
  }

  @Override
  public void afterJob(JobExecution jobExecution) {
    log.info("Finished contribution job execution: {}", jobExecution);

    var context = ContributionJobContext.of(jobExecution);

    contributionService.completeContribution(context.getCentralServerId());
  }

}
