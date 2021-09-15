package org.folio.innreach.batch.contribution.listener;

import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;

import java.util.UUID;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.batch.core.listener.ItemListenerSupport;
import org.springframework.beans.factory.annotation.Value;

import org.folio.innreach.batch.contribution.ContributionJobContext;
import org.folio.innreach.domain.service.ContributionService;
import org.folio.innreach.dto.ContributionErrorDTO;

@Log4j2
@RequiredArgsConstructor
public abstract class ContributionExceptionListener<K, V> extends ItemListenerSupport<K, V> {

  protected static final UUID UNKNOWN_ID = UUID.fromString("00000000-0000-0000-0000-000000000000");

  private final ContributionService contributionService;
  private final ContributionJobContext jobContext;

  @Value("#{stepExecution.stepName}")
  private String stepName;

  protected void logError(Exception e, UUID recordId, String stepStage) {
    log.warn("Step: [{}] error on stage {}", stepName, stepStage, e);
    var msg = String.format("Step: [%s] error on stage %s: %s", stepName, stepStage, e.getMessage());

    var error = new ContributionErrorDTO();
    error.setRecordId(recordId);
    error.setMessage(msg);

    contributionService.logContributionError(jobContext.getContributionId(), error);
  }

  protected void logReaderError(Exception e) {
    logError(e, UNKNOWN_ID, "read");
  }

  protected void logWriteError(Exception e, UUID recordId) {
    logError(e, defaultIfNull(recordId, UNKNOWN_ID), "write");
  }

  protected void logProcessError(Exception e, UUID recordId) {
    logError(e, defaultIfNull(recordId, UNKNOWN_ID), "process");
  }

}
