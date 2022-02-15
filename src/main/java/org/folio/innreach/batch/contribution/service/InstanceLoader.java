package org.folio.innreach.batch.contribution.service;

import java.util.Objects;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import org.folio.innreach.batch.contribution.ContributionJobContextManager;
import org.folio.innreach.domain.dto.folio.inventorystorage.InstanceIterationEvent;
import org.folio.innreach.domain.service.ContributionValidationService;
import org.folio.innreach.domain.service.InventoryViewService;
import org.folio.innreach.dto.Instance;

@Log4j2
@Service
@RequiredArgsConstructor
public class InstanceLoader {

  private final InventoryViewService inventoryService;
  private final ContributionValidationService validationService;

  public Instance load(InstanceIterationEvent event) {
    log.info("Processing instance iteration event = {}", event);

    if (isUnknownEvent(event)) {
      log.info("Skipping unknown event, current job is {}", getIterationJobId());
      return null;
    }

    var instanceId = event.getInstanceId();

    var instance = inventoryService.getInstance(instanceId);

    // if the returned instance is null it is assumed that processing of the event should not continue
    if (instance == null) {
      log.info("No instance found by id {}", instanceId);
      return null;
    }

    if (!validationService.isEligibleForContribution(getCentralServerId(), instance)) {
      log.info("Instance {} is not eligible for contribution", instanceId);
      return null;
    }

    log.info("Loaded instance with hrid {}", instance.getHrid());

    return instance;
  }

  private boolean isUnknownEvent(InstanceIterationEvent event) {
    var iterationJobId = getIterationJobId();
    return !Objects.equals(event.getJobId(), iterationJobId);
  }

  private UUID getIterationJobId() {
    return ContributionJobContextManager.getContributionJobContext().getIterationJobId();
  }

  private UUID getCentralServerId() {
    return ContributionJobContextManager.getContributionJobContext().getCentralServerId();
  }

}
