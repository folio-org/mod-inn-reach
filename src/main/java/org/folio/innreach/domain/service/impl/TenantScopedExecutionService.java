package org.folio.innreach.domain.service.impl;

import static org.folio.spring.scope.FolioExecutionScopeExecutionContextManager.getRunnableWithFolioContext;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.scope.FolioExecutionContextSetter;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TenantScopedExecutionService {

  private final FolioExecutionContextBuilder contextBuilder;
  private final SystemUserService systemUserService;

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
    return contextBuilder.forSystemUser(systemUserService.getSystemUser(tenant));
  }

}
