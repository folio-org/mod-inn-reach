package org.folio.innreach.support;

import lombok.SneakyThrows;
import org.folio.innreach.domain.service.impl.TenantScopedExecutionService;
import org.folio.spring.context.ExecutionContextBuilder;
import org.folio.spring.scope.FolioExecutionContextSetter;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

/**
 * Test replacement for {@link TenantScopedExecutionService} that runs jobs synchronously.
 * Properly sets the tenant context before running the job, except for tenant "testing4"
 * which throws {@link RuntimeException} to simulate error scenarios.
 */
@Primary
@Service
@Profile("test")
public class TestTenantScopedExecutionService extends TenantScopedExecutionService {

  private final ExecutionContextBuilder contextBuilder;

  public TestTenantScopedExecutionService(ExecutionContextBuilder contextBuilder) {
    super(null);
    this.contextBuilder = contextBuilder;
  }

  @Override
  @SneakyThrows
  public void runTenantScoped(String tenantId, Runnable job) {
    if (tenantId.equals("testing4")) {
      throw new RuntimeException("testing exception");
    }
    try (var fex = new FolioExecutionContextSetter(contextBuilder.buildContext(tenantId))) {
      job.run();
    }
  }

  @Override
  public void executeAsyncTenantScoped(String tenantId, Runnable job) {
    try (var fex = new FolioExecutionContextSetter(contextBuilder.buildContext(tenantId))) {
      job.run();
    }
  }
}
