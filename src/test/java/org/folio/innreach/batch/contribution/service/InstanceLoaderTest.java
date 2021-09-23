package org.folio.innreach.batch.contribution.service;

import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.UUID;
import java.util.concurrent.Callable;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.folio.innreach.batch.contribution.ContributionJobContext;
import org.folio.innreach.domain.dto.folio.inventorystorage.InstanceIterationEvent;
import org.folio.innreach.domain.service.InventoryService;
import org.folio.innreach.domain.service.impl.TenantScopedExecutionService;

@ExtendWith(MockitoExtension.class)
class InstanceLoaderTest {

  private static final UUID JOB_ID = randomUUID();

  @Mock
  private TenantScopedExecutionService tenantScopedExecutionService;
  @Mock
  private InventoryService inventoryService;
  @Mock
  private ContributionJobContext context;

  @InjectMocks
  private InstanceLoader instanceLoader;

  @Test
  void shouldProcess() throws Exception {
    var event =
      InstanceIterationEvent.of(JOB_ID, "test", "test", randomUUID());

    when(tenantScopedExecutionService.executeTenantScoped(any(), any()))
      .thenAnswer(invocationOnMock -> {
        var job = (Callable<?>) invocationOnMock.getArgument(1);
        return job.call();
      });
    when(context.getIterationJobId()).thenReturn(JOB_ID);

    instanceLoader.process(event);

    verify(inventoryService).getInstance(any(UUID.class));
  }

  @Test
  void shouldSkip() throws Exception {
    var event =
      InstanceIterationEvent.of(randomUUID(), "test", "test", randomUUID());

    when(tenantScopedExecutionService.executeTenantScoped(any(), any()))
      .thenAnswer(invocationOnMock -> {
        var job = (Callable<?>) invocationOnMock.getArgument(1);
        return job.call();
      });
    when(context.getIterationJobId()).thenReturn(JOB_ID);

    var instance = instanceLoader.process(event);

    assertNull(instance);
    verifyNoMoreInteractions(inventoryService);
  }
}
