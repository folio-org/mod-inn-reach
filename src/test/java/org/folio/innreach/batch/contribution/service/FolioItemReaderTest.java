package org.folio.innreach.batch.contribution.service;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import static org.folio.innreach.batch.contribution.service.FolioItemReader.INSTANCE_ITEM_OFFSET_CONTEXT;
import static org.folio.innreach.batch.contribution.service.FolioItemReader.INSTANCE_ITEM_TOTAL_CONTEXT;
import static org.folio.innreach.fixture.ContributionFixture.createBatchStepExecution;
import static org.folio.innreach.fixture.ContributionFixture.createExecutionContext;
import static org.folio.innreach.fixture.ContributionFixture.createInstance;
import static org.folio.innreach.fixture.FolioContextFixture.createTenantExecutionService;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.item.ExecutionContext;

import org.folio.innreach.batch.contribution.ContributionJobContext;
import org.folio.innreach.domain.service.InventoryViewService;
import org.folio.innreach.domain.service.impl.TenantScopedExecutionService;

@ExtendWith(MockitoExtension.class)
class FolioItemReaderTest {

  @Spy
  private TenantScopedExecutionService tenantScopedExecutionService = createTenantExecutionService();

  @Mock
  private ContributionJobContext jobContext;

  @Mock
  private InventoryViewService inventoryService;

  @InjectMocks
  private FolioItemReader reader;

  @Test
  void shouldOpenEmptyContext() {
    reader.open(new ExecutionContext());

    assertTrue(reader.getInstanceItemOffsets().isEmpty());
    assertTrue(reader.getInstanceItemTotals().isEmpty());
    assertTrue(reader.getContributedInstanceIds().isEmpty());
  }

/*  @Test
  void shouldOpenExistingContext() {
    var context = createExecutionContext();
    reader.open(context);

    var contextInstanceIds = (List<UUID>) context.get(INSTANCE_CONTRIBUTED_ID_CONTEXT);
    var contextItemOffsets = (Map<UUID, Integer>) context.get(INSTANCE_ITEM_OFFSET_CONTEXT);
    var contextItemTotals = (Map<UUID, Integer>) context.get(INSTANCE_ITEM_TOTAL_CONTEXT);

    Assertions.assertThat(reader.getContributedInstanceIds())
      .containsExactlyInAnyOrderElementsOf(contextInstanceIds);

    Assertions.assertThat(reader.getInstanceItemOffsets())
      .containsExactlyInAnyOrderEntriesOf(contextItemOffsets);

    Assertions.assertThat(reader.getInstanceItemTotals())
      .containsExactlyInAnyOrderEntriesOf(contextItemTotals);
  }*/

  @Test
  void shouldReturnNullOnRead() {
    reader.loadInstanceStepContext(createBatchStepExecution());
    reader.open(createExecutionContext());

    var event = reader.read();

    assertNull(event);
  }

  @Test
  void shouldReturnItem() {
    reader.loadInstanceStepContext(createBatchStepExecution());
    reader.open(createExecutionContext());

    when(inventoryService.getInstance(any(UUID.class))).thenReturn(createInstance());

    var event = reader.read();

    assertNotNull(event);
  }

  @Test
  void shouldUpdate() {
    var contextMock = Mockito.mock(ExecutionContext.class);
    reader.update(contextMock);

    verify(contextMock).put(eq(INSTANCE_ITEM_OFFSET_CONTEXT), any());
    verify(contextMock).put(eq(INSTANCE_ITEM_TOTAL_CONTEXT), any());
  }

}
