package org.folio.innreach.support;

import lombok.SneakyThrows;
import org.folio.innreach.domain.service.impl.TenantScopedExecutionService;

/**
 * Test replacement for {@link TenantScopedExecutionService} that runs jobs synchronously
 * without tenant scoping. Throws {@link RuntimeException} for tenant "testing4" to simulate
 * error scenarios.
 *
 * <p>Note: {@code @Primary @Service @Profile("test")} annotations will be added when
 * the Kafka test migration is complete (Task 9) and the inner class in BaseKafkaApiTest
 * is removed, to avoid conflicting bean definitions during migration.
 */
public class TestTenantScopedExecutionService extends TenantScopedExecutionService {

  public TestTenantScopedExecutionService() {
    super(null);
  }

  @Override
  @SneakyThrows
  public void runTenantScoped(String tenantId, Runnable job) {
    if (tenantId.equals("testing4")) {
      throw new RuntimeException("testing exception");
    }
    job.run();
  }

  @Override
  public void executeAsyncTenantScoped(String tenantId, Runnable job) {
    job.run();
  }
}
