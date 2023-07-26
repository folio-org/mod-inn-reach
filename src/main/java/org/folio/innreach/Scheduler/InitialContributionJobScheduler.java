package org.folio.innreach.Scheduler;

import com.google.common.cache.Cache;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.innreach.batch.contribution.service.ContributionJobRunner;
import org.folio.innreach.domain.entity.TenantInfo;
import org.folio.innreach.domain.service.ContributionService;
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
  private final ContributionService contributionService;
  @Value(value = "${initial-contribution.fetch-limit}")
  private int recordLimit;
  private final Cache<String, List<String>> tenantDetailsCache;

  @Scheduled(fixedDelayString = "${initial-contribution.scheduler.fixed-delay}", initialDelayString = "${initial-contribution.scheduler.initial-delay}")
  public void processInitialContributionEvents() {
    List<String> tenants = loadTenants();
    log.info("processInitialContributionEvents :: tenantsList {}", tenants);
    tenants.forEach(tenant ->
      executionService.runTenantScoped(tenant,
        () -> {
          try {
            jobExecutionStatusRepository.updateAndFetchJobExecutionRecordsByStatus(recordLimit)
              .forEach(contributionJobRunner::processInitialContributionEvents);
          } catch (Exception ex) {
            log.warn("Exception caught while processing Initial contribution for tenant {} {} ", tenant, ex.getMessage());
          }
        }
      ));
  }

  @Scheduled(fixedDelay = 60000 * 2,
    initialDelayString = "${initial-contribution.scheduler.initial-delay}")
  public void updateContributionStatistics() {
    List<String> tenants = loadTenants();
    log.info("updateContributionStatistics :: tenantsList {}", tenants);
    tenants.forEach(tenant ->
      executionService.runTenantScoped(tenant,
        () -> {
          try {
            contributionService.updateInProgressContributionStatistics();
          } catch (Exception ex) {
            log.warn("Exception caught while updating statistics for tenant {} ", tenant, ex);
          }
        }
      ));
  }

  private List<String> loadTenants() {
    String tenantCacheKey = "tenantList";
    var tenantList = tenantDetailsCache.getIfPresent(tenantCacheKey);
    if (tenantList == null) {
      tenantList = tenantRepository.findAll().
        stream().map(TenantInfo::getTenantId).
        distinct().toList();
      tenantDetailsCache.put(tenantCacheKey, tenantList);
    }
    return tenantList;
  }
}
