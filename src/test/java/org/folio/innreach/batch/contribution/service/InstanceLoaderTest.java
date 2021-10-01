package org.folio.innreach.batch.contribution.service;

import static java.util.UUID.randomUUID;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import static org.folio.innreach.fixture.ContributionFixture.createInstance;

import java.util.UUID;
import java.util.concurrent.Callable;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.folio.innreach.batch.contribution.ContributionJobContext;
import org.folio.innreach.domain.dto.folio.inventorystorage.InstanceIterationEvent;
import org.folio.innreach.domain.service.InventoryViewService;
import org.folio.innreach.domain.service.impl.TenantScopedExecutionService;

@ExtendWith(MockitoExtension.class)
class InstanceLoaderTest {

  private static final UUID JOB_ID = randomUUID();

  @Mock
  private TenantScopedExecutionService tenantScopedExecutionService;
  @Mock
  private InventoryViewService inventoryService;
  @Mock
  private ContributionJobContext context;

  @InjectMocks
  private InstanceLoader instanceLoader;

  @Test
  void shouldProcess() throws Exception {
    var event =
      InstanceIterationEvent.of(JOB_ID, "test", "test", randomUUID());
    var instance = createInstance();

    when(tenantScopedExecutionService.executeTenantScoped(any(), any()))
      .thenAnswer(invocationOnMock -> {
        var job = (Callable<?>) invocationOnMock.getArgument(1);
        return job.call();
      });
    when(context.getIterationJobId()).thenReturn(JOB_ID);
    when(inventoryService.getInstance(any(UUID.class))).thenReturn(instance);

    var result = instanceLoader.process(event);

    assertNotNull(result);
    assertEquals(instance, result);
  }

  @Test
  void shouldSkipUnknownEvent() throws Exception {
    var event =
      InstanceIterationEvent.of(randomUUID(), "test", "test", randomUUID());

    when(tenantScopedExecutionService.executeTenantScoped(any(), any()))
      .thenAnswer(invocationOnMock -> {
        var job = (Callable<?>) invocationOnMock.getArgument(1);
        return job.call();
      });
    when(context.getIterationJobId()).thenReturn(JOB_ID);

    var result = instanceLoader.process(event);

    assertNull(result);
    verifyNoMoreInteractions(inventoryService);
  }

  @Test
  void shouldSkipNotFoundInstance() throws Exception {
    var event =
      InstanceIterationEvent.of(JOB_ID, "test", "test", randomUUID());

    when(tenantScopedExecutionService.executeTenantScoped(any(), any()))
      .thenAnswer(invocationOnMock -> {
        var job = (Callable<?>) invocationOnMock.getArgument(1);
        return job.call();
      });
    when(context.getIterationJobId()).thenReturn(JOB_ID);
    when(inventoryService.getInstance(any(UUID.class))).thenReturn(null);

    var result = instanceLoader.process(event);

    assertNull(result);
  }

  @Test
  void shouldSkipUnsupportedSource() throws Exception {
    var event =
      InstanceIterationEvent.of(JOB_ID, "test", "test", randomUUID());
    var instance = createInstance();
    instance.setSource("FOLIO");

    when(tenantScopedExecutionService.executeTenantScoped(any(), any()))
      .thenAnswer(invocationOnMock -> {
        var job = (Callable<?>) invocationOnMock.getArgument(1);
        return job.call();
      });
    when(context.getIterationJobId()).thenReturn(JOB_ID);
    when(inventoryService.getInstance(any(UUID.class))).thenReturn(instance);

    var result = instanceLoader.process(event);

    assertNull(result);
  }

}
