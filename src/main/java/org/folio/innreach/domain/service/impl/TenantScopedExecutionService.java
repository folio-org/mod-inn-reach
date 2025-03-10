package org.folio.innreach.domain.service.impl;

import static org.folio.spring.scope.FolioExecutionScopeExecutionContextManager.getRunnableWithFolioContext;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.context.ExecutionContextBuilder;
import org.folio.spring.scope.FolioExecutionContextSetter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.folio.spring.service.SystemUserService;

@Service
@RequiredArgsConstructor
public class TenantScopedExecutionService {

  private final ExecutionContextBuilder contextBuilder;
  private SystemUserService systemUserService;

  @Autowired(required = false)
  public void setSystemUserService(SystemUserService systemUserService) {
    this.systemUserService = systemUserService;
  }

  @SneakyThrows
  public void runTenantScoped(String tenantId, Runnable job) {
    getRunnableWithFolioContext(folioExecutionContext(tenantId), job).run();
  }

  public void executeAsyncTenantScoped(String tenantId, Runnable job) {
    try (var fex = new FolioExecutionContextSetter(folioExecutionContext(tenantId))) {
      job.run();
    }
  }

  private FolioExecutionContext folioExecutionContext(String tenant) {
    return systemUserService != null
      ? contextBuilder.forSystemUser(systemUserService.getAuthedSystemUser(tenant), null)
      : contextBuilder.buildContext(tenant);
  }
}
