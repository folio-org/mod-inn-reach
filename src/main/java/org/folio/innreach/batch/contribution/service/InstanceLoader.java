package org.folio.innreach.batch.contribution.service;

import static org.folio.innreach.domain.service.impl.MARCRecordTransformationServiceImpl.isMARCRecord;

import java.util.Objects;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Service;

import org.folio.innreach.batch.contribution.ContributionJobContextManager;
import org.folio.innreach.domain.dto.folio.inventorystorage.InstanceIterationEvent;
import org.folio.innreach.domain.service.InventoryViewService;
import org.folio.innreach.dto.Instance;

@Log4j2
@Service
@RequiredArgsConstructor
public class InstanceLoader implements ItemProcessor<InstanceIterationEvent, Instance> {

  private final InventoryViewService inventoryService;

  @Override
  public Instance process(InstanceIterationEvent event) throws Exception {
    log.info("Processing instance iteration event = {}", event);

    if (isUnknownEvent(event)) {
      log.info("Skipping unknown event, current job is {}", getIterationJobId());
      return null;
    }

    // if the returned instance is null it is assumed that processing of the event should not continue
    var instance = inventoryService.getInstance(event.getInstanceId());

    if (instance == null) {
      log.info("No instance found by id {}", event.getInstanceId());
      return null;
    }

    if (!isMARCRecord(instance)) {
      log.info("Source {} is not supported", instance.getSource());
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

}
