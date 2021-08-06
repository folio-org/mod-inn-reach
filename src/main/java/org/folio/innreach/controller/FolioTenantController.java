package org.folio.innreach.controller;

import lombok.extern.log4j.Log4j2;
import org.folio.innreach.domain.service.impl.FolioTenantService;
import org.folio.spring.controller.TenantController;
import org.folio.spring.service.TenantService;
import org.folio.tenant.domain.dto.TenantAttributes;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Log4j2
@RequestMapping(value = "/_/")
@RestController("folioTenantController")
public class FolioTenantController extends TenantController {

  private final FolioTenantService tenantService;

  public FolioTenantController(TenantService baseTenantService, FolioTenantService tenantService) {
    super(baseTenantService);
    this.tenantService = tenantService;
  }

  @Override
  public ResponseEntity<String> postTenant(TenantAttributes tenantAttributes) {
    var tenantInit = super.postTenant(tenantAttributes);

    if (tenantInit.getStatusCode() == HttpStatus.OK) {
      tenantService.initializeTenant();
    }

    log.info("Tenant init has been completed [response={}]", tenantInit);
    return tenantInit;
  }

}
