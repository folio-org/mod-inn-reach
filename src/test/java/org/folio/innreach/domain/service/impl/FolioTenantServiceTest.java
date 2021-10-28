package org.folio.innreach.domain.service.impl;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

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

  public static final TenantAttributes TENANT_ATTRIBUTES = new TenantAttributes()
    .addParametersItem(new Parameter().key(LOAD_REF_DATA_PARAMETER).value("true"));

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
    service.initializeTenant(TENANT_ATTRIBUTES);

    verify(systemUserService).prepareSystemUser();
    verify(contributionJobRunner).cancelJobs();
    verify(referenceDataLoader).loadRefData();
  }

  @Test
  void shouldNotLoadRefData() {
    var tenantAttributes = new TenantAttributes()
      .addParametersItem(new Parameter().key(LOAD_REF_DATA_PARAMETER).value("false"));

    service.initializeTenant(tenantAttributes);

    verify(systemUserService).prepareSystemUser();
    verify(contributionJobRunner).cancelJobs();
    verifyNoInteractions(referenceDataLoader);
  }

  @Test
  void shouldNotLoadRefData_noParam() {
    var tenantAttributes = new TenantAttributes();

    service.initializeTenant(tenantAttributes);

    verify(systemUserService).prepareSystemUser();
    verify(contributionJobRunner).cancelJobs();
    verifyNoInteractions(referenceDataLoader);
  }

  @Test
  void shouldNotInitializeTenantIfSystemUserInitFailed() {
    doThrow(new RuntimeException("test")).when(systemUserService).prepareSystemUser();

    assertThrows(RuntimeException.class, () -> service.initializeTenant(TENANT_ATTRIBUTES));
  }

}
