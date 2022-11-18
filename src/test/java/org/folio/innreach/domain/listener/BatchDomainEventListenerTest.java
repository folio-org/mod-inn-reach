package org.folio.innreach.domain.listener;

import org.folio.innreach.domain.event.DomainEventType;
import org.folio.innreach.domain.listener.base.BaseKafkaApiTest;
import org.folio.innreach.domain.service.impl.BatchDomainEventProcessor;
import org.folio.innreach.domain.service.impl.TenantScopedExecutionService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

class BatchDomainEventListenerTest extends BaseKafkaApiTest {
  private static final String TEST_TENANT_ID = "testing";

  private static final UUID PRE_POPULATED_LOCAL_ITEM_ID = UUID.fromString("c633da85-8112-4453-af9c-c250e417179d");

  @SpyBean
  private KafkaInventoryEventListener listener;

  @MockBean
  private TenantScopedExecutionService executionService;

  @SpyBean
  private BatchDomainEventProcessor eventProcessor;

  @Test
  void shouldNotProcessEventIfModuleDisabled() {
    KafkaInventoryEventListenerApiTest kafkaInventory = new KafkaInventoryEventListenerApiTest();
    var event = kafkaInventory.getItemDomainEvent(DomainEventType.UPDATED, PRE_POPULATED_LOCAL_ITEM_ID);
    var updatedItem = event.getData().getNewEntity();

    doThrow(new RuntimeException()).when(executionService).runTenantScoped(eq(TEST_TENANT_ID), any(Runnable.class));

    listener.handleItemEvents(asSingleConsumerRecord(INVENTORY_ITEM_TOPIC, PRE_POPULATED_LOCAL_ITEM_ID, event));

    verify(executionService).runTenantScoped(eq(TEST_TENANT_ID), any(Runnable.class));
  }


}
