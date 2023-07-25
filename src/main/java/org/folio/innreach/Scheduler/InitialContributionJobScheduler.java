package org.folio.innreach.Scheduler;

import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.innreach.batch.contribution.service.ContributionJobRunner;
import org.folio.innreach.domain.entity.TenantInfo;
import org.folio.innreach.domain.service.impl.TenantScopedExecutionService;
import org.folio.innreach.repository.JobExecutionStatusRepository;
import org.folio.innreach.repository.TenantInfoRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;


@Service
@AllArgsConstructor
@Log4j2
public class InitialContributionJobScheduler {
  private TenantScopedExecutionService executionService;
  private JobExecutionStatusRepository jobExecutionStatusRepository;
  private ContributionJobRunner contributionJobRunner;
  private List<String> tenants;
  private TenantInfoRepository tenantRepository;

  public void loadTenants() {
    tenants = tenantRepository.findAll().
        stream().map(TenantInfo::getTenantId).
        distinct().toList();
  }
  @Scheduled(fixedDelayString = "${initial-contribution.scheduler.fixed-delay}", initialDelayString = "${initial-contribution.scheduler.initial-delay}")
  public void processInitialContributionEvents() {
    if(tenants.isEmpty()){
      this.loadTenants();
    }
    log.info("processInitialContributionEvents :: tenantsList {} ", tenants);
    tenants.forEach(tenant ->
      executionService.runTenantScoped(tenant,
        () -> {
          log.info("Fetching jobs for tenant {} ", tenant);
          jobExecutionStatusRepository.updateAndFetchJobExecutionRecordsByStatus()
            .forEach(contributionJobRunner::processInitialContributionEvents);
        }
      ));
  }
}
