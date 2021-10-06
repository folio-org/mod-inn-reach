package org.folio.innreach.batch.contribution.service;

import static java.util.UUID.randomUUID;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import static org.folio.innreach.fixture.ContributionFixture.createInstance;
import static org.folio.innreach.fixture.FolioContextFixture.createTenantExecutionService;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import org.folio.innreach.batch.contribution.ContributionJobContext;
import org.folio.innreach.domain.dto.folio.inventorystorage.InstanceIterationEvent;
import org.folio.innreach.domain.service.InventoryViewService;
import org.folio.innreach.domain.service.impl.TenantScopedExecutionService;

@ExtendWith(MockitoExtension.class)
class InstanceLoaderTest {

  private static final UUID JOB_ID = randomUUID();

  @Spy
  private TenantScopedExecutionService tenantScopedExecutionService = createTenantExecutionService();
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

    when(inventoryService.getInstance(any(UUID.class))).thenReturn(instance);

    var result = instanceLoader.process(event);

    assertNotNull(result);
    assertEquals(instance, result);
  }

  void shouldSkipUnknownEvent() throws Exception {
    var event =
      InstanceIterationEvent.of(randomUUID(), "test", "test", randomUUID());

    var result = instanceLoader.process(event);

    assertNull(result);
    verifyNoMoreInteractions(inventoryService);
  }

  @Test
  void shouldSkipNotFoundInstance() throws Exception {
    var event =
      InstanceIterationEvent.of(JOB_ID, "test", "test", randomUUID());

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

    when(inventoryService.getInstance(any(UUID.class))).thenReturn(instance);

    var result = instanceLoader.process(event);

    assertNull(result);
  }

}
