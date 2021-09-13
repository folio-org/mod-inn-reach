package org.folio.innreach.batch.contribution.listener;

import java.util.List;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.batch.core.listener.ItemListenerSupport;
import org.springframework.stereotype.Component;

import org.folio.innreach.batch.contribution.ContributionJobContext;
import org.folio.innreach.domain.dto.folio.inventorystorage.InstanceIterationEvent;
import org.folio.innreach.domain.service.ContributionService;
import org.folio.innreach.dto.ContributionErrorDTO;
import org.folio.innreach.dto.Instance;

@Log4j2
@Component
@RequiredArgsConstructor
public class ContributionJobExceptionListener extends ItemListenerSupport<InstanceIterationEvent, Instance> {

  private static final UUID UNKNOWN_ID = UUID.fromString("00000000-0000-0000-0000-000000000000");

  private final ContributionService contributionService;
  private final ContributionJobContext context;

  @Override
  public void onReadError(Exception e) {
    logError("reading", e, UNKNOWN_ID);
  }

  @Override
  public void onProcessError(InstanceIterationEvent iterationEvent, Exception e) {
    logError("processing", e, iterationEvent.getInstanceId());
  }

  @Override
  public void onWriteError(Exception e, List<? extends Instance> instances) {
    var instanceId = instances.size() == 1 ? instances.get(0).getId() : UNKNOWN_ID;
    logError("contribution", e, instanceId);
  }

  private void logError(String on, Exception e, UUID instanceId) {
    log.warn("Encountered error on {}", on, e);
    var msg = String.format("Encountered error on %s: %s", on, e.getMessage());

    var error = new ContributionErrorDTO();
    error.setRecordId(instanceId);
    error.setMessage(msg);

    contributionService.logContributionError(context.getContributionId(), error);
  }

}
