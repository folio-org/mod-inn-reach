package org.folio.innreach.Scheduler;

import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.innreach.domain.service.ContributionService;
import org.folio.innreach.domain.service.impl.TenantScopedExecutionService;
import org.folio.innreach.repository.JobExecutionStatusRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;


import static org.folio.innreach.domain.service.impl.CustomTenantService.tenants;

@Service
@AllArgsConstructor
@Log4j2
public class InitialContributionJobScheduler {
  private TenantScopedExecutionService executionService;
  private JobExecutionStatusRepository jobExecutionStatusRepository;
  private ContributionService contributionService;

  @Scheduled(fixedDelay = 30000)
  public void processInitialContributionEvents() {
    log.info("processInitialContributionEvents :: tenantsList {} ", tenants);
    tenants.forEach(tenant ->
      executionService.runTenantScoped(tenant,
        () -> {
          log.info("Fetching jobs for tenant {} ", tenant);
          jobExecutionStatusRepository.updateAndFetchJobExecutionRecordsByStatus()
            .forEach(contributionService::processInitialContributionEvents);
        }
      ));
  }
}
