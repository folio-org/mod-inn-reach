package org.folio.innreach.domain.service.impl;

import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
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

import javax.annotation.PostConstruct;

@Log4j2
@Service
@Primary
@Lazy
public class CustomTenantService extends TenantService {
  {
    System.out.println("CustomTenantService bean is about to create....");
  }

  private final SystemUserService systemUserService;
  private final ContributionJobRunner contributionJobRunner;
  private final ReferenceDataLoader referenceDataLoader;
  private final TestTenant testTenant;
  private final FolioExecutionContext folioContext;


  public CustomTenantService(JdbcTemplate jdbcTemplate, FolioExecutionContext context,
                             FolioSpringLiquibase folioSpringLiquibase, SystemUserService systemUserService,
                             ContributionJobRunner contributionJobRunner, ReferenceDataLoader referenceDataLoader, TestTenant testTenant, FolioExecutionContext folioContext) {
    super(jdbcTemplate, context, folioSpringLiquibase);

    this.systemUserService = systemUserService;
    this.contributionJobRunner = contributionJobRunner;
    this.referenceDataLoader = referenceDataLoader;
    this.testTenant = testTenant;
    this.folioContext = folioContext;
  }

  @PostConstruct
  void postConstruct(){
    System.out.println("Inside postConstruct of CustomTenantService");
    System.out.println("folioContext header value "+folioContext.getAllHeaders() + " userId " +folioContext.getUserId());
  }

  @Override
  protected void afterTenantUpdate(TenantAttributes tenantAttributes) {
    log.debug("Debug testing...");
    log.info("FolioExecutionContext value {} , userId {} ",folioContext,folioContext.getUserId());
    log.info("afterTenantUpdate:: parameters tenantAttributes: {} , context tenantId {} , testTenant Name {} ", tenantAttributes, context.getTenantId(),testTenant.getTenantName());
    log.info(!context.getTenantId().startsWith(testTenant.getTenantName()));
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
