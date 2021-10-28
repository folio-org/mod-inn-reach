package org.folio.innreach.domain.service.impl;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import static org.folio.innreach.domain.service.impl.FolioTenantService.LOAD_REF_DATA_PARAMETER;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import org.folio.innreach.batch.contribution.service.ContributionJobRunner;
import org.folio.spring.FolioExecutionContext;
import org.folio.tenant.domain.dto.Parameter;
import org.folio.tenant.domain.dto.TenantAttributes;

class FolioTenantServiceTest {

  @Mock
  private SystemUserService systemUserService;

  @Mock
  private FolioExecutionContext context;

  @Mock
  private ContributionJobRunner contributionJobRunner;

  @Mock
  private ReferenceDataLoader referenceDataLoader;

  @InjectMocks
  private FolioTenantService service;

  @BeforeEach
  void setupBeforeEach() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  void shouldInitializeTenant() {
    var tenantAttributes = new TenantAttributes()
      .addParametersItem(new Parameter().key(LOAD_REF_DATA_PARAMETER).value("true"));

    service.initializeTenant(tenantAttributes);

    verify(systemUserService).prepareSystemUser();
    verify(contributionJobRunner).cancelJobs();
    verify(referenceDataLoader).loadRefData();
  }

  @Test
  void shouldInitializeTenantIfSystemUserInitFailed() {
    doThrow(new RuntimeException("test")).when(systemUserService).prepareSystemUser();

    assertThrows(RuntimeException.class, () -> service.initializeTenant(new TenantAttributes()));
  }

}
