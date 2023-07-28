package org.folio.innreach.batch.contribution.listener;

import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;

import static org.folio.innreach.batch.contribution.ContributionJobContextManager.getContributionJobContext;

import java.util.UUID;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

import org.folio.innreach.domain.service.ContributionService;
import org.folio.innreach.dto.ContributionErrorDTO;
import org.springframework.stereotype.Service;

@Log4j2
@RequiredArgsConstructor
public class ContributionExceptionListener {

  protected static final UUID UNKNOWN_ID = UUID.fromString("00000000-0000-0000-0000-000000000000");

  private final ContributionService contributionService;
  private final String stepName;

  protected void logError(Exception e, UUID recordId, String stepStage) {
    try {
      log.warn("Step: [{}] error on stage {} record Id {} e: {}", stepName, stepStage, recordId, e);
      var msg = String.format("Step: [%s] error on stage %s: %s", stepName, stepStage, e.getMessage());
      var error = new ContributionErrorDTO();
      error.setRecordId(recordId);
      error.setMessage(msg);

      contributionService.logContributionError(getContributionJobContext().getContributionId(), error);
    } catch (Exception ex) {
      log.warn("Can't persist record {} contribution error: {}", recordId, ex);
    }
  }

  public void logReaderError(Exception e) {
    logError(e, UNKNOWN_ID, "read");
  }

  public void logWriteError(Exception e, UUID recordId) {
    logError(e, defaultIfNull(recordId, UNKNOWN_ID), "write");
  }
  public void logError(Exception e, UUID recordId, UUID contributionId) {
    try {
      log.warn("Step: [{}] error on record Id {} e: {}", stepName, recordId, e);
      var msg = String.format("Step: [%s] error on stage : %s", stepName, e.getMessage());
      var error = new ContributionErrorDTO();
      error.setRecordId(recordId);
      error.setMessage(msg);
      contributionService.logContributionError(contributionId, error);
    } catch (Exception ex) {
      log.warn("Can't persist record {} contribution error: {}", recordId, ex);
    }
  }

  public void logProcessError(Exception e, UUID recordId) {
    logError(e, defaultIfNull(recordId, UNKNOWN_ID), "process");
  }

}
