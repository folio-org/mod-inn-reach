package org.folio.innreach.controller;

import liquibase.exception.LiquibaseException;
import org.folio.innreach.domain.service.impl.FolioTenantService;
import org.folio.spring.service.TenantService;
import org.folio.tenant.domain.dto.TenantAttributes;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class FolioTenantControllerTest {
  private static final TenantAttributes TENANT_ATTRIBUTES = new TenantAttributes()
    .moduleTo("mod-innreach-1.0.0");

  @Mock
  private FolioTenantService tenantService;
  @Mock
  private TenantService baseTenantService;
  @InjectMocks
  private FolioTenantController tenantController;

  @Test
  void postTenant_shouldCallTenantInitialize() {
    tenantController.postTenant(TENANT_ATTRIBUTES);

    verify(tenantService).initializeTenant();
  }

  @Test
  void postTenant_shouldNotCallTenantInitialize_liquibaseError() throws Exception {
    doThrow(new LiquibaseException()).when(baseTenantService).createTenant();

    tenantController.postTenant(TENANT_ATTRIBUTES);

    verifyNoInteractions(tenantService);
  }

}
