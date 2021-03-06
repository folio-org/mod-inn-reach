package org.folio.innreach.domain.service.impl;

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

@Service
@Primary
@Lazy
public class CustomTenantService extends TenantService {

  private final SystemUserService systemUserService;
  private final ContributionJobRunner contributionJobRunner;
  private final ReferenceDataLoader referenceDataLoader;
  private final TestTenant testTenant;


  public CustomTenantService(JdbcTemplate jdbcTemplate, FolioExecutionContext context,
      FolioSpringLiquibase folioSpringLiquibase, SystemUserService systemUserService,
      ContributionJobRunner contributionJobRunner, ReferenceDataLoader referenceDataLoader, TestTenant testTenant) {
    super(jdbcTemplate, context, folioSpringLiquibase);

    this.systemUserService = systemUserService;
    this.contributionJobRunner = contributionJobRunner;
    this.referenceDataLoader = referenceDataLoader;
    this.testTenant = testTenant;
  }

  @Override
  protected void afterTenantUpdate(TenantAttributes tenantAttributes) {
    if (!context.getTenantId().startsWith(testTenant.getTenantName())) {
      systemUserService.prepareSystemUser();
      contributionJobRunner.cancelJobs();
    }
  }

  @Override
  public void loadReferenceData() {
    referenceDataLoader.loadRefData();
  }

}
