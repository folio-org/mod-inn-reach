package org.folio.innreach.scheduler;

import com.google.common.cache.Cache;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.innreach.batch.contribution.service.InitialContributionEventProcessor;
import org.folio.innreach.batch.contribution.service.OngoingContributionEventProcessor;
import org.folio.innreach.domain.entity.TenantInfo;
import org.folio.innreach.domain.service.ContributionService;
import org.folio.innreach.domain.service.impl.TenantScopedExecutionService;
import org.folio.innreach.repository.JobExecutionStatusRepository;
import org.folio.innreach.repository.OngoingContributionStatusRepository;
import org.folio.innreach.repository.TenantInfoRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.locks.ReentrantLock;


@Service
@RequiredArgsConstructor
@Log4j2
public class ContributionJobScheduler {
  private final TenantScopedExecutionService tenantScopedExecutionService;
  private final JobExecutionStatusRepository jobExecutionStatusRepository;
  private final InitialContributionEventProcessor eventProcessor;
  private final TenantInfoRepository tenantRepository;
  private final ContributionService contributionService;
  private final OngoingContributionStatusRepository ongoingContributionStatusRepository;
  private final OngoingContributionEventProcessor ongoingContributionEventProcessor;
  @Value(value = "${contribution.fetch-limit}")
  private int recordLimit;
  @Value(value = "${contribution.item-pause}")
  private double itemPause;
  // Minimum number of available slots (recordLimit - inProgressCount) required before
  // claiming a new batch. If available < minBatchThreshold, the scheduler skips
  // this tick for the tenant. Value depends on fetch-limit and scheduler.fixed-delay.
  @Value(value = "${contribution.min-batch-threshold}")
  private int minBatchThreshold;
  private final Cache<String, List<String>> tenantDetailsCache;

  // Guards against re-entrant scheduler ticks: if the previous tick's tenant loop
  // is still running, the next tick skips instead of piling up overlapping work.
  private final ReentrantLock initialContributionLock = new ReentrantLock();
  private final ReentrantLock ongoingContributionLock = new ReentrantLock();

  @PostConstruct
  public void postConstruct() {
    try {
      this.loadTenants()
        .forEach(tenantId ->
          tenantScopedExecutionService.runTenantScoped(tenantId, () -> {
            jobExecutionStatusRepository.updateInProgressRecordsToReady();
            ongoingContributionStatusRepository.updateInProgressToReady();
          }));
      log.info("postConstruct:: Status of the records updated to Ready successfully");
    } catch (Exception ex) {
      log.warn("postConstruct:: Error while updating the record status from In progress to ready {}", ex.getMessage());
    }
  }

  @Scheduled(fixedDelayString = "${contribution.initial.scheduler.fixed-delay}",
    initialDelayString = "${contribution.initial.scheduler.initial-delay}")
  public void processInitialContributionEvents() {
    if (!initialContributionLock.tryLock()) {
      log.debug("processInitialContributionEvents:: previous tick still running, skipping");
      return;
    }
    try {
      List<String> tenants = loadTenants();
      log.debug("processInitialContributionEvents :: tenantsList {}", tenants);
      tenants.forEach(tenant ->
        tenantScopedExecutionService.runTenantScoped(tenant, () -> processInitialContributionForTenant(tenant)));
    } finally {
      initialContributionLock.unlock();
    }
  }

  private void processInitialContributionForTenant(String tenant) {
    try {
      long inProgressCount = jobExecutionStatusRepository.getInProgressRecordsCount();
      long available = recordLimit - inProgressCount;
      if (available >= minBatchThreshold) {
        var newRecordsToProcess = jobExecutionStatusRepository.updateAndFetchJobExecutionRecordsByStatus((int) available, itemPause);
        if (!newRecordsToProcess.isEmpty()) {
          log.info("processInitialContributionEvents:: Fetched new set of {} initial contribution records (available={})",
            newRecordsToProcess.size(), available);
          newRecordsToProcess.forEach(eventProcessor::processInitialContributionEvents);
        }
      } else {
        log.info("processInitialContributionEvents:: skipping tick for tenant {}, " +
          "available {} is below minBatchThreshold {} (inProgress={}, fetchLimit={})",
          tenant, available, minBatchThreshold, inProgressCount, recordLimit);
      }
      contributionService.updateStatisticsAndContributionStatus();
    } catch (Exception ex) {
      log.warn("Exception caught while processing Initial contribution for tenant {} {} ", tenant, ex.getMessage(), ex);
    }
  }

  @Scheduled(fixedDelayString = "${contribution.ongoing.scheduler.fixed-delay}",
    initialDelayString = "${contribution.ongoing.scheduler.initial-delay}")
  public void processOngoingContributionEvents() {
    if (!ongoingContributionLock.tryLock()) {
      log.debug("processOngoingContributionEvents:: previous tick still running, skipping");
      return;
    }
    try {
      List<String> tenants = loadTenants();
      log.debug("processOngoingContributionEvents :: tenantsList {}", tenants);
      tenants.forEach(tenant ->
        tenantScopedExecutionService.runTenantScoped(tenant, () -> processOngoingContributionForTenant(tenant)));
    } finally {
      ongoingContributionLock.unlock();
    }
  }

  private void processOngoingContributionForTenant(String tenant) {
    try {
      long inProgressCount = ongoingContributionStatusRepository.getInProgressRecordsCount();
      long available = recordLimit - inProgressCount;
      if (available >= minBatchThreshold) {
        var newRecordsToProcess = ongoingContributionStatusRepository.updateAndFetchOngoingContributionRecordsByStatus((int) available);
        if (!newRecordsToProcess.isEmpty()) {
          log.info("processOngoingContributionEvents:: Fetched new set of {} ongoing contribution records (available={})",
            newRecordsToProcess.size(), available);
          newRecordsToProcess.forEach(ongoingContributionEventProcessor::processOngoingContribution);
        }
      } else {
        log.info("processOngoingContributionEvents:: skipping tick for tenant {}, " +
          "available {} is below minBatchThreshold {} (inProgress={}, fetchLimit={})",
          tenant, available, minBatchThreshold, inProgressCount, recordLimit);
      }
    } catch (Exception ex) {
      log.warn("processOngoingContributionEvents:: Exception caught while processing ongoing contribution for tenant {} {} ", tenant, ex.getMessage());
    }
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
