package org.folio.innreach.Scheduler;

import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.innreach.domain.entity.JobExecutionStatus;
import org.folio.innreach.domain.service.impl.TenantScopedExecutionService;
import org.folio.innreach.repository.JobExecutionStatusRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import static org.folio.innreach.domain.service.impl.CustomTenantService.tenants;

@Service
@AllArgsConstructor
@Log4j2
public class InitialContributionJobScheduler {
  private TenantScopedExecutionService executionService;
  private JobExecutionStatusRepository jobExecutionStatusRepository;

  @Scheduled(fixedDelay = 30000)
  public void processInitialContributionEvents() {
    log.info("processInitialContributionEvents :: tenantsList {} ", tenants);
    tenants.forEach(tenant ->
      executionService.runTenantScoped(tenant,
        () -> {
          List<JobExecutionStatus> jobs = jobExecutionStatusRepository.updateAndFetchJobExecutionRecordsByStatus();
          log.info("jobs :: {} ", jobs);
        }


      ));
  }
}
