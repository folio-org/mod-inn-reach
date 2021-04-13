package org.folio.innreach.controller;

import lombok.extern.log4j.Log4j2;
import org.folio.spring.FolioExecutionContext;
import org.folio.tenant.domain.dto.TenantAttributes;
import org.folio.tenant.rest.resource.TenantApi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.Collections;
import java.util.List;

@Log4j2
@RestController("folioTenantController")
@RequestMapping(value = "/_/")
public class TenantController implements TenantApi {

  private static final String SAMPLES_DIR = "samples";
  private final FolioExecutionContext context;
  private final List<String> samples = Collections.singletonList("dematic.json");

  @Autowired
  public TenantController(FolioExecutionContext context) {
    this.context = context;
  }

  @Override
  public ResponseEntity<String> postTenant(@Valid TenantAttributes tenantAttributes) {

    return ResponseEntity.ok().body("true");
  }
}
