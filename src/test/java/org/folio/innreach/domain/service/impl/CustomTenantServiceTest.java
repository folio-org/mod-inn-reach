package org.folio.innreach.domain.service.impl;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import liquibase.exception.LiquibaseException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import org.folio.innreach.batch.contribution.service.ContributionJobRunner;
import org.folio.innreach.config.props.TestTenant;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.exception.TenantUpgradeException;
import org.folio.spring.liquibase.FolioSpringLiquibase;
import org.folio.tenant.domain.dto.TenantAttributes;

class CustomTenantServiceTest {

  private static final String TEST_TENANT = "test_tenant";
  private static final String TENANT = "tenant";

  @Mock
  private SystemUserService systemUserService;
  @Mock
  private FolioExecutionContext context;
  @Mock
  private ContributionJobRunner contributionJobRunner;
  @Mock
  private ReferenceDataLoader referenceDataLoader;
  @Mock
  private TestTenant testTenant;
  @Mock
  private FolioSpringLiquibase folioSpringLiquibase;

  @InjectMocks
  private CustomTenantService service;

  @BeforeEach
  void setupBeforeEach() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  void should_prepareSystemUser_and_cancelContribJobs() {
    when(context.getTenantId()).thenReturn(TENANT);
    when(testTenant.getTenantName()).thenReturn(TEST_TENANT);

    service.createOrUpdateTenant(new TenantAttributes());

    verify(systemUserService).prepareSystemUser();
    verify(contributionJobRunner).cancelJobs();
  }

  @Test
  void should_not_prepareSystemUser_and_cancelContribJobs_for_testTenant() {
    when(context.getTenantId()).thenReturn(TEST_TENANT);
    when(testTenant.getTenantName()).thenReturn(TEST_TENANT);

    service.createOrUpdateTenant(new TenantAttributes());

    verify(systemUserService, never()).prepareSystemUser();
    verify(contributionJobRunner, never()).cancelJobs();
  }

  @Test
  void shouldNotInitializeTenantIfSystemUserInitFailed() throws LiquibaseException {
    doThrow(new LiquibaseException("failed")).when(folioSpringLiquibase).performLiquibaseUpdate();

    assertThrows(TenantUpgradeException.class,
            () -> service.createOrUpdateTenant(new TenantAttributes()));

    verify(systemUserService, never()).prepareSystemUser();
    verify(contributionJobRunner, never()).cancelJobs();
  }

  @Test
  void should_LoadRefData() {
    service.loadReferenceData();

    verify(referenceDataLoader).loadRefData();
  }

}
