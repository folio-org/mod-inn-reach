package org.folio.innreach.batch.contribution.service;

import static java.util.UUID.randomUUID;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import static org.folio.innreach.fixture.ContributionFixture.createContributionJobContext;
import static org.folio.innreach.fixture.ContributionFixture.createInstance;

import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.folio.innreach.batch.contribution.ContributionJobContext;
import org.folio.innreach.batch.contribution.ContributionJobContextManager;
import org.folio.innreach.domain.dto.folio.inventorystorage.InstanceIterationEvent;
import org.folio.innreach.domain.service.InventoryViewService;

@ExtendWith(MockitoExtension.class)
class InstanceLoaderTest {

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
  void shouldProcess() {
    var event =
      InstanceIterationEvent.of(JOB_CONTEXT.getIterationJobId(), "test", "test", randomUUID());
    var instance = createInstance();

    when(inventoryService.getInstance(any(UUID.class))).thenReturn(instance);

    var result = instanceLoader.load(event);

    assertNotNull(result);
    assertEquals(instance, result);
  }

  @Test
  void shouldSkipNotFoundInstance() {
    var event =
      InstanceIterationEvent.of(JOB_CONTEXT.getIterationJobId(), "test", "test", randomUUID());

    when(inventoryService.getInstance(any(UUID.class))).thenReturn(null);

    var result = instanceLoader.load(event);

    assertNull(result);
    verify(inventoryService).getInstance(event.getInstanceId());
  }

  @Test
  void shouldSkipUnsupportedSource() {
    var event =
      InstanceIterationEvent.of(JOB_CONTEXT.getIterationJobId(), "test", "test", randomUUID());
    var instance = createInstance();
    instance.setSource("FOLIO");

    when(inventoryService.getInstance(any(UUID.class))).thenReturn(instance);

    var result = instanceLoader.load(event);

    assertNull(result);
  }

  @Test
  void shouldSkipUnknownEvent() {
    var event =
      InstanceIterationEvent.of(randomUUID(), "test", "test", randomUUID());

    var result = instanceLoader.load(event);

    assertNull(result);
    verifyNoInteractions(inventoryService);
  }

}
