package org.folio.innreach.domain.service.impl;

import static org.folio.spring.scope.FolioExecutionScopeExecutionContextManager.getRunnableWithFolioContext;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.folio.spring.FolioExecutionContext;
import org.springframework.stereotype.Service;
import org.folio.spring.service.SystemUserService;

@Service
@RequiredArgsConstructor
public class TenantScopedExecutionService {

  private final InnReachFolioExecutionContextBuilder contextBuilder;
  private final SystemUserService systemUserService;

  @SneakyThrows
  public void runTenantScoped(String tenantId, Runnable job) {
    getRunnableWithFolioContext(folioExecutionContext(tenantId), job).run();
  }

  private FolioExecutionContext folioExecutionContext(String tenant) {
    return contextBuilder.forSystemUser(systemUserService.getAuthedSystemUser(tenant));
  }

}
