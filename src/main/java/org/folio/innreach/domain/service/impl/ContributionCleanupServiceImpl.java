package org.folio.innreach.domain.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.innreach.domain.entity.ContributionStatus;
import org.folio.innreach.domain.service.ContributionCleanupService;
import org.folio.innreach.repository.JobExecutionStatusRepository;
import org.folio.innreach.repository.OngoingContributionStatusRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Log4j2
public class ContributionCleanupServiceImpl implements ContributionCleanupService {

  private final JobExecutionStatusRepository jobRepo;
  private final OngoingContributionStatusRepository ongoingRepo;

  @Value("${contribution.cleanup.enabled}")
  private boolean cleanupEnabled;

  @Value("${contribution.cleanup.retention-days}")
  private int retentionDays;

  @Value("${contribution.cleanup.batch-size}")
  private int batchSize;

  @Override
  public void cleanup() {
    if (!cleanupEnabled) {
      log.info("Contribution cleanup is disabled, skipping");
      return;
    }

    var terminalStatuses = List.of(
      ContributionStatus.PROCESSED.name(),
      ContributionStatus.FAILED.name(),
      ContributionStatus.DE_CONTRIBUTED.name()
    );
    var cutoff = OffsetDateTime.now().minusDays(retentionDays);

    int initialDeleted = deleteInBatches(jobRepo::deleteBatchByStatusAndUpdatedBefore, terminalStatuses, cutoff);
    int ongoingDeleted = deleteInBatches(ongoingRepo::deleteBatchByStatusAndUpdatedBefore, terminalStatuses, cutoff);

    log.info("Cleanup completed: {} initial contribution records, {} ongoing contribution records purged",
      initialDeleted, ongoingDeleted);
  }

  private int deleteInBatches(BatchDeleter deleter, List<String> statuses, OffsetDateTime cutoff) {
    int totalDeleted = 0;
    int deleted;
    do {
      deleted = deleter.delete(statuses, cutoff, batchSize);
      totalDeleted += deleted;
    } while (deleted >= batchSize);
    return totalDeleted;
  }

  @FunctionalInterface
  private interface BatchDeleter {
    int delete(List<String> statuses, OffsetDateTime cutoff, int batchSize);
  }
}
