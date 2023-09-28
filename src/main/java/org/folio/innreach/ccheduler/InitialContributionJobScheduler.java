package org.folio.innreach.ccheduler;

import com.google.common.cache.Cache;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.innreach.batch.contribution.service.InitialContributionEventProcessor;
import org.folio.innreach.domain.entity.TenantInfo;
import org.folio.innreach.domain.service.ContributionService;
import org.folio.innreach.domain.service.impl.TenantScopedExecutionService;
import org.folio.innreach.repository.JobExecutionStatusRepository;
import org.folio.innreach.repository.TenantInfoRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.List;


@Service
@RequiredArgsConstructor
@Log4j2
public class InitialContributionJobScheduler {
  private final TenantScopedExecutionService tenantScopedExecutionService;
  private final JobExecutionStatusRepository jobExecutionStatusRepository;
  private final InitialContributionEventProcessor eventProcessor;
  private final TenantInfoRepository tenantRepository;
  private final ContributionService contributionService;
  @Value(value = "${initial-contribution.fetch-limit}")
  private int recordLimit;
  @Value(value = "${initial-contribution.item-pause}")
  private int itemPause;
  private final Cache<String, List<String>> tenantDetailsCache;

  @PostConstruct
  public void postConstruct() {
    try {
      this.loadTenants()
        .forEach(tenantId ->
          tenantScopedExecutionService.runTenantScoped(tenantId,
              jobExecutionStatusRepository::updateInProgressRecordsToReady));
    } catch (Exception ex) {
      log.warn("postConstruct:: Error while updating the record status from In progress to ready {}", ex.getMessage());
    }
  }

  @Scheduled(fixedDelayString = "${initial-contribution.scheduler.fixed-delay}",
    initialDelayString = "${initial-contribution.scheduler.initial-delay}")
  public void processInitialContributionEvents() {
    List<String> tenants = loadTenants();
    log.info("processInitialContributionEvents :: tenantsList {}", tenants);
    tenants.forEach(tenant ->
      tenantScopedExecutionService.runTenantScoped(tenant,
        () -> {
          try {
            long inProgressCount = jobExecutionStatusRepository.getInProgressRecordsCount();
            if(recordLimit> inProgressCount) {
              log.info("processInitialContributionEvents:: Fetching new set of records");
              jobExecutionStatusRepository.updateAndFetchJobExecutionRecordsByStatus(recordLimit, itemPause)
                .forEach(eventProcessor::processInitialContributionEvents);
            } else {
              log.info("processInitialContributionEvents:: unable to fetch new records, " +
                "as inProgress count {} is greater than fetchLimit {}", inProgressCount, recordLimit);
            }
            contributionService.updateStatisticsAndContributionStatus();
          } catch (Exception ex) {
            log.warn("Exception caught while processing Initial contribution for tenant {} {} ", tenant, ex.getMessage());
          }
        }
      ));
  }

  private List<String> loadTenants() {
    String tenantCacheKey = "tenantList";
    var tenantList = tenantDetailsCache.getIfPresent(tenantCacheKey);
    if (tenantList == null) {
      log.info("Tenant details Refreshed");
      tenantList = tenantRepository.findAll().
        stream().map(TenantInfo::getTenantId).
        distinct().toList();
      tenantDetailsCache.put(tenantCacheKey, tenantList);
    }
    return tenantList;
  }
}
