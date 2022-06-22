package org.folio.innreach.domain.service.impl;

import static org.folio.innreach.domain.service.impl.FolioExecutionContextUtils.executeWithinContext;

import java.util.concurrent.Callable;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;

import org.springframework.stereotype.Service;

import org.folio.spring.FolioExecutionContext;

@Service
@RequiredArgsConstructor
public class TenantScopedExecutionService {

  private final FolioExecutionContextBuilder contextBuilder;
  private final SystemUserService systemUserService;

  /**
   * Executes given job tenant scoped.
   *
   * @param tenantId - The tenant name.
   * @param job      - Job to be executed in tenant scope.
   * @param <T>      - Optional return value for the job.
   * @return Result of job.
   * @throws RuntimeException - Wrapped exception from the job.
   */
  public <T> T executeTenantScoped(String tenantId, Callable<T> job) {
    return executeWithinContext(folioExecutionContext(tenantId), job);
  }

  @SneakyThrows
  public void runTenantScoped(String tenantId, Runnable job) {
    executeTenantScoped(tenantId, () -> {
      job.run();
      return null;
    });
  }

  private FolioExecutionContext folioExecutionContext(String tenant) {
    return contextBuilder.forSystemUser(systemUserService.getSystemUser(tenant));
  }

}
