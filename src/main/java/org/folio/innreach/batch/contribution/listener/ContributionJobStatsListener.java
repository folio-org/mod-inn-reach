package org.folio.innreach.batch.contribution.listener;

import static org.folio.innreach.batch.contribution.ContributionJobContextManager.getContributionJobContext;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;

import org.folio.innreach.batch.contribution.ContributionJobContext;
import org.folio.innreach.domain.service.ContributionService;
import org.folio.innreach.dto.ContributionDTO;

@Log4j2
@Component
@RequiredArgsConstructor
public class ContributionJobStatsListener {

  private final ContributionService contributionService;

  public void updateStats(ContributionJobContext.Statistics statistics) {
    var centralServerId = getContributionJobContext().getCentralServerId();
    var stats = collectStats(statistics);

    contributionService.updateContributionStats(centralServerId, stats);
  }

  private ContributionDTO collectStats(ContributionJobContext.Statistics stats) {
    var contribution = new ContributionDTO();
    contribution.setRecordsContributed((long) stats.getWriteCount());
    contribution.setRecordsProcessed(contribution.getRecordsContributed() + stats.getWriteSkipCount());
    contribution.setRecordsTotal((long) (stats.getReadCount()));
    return contribution;
  }

}
