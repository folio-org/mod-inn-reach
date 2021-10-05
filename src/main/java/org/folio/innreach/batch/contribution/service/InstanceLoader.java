package org.folio.innreach.batch.contribution.service;

import static org.folio.innreach.domain.service.impl.MARCRecordTransformationServiceImpl.isMARCRecord;

import java.util.Objects;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Service;

import org.folio.innreach.batch.contribution.ContributionJobContext;
import org.folio.innreach.domain.dto.folio.inventorystorage.InstanceIterationEvent;
import org.folio.innreach.domain.service.InventoryViewService;
import org.folio.innreach.domain.service.impl.TenantScopedExecutionService;
import org.folio.innreach.dto.Instance;

@Log4j2
@JobScope
@Service
@RequiredArgsConstructor
public class InstanceLoader implements ItemProcessor<InstanceIterationEvent, Instance> {

  private final TenantScopedExecutionService tenantScopedExecutionService;
  private final InventoryViewService inventoryService;
  private final ContributionJobContext context;

  @Override
  public Instance process(InstanceIterationEvent instanceIterationEvent) throws Exception {
    return tenantScopedExecutionService.executeTenantScoped(instanceIterationEvent.getTenant(), () -> {
      log.info("Processing instance iteration event = {}", instanceIterationEvent);

      if (isUnknownEvent(instanceIterationEvent)) {
        log.info("Skipping unknown event...");
        return null;
      }

      // if the returned instance is null it is assumed that processing of the event should not continue
      var instance = inventoryService.getInstance(instanceIterationEvent.getInstanceId());

      if (instance == null) {
        log.info("No instance found by id {}", instanceIterationEvent.getInstanceId());
        return null;
      }

      if (!isMARCRecord(instance)) {
        log.info("Source {} is not supported", instance.getSource());
        return null;
      }

      return instance;
    });
  }

  private boolean isUnknownEvent(InstanceIterationEvent event) {
    return !Objects.equals(event.getJobId(), context.getIterationJobId());
  }

}
