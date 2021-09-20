package org.folio.innreach.batch.contribution.service;

import java.util.Objects;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Service;

import org.folio.innreach.batch.contribution.ContributionJobContext;
import org.folio.innreach.domain.dto.folio.inventorystorage.InstanceIterationEvent;
import org.folio.innreach.domain.service.InventoryService;
import org.folio.innreach.domain.service.impl.TenantScopedExecutionService;
import org.folio.innreach.dto.Instance;

@Log4j2
@JobScope
@Service
@RequiredArgsConstructor
public class InstanceLoader implements ItemProcessor<InstanceIterationEvent, Instance> {

  private final TenantScopedExecutionService tenantScopedExecutionService;
  private final InventoryService inventoryService;
  private final ContributionJobContext context;

  @Override
  public Instance process(InstanceIterationEvent instanceIterationEvent) throws Exception {
    return tenantScopedExecutionService.executeTenantScoped(instanceIterationEvent.getTenant(), () -> {
      log.info("Processing instance iteration event = {}", instanceIterationEvent);

      if (shouldSkip(instanceIterationEvent)) {
        log.info("Skipping event...");
        return null;
      }

      // if the returned instance is null it is assumed that processing of the instance should not continue
      return inventoryService.getInstance(instanceIterationEvent.getInstanceId());
    });
  }

  private boolean shouldSkip(InstanceIterationEvent event) {
    return !Objects.equals(event.getJobId(), context.getIterationJobId());
  }

}
