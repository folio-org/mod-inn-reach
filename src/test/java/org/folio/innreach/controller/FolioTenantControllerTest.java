package org.folio.innreach.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import static org.folio.innreach.domain.service.impl.FolioTenantService.LOAD_REF_DATA_PARAMETER;

import liquibase.exception.LiquibaseException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.folio.innreach.domain.service.impl.FolioTenantService;
import org.folio.spring.service.TenantService;
import org.folio.tenant.domain.dto.Parameter;
import org.folio.tenant.domain.dto.TenantAttributes;

@ExtendWith(MockitoExtension.class)
class FolioTenantControllerTest {

  private static final TenantAttributes TENANT_ATTRIBUTES = new TenantAttributes().moduleTo("mod-innreach-1.1.0")
    .addParametersItem(new Parameter().key(LOAD_REF_DATA_PARAMETER).value("true"));

  @Mock
  private FolioTenantService tenantService;
  @Mock
  private TenantService baseTenantService;

  @InjectMocks
  private FolioTenantController tenantController;

  @Test
  void postTenant_shouldCallTenantInitialize() {
    tenantController.postTenant(TENANT_ATTRIBUTES);

    verify(tenantService).initializeTenant(any(TenantAttributes.class));
  }

  @Test
  void postTenant_shouldNotCallTenantInitialize_liquibaseError() throws Exception {
    doThrow(new LiquibaseException()).when(baseTenantService).createTenant();

    tenantController.postTenant(TENANT_ATTRIBUTES);

    verifyNoInteractions(tenantService);
  }

}
