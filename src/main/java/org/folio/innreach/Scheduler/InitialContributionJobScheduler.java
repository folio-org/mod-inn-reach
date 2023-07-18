package org.folio.innreach.Scheduler;

import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.innreach.domain.service.TenantsHolder;
import org.folio.innreach.domain.service.impl.TenantScopedExecutionService;
import org.folio.innreach.repository.JobExecutionStatusRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
@Log4j2
public class InitialContributionJobScheduler {
  private TenantsHolder tenants;
  private TenantScopedExecutionService executionService;
  private JobExecutionStatusRepository jobExecutionStatusRepository;

  @Scheduled(fixedDelay = 2000)
  public void processInitialContributionEvents() {
    log.info("processInitialContributionEvents :: tenantsList {} ", tenants.getAll());
    tenants.getAll().forEach(tenant ->
      executionService.runTenantScoped(tenant,
        () ->
          log.info("Count of Job ExecutionStatus Repository table is {} ", jobExecutionStatusRepository.count())
      ));
  }
}
