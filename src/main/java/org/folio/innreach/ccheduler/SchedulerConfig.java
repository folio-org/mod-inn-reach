package org.folio.innreach.ccheduler;

import com.google.common.cache.Cache;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.innreach.domain.entity.TenantInfo;
import org.folio.innreach.domain.service.impl.FolioExecutionContextBuilder;
import org.folio.innreach.domain.service.impl.TenantScopedExecutionService;
import org.folio.innreach.repository.JobExecutionStatusRepository;
import org.folio.innreach.repository.TenantInfoRepository;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.scope.FolioExecutionContextSetter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.List;

@Service
@Log4j2
@RequiredArgsConstructor
public class SchedulerConfig {
  private final TenantScopedExecutionService executionService;
  private final JobExecutionStatusRepository jobExecutionStatusRepository;
  private final Cache<String, List<String>> tenantDetailsCache;
  private final TenantInfoRepository tenantRepository;
  private final FolioExecutionContextBuilder folioExecutionContextBuilder;
  private final FolioExecutionContext folioContext;



  @PostConstruct
  public void initialize(){
    log.info("InitialContributionJobScheduler:: initialize");
//    List<String> tenants = loadTenants();
//    log.info("processInitialContributionEvents :: tenantsList {}", tenants);
//    tenants.forEach(tenant ->
//      executionService.runTenantScoped(tenant,
//        () -> {
//          try {
//          } catch (Exception ex) {
//            log.warn("Exception caught while processing Initial contribution for tenant {} {} ", tenant, ex.getMessage());
//          }
//        }
//      ));
    try (var context = new FolioExecutionContextSetter(folioExecutionContextBuilder.withUserId(folioContext,null))) {
      jobExecutionStatusRepository.updateJobExecutionRecordsByStatus();
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
