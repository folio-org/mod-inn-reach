package org.folio.innreach.batch.contribution.service;

import static java.util.UUID.randomUUID;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import static org.folio.innreach.fixture.ContributionFixture.createContributionJobContext;
import static org.folio.innreach.fixture.ContributionFixture.createInstance;
import static org.folio.innreach.fixture.FolioContextFixture.createTenantExecutionService;

import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import org.folio.innreach.batch.contribution.ContributionJobContext;
import org.folio.innreach.batch.contribution.ContributionJobContextManager;
import org.folio.innreach.domain.dto.folio.inventorystorage.InstanceIterationEvent;
import org.folio.innreach.domain.service.InventoryViewService;
import org.folio.innreach.domain.service.impl.TenantScopedExecutionService;

@ExtendWith(MockitoExtension.class)
class InstanceLoaderTest {

  private static final UUID JOB_ID = randomUUID();
  private static final ContributionJobContext JOB_CONTEXT = createContributionJobContext();

  @Mock
  private InventoryViewService inventoryService;

  @InjectMocks
  private InstanceLoader instanceLoader;

  @BeforeEach
  public void init() {
    ContributionJobContextManager.beginContributionJobContext(JOB_CONTEXT);
  }

  @AfterEach
  public void clear() {
    ContributionJobContextManager.endContributionJobContext();
  }

  @Test
  void shouldProcess() throws Exception {
    var event =
      InstanceIterationEvent.of(JOB_CONTEXT.getIterationJobId(), "test", "test", randomUUID());
    var instance = createInstance();

    when(inventoryService.getInstance(any(UUID.class))).thenReturn(instance);

    var result = instanceLoader.load(event);

    assertNotNull(result);
    assertEquals(instance, result);
  }

  @Test
  void shouldSkipNotFoundInstance() throws Exception {
    var event =
      InstanceIterationEvent.of(JOB_CONTEXT.getIterationJobId(), "test", "test", randomUUID());

    when(inventoryService.getInstance(any(UUID.class))).thenReturn(null);

    var result = instanceLoader.load(event);

    assertNull(result);
  }

  @Test
  void shouldSkipUnsupportedSource() throws Exception {
    var event =
      InstanceIterationEvent.of(JOB_CONTEXT.getIterationJobId(), "test", "test", randomUUID());
    var instance = createInstance();
    instance.setSource("FOLIO");

    var result = instanceLoader.load(event);

    assertNull(result);
  }

}
