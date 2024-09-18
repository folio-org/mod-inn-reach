package org.folio.innreach.domain.service.impl;

import lombok.extern.log4j.Log4j2;
import org.folio.innreach.config.props.TestTenant;
import org.folio.innreach.domain.entity.TenantInfo;
import org.folio.innreach.repository.TenantInfoRepository;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.liquibase.FolioSpringLiquibase;
import org.folio.spring.service.PrepareSystemUserService;
import org.folio.spring.service.TenantService;
import org.folio.tenant.domain.dto.TenantAttributes;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Log4j2
@Service
@Primary
@Lazy
public class CustomTenantService extends TenantService {

  private final PrepareSystemUserService systemUserService;
  private final ReferenceDataLoader referenceDataLoader;
  private final TestTenant testTenant;
  private final TenantInfoRepository tenantRepository;

  @Value("${folio.isEureka}")
  private Boolean isEureka = false;


  public CustomTenantService(JdbcTemplate jdbcTemplate, FolioExecutionContext context,
      FolioSpringLiquibase folioSpringLiquibase, PrepareSystemUserService systemUserService,
      ReferenceDataLoader referenceDataLoader, TestTenant testTenant, TenantInfoRepository tenantRepository) {
    super(jdbcTemplate, context, folioSpringLiquibase);
    this.systemUserService = systemUserService;
    this.referenceDataLoader = referenceDataLoader;
    this.testTenant = testTenant;
    this.tenantRepository = tenantRepository;
  }

  @Override
  protected void afterTenantUpdate(TenantAttributes tenantAttributes) {
    log.debug("afterTenantUpdate:: parameters tenantAttributes: {}", tenantAttributes);
    if (!context.getTenantId().startsWith(testTenant.getTenantName())) {
      if(!isEureka) {
        systemUserService.setupSystemUser();
      }
      saveTenant();
    }
  }

  @Override
  public void loadReferenceData() {
    referenceDataLoader.loadRefData();
  }

  @Override
  public void afterTenantDeletion(TenantAttributes tenantAttributes) {
    tenantRepository.deleteByTenantId(context.getTenantId());
  }

  private void saveTenant() {
    String tenantId = context.getTenantId();
    log.info("saveTenant:: tenantId {} ", tenantId);
      TenantInfo tenantInfo = tenantRepository.findByTenantId(tenantId);
      if(tenantInfo == null) {
        tenantInfo = new TenantInfo();
        tenantInfo.setTenantId(tenantId);
        tenantRepository.save(tenantInfo);
      }

  }
}
