package org.folio.innreach.domain.service.impl;

import lombok.extern.log4j.Log4j2;
import org.folio.innreach.domain.entity.TenantInfo;
import org.folio.innreach.repository.TenantInfoRepository;
import org.folio.spring.scope.FolioExecutionContextSetter;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import org.folio.innreach.batch.contribution.service.ContributionJobRunner;
import org.folio.innreach.config.props.TestTenant;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.liquibase.FolioSpringLiquibase;
import org.folio.spring.service.TenantService;
import org.folio.tenant.domain.dto.TenantAttributes;

@Log4j2
@Service
@Primary
@Lazy
public class CustomTenantService extends TenantService {

  private final SystemUserService systemUserService;
  private final ContributionJobRunner contributionJobRunner;
  private final ReferenceDataLoader referenceDataLoader;
  private final TestTenant testTenant;
  private final TenantInfoRepository tenantRepository;
  private final FolioExecutionContextBuilder folioExecutionContextBuilder;


  public CustomTenantService(JdbcTemplate jdbcTemplate, FolioExecutionContext context,
      FolioSpringLiquibase folioSpringLiquibase, SystemUserService systemUserService,
      ContributionJobRunner contributionJobRunner, ReferenceDataLoader referenceDataLoader,
      TestTenant testTenant, TenantInfoRepository tenantRepository,
      FolioExecutionContextBuilder folioExecutionContextBuilder) {
    super(jdbcTemplate, context, folioSpringLiquibase);
    this.systemUserService = systemUserService;
    this.contributionJobRunner = contributionJobRunner;
    this.referenceDataLoader = referenceDataLoader;
    this.testTenant = testTenant;
    this.tenantRepository = tenantRepository;
    this.folioExecutionContextBuilder = folioExecutionContextBuilder;
  }

  @Override
  protected void afterTenantUpdate(TenantAttributes tenantAttributes) {
    String tenantId = context.getTenantId();
    if (!context.getTenantId().startsWith(testTenant.getTenantName())) {
      systemUserService.prepareSystemUser();
    }
    saveTenant(tenantId);
  }

  @Override
  public void loadReferenceData() {
    referenceDataLoader.loadRefData();
  }

  @Override
  public void afterTenantDeletion(TenantAttributes tenantAttributes) {
    String tenantId = context.getTenantId();
    try(var ContextSetter = new FolioExecutionContextSetter(folioExecutionContextBuilder.dbOnlyContext("public"))) {
      tenantRepository.deleteByTenantId(tenantId);
    }
  }
  private void saveTenant(String tenantId) {
    log.info("saveTenant:: tenantId {} ", tenantId);
    try(var ContextSetter = new FolioExecutionContextSetter(folioExecutionContextBuilder.dbOnlyContext("public"))) {
      TenantInfo tenantInfo = tenantRepository.findByTenantId(tenantId);
      if(tenantInfo == null) {
        tenantInfo = new TenantInfo();
        tenantInfo.setTenantId(tenantId);
        tenantRepository.save(tenantInfo);
      }
    }
  }
}
