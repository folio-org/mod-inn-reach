package org.folio.innreach.Scheduler;

import com.google.common.cache.Cache;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.innreach.batch.contribution.service.ContributionJobRunner;
import org.folio.innreach.domain.entity.TenantInfo;
import org.folio.innreach.domain.service.impl.TenantScopedExecutionService;
import org.folio.innreach.repository.JobExecutionStatusRepository;
import org.folio.innreach.repository.TenantInfoRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;


@Service
@RequiredArgsConstructor
@Log4j2
public class InitialContributionJobScheduler {
  private final TenantScopedExecutionService executionService;
  private final JobExecutionStatusRepository jobExecutionStatusRepository;
  private final ContributionJobRunner contributionJobRunner;
  private final TenantInfoRepository tenantRepository;
  @Value(value = "${initial-contribution.fetch-limit}")
  private int limit;
  private final Cache<String, List<String>>  tenantDetailsCache;
  public List<String> loadTenants() {
    String tenantCacheKey = "tenantList";
    var tenantList = tenantDetailsCache.getIfPresent(tenantCacheKey);
    log.info("tenantList {} ", tenantList);
    if (tenantList == null || tenantList.isEmpty()) {
      log.info("tenant list is empty so loading newly");
      tenantList = tenantRepository.findAll().
        stream().map(TenantInfo::getTenantId).
        distinct().toList();
      tenantDetailsCache.put(tenantCacheKey, tenantList);
    }
    return tenantList;
  }
  @Scheduled(fixedDelayString = "${initial-contribution.scheduler.fixed-delay}", initialDelayString = "${initial-contribution.scheduler.initial-delay}")
  public void processInitialContributionEvents() {
    List<String> tenants = loadTenants();
    log.info("processInitialContributionEvents :: tenantsList {} , fetch limit {} ", tenants, limit);
    tenants.forEach(tenant ->
      executionService.runTenantScoped(tenant,
        () -> {
          log.info("Fetching jobs for tenant {} ", tenant);
          jobExecutionStatusRepository.updateAndFetchJobExecutionRecordsByStatus(limit)
            .forEach(contributionJobRunner::processInitialContributionEvents);
        }
      ));
  }
}
