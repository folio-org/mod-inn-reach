package org.folio.innreach.domain.service.impl;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import liquibase.exception.LiquibaseException;

import org.folio.innreach.repository.TenantInfoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.folio.innreach.batch.contribution.service.ContributionJobRunner;
import org.folio.innreach.config.props.TestTenant;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.FolioModuleMetadata;
import org.folio.spring.exception.TenantUpgradeException;
import org.folio.spring.liquibase.FolioSpringLiquibase;
import org.folio.tenant.domain.dto.TenantAttributes;
import org.folio.spring.service.PrepareSystemUserService;

@ExtendWith(MockitoExtension.class)
class CustomTenantServiceTest {

  private static final String TEST_TENANT = "test_tenant";
  private static final String TENANT = "tenant";
  private static final String TENANT_SCHEMA = "db_tenant";

  @Mock
  private PrepareSystemUserService systemUserService;
  @Mock
  private FolioExecutionContext context;
  @Mock
  private ContributionJobRunner contributionJobRunner;
  @Mock
  private ReferenceDataLoader referenceDataLoader;
  @Mock
  private TestTenant testTenant;
  @Mock
  private FolioModuleMetadata moduleMetadata;
  @Mock
  private FolioSpringLiquibase folioSpringLiquibase;
  @Mock
  private TenantInfoRepository tenantRepository;

  @InjectMocks
  private CustomTenantService service;


  @Test
  void should_prepareSystemUser_and_cancelContribJobs() {
    mockTenantName(TENANT);
    mockTenantSchemaName();

    service.createOrUpdateTenant(new TenantAttributes());

    verify(systemUserService).setupSystemUser();
  }

  @Test
  void should_not_prepareSystemUser_and_cancelContribJobs_for_testTenant() {
    mockTenantName(TEST_TENANT);
    mockTenantSchemaName();

    service.createOrUpdateTenant(new TenantAttributes());

    verify(systemUserService, never()).setupSystemUser();
    verify(contributionJobRunner, never()).cancelJobs();
  }

  @Test
  void shouldNotInitializeTenantIfSystemUserInitFailed() throws LiquibaseException {
    mockTenantSchemaName();
    doThrow(new LiquibaseException("failed")).when(folioSpringLiquibase).performLiquibaseUpdate();

    TenantAttributes attributes = new TenantAttributes();
    assertThrows(TenantUpgradeException.class, () -> service.createOrUpdateTenant(attributes));

    verify(systemUserService, never()).setupSystemUser();
    verify(contributionJobRunner, never()).cancelJobs();
  }

  @Test
  void should_LoadRefData() {
    service.loadReferenceData();

    verify(referenceDataLoader).loadRefData();
  }

  private void mockTenantName(String name) {
    when(context.getTenantId()).thenReturn(name);
    when(testTenant.getTenantName()).thenReturn(TEST_TENANT);
  }

  private void mockTenantSchemaName() {
    when(context.getFolioModuleMetadata()).thenReturn(moduleMetadata);
    when(moduleMetadata.getDBSchemaName(any())).thenReturn(TENANT_SCHEMA);
  }

}
