package org.folio.innreach.domain.listener;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.AFTER_TEST_METHOD;
import static org.springframework.test.context.jdbc.SqlMergeMode.MergeMode.MERGE;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlMergeMode;

import org.folio.innreach.domain.event.DomainEvent;
import org.folio.innreach.domain.event.DomainEventType;
import org.folio.innreach.domain.event.EntityChangedData;
import org.folio.innreach.domain.listener.base.BaseKafkaApiTest;
import org.folio.innreach.domain.service.RecordContributionService;
import org.folio.innreach.domain.service.impl.BatchDomainEventProcessor;
import org.folio.innreach.dto.Holding;
import org.folio.innreach.dto.Instance;
import org.folio.innreach.dto.Item;
import org.folio.innreach.external.service.InnReachExternalService;

@Sql(
  scripts = {"classpath:db/contribution-criteria/clear-contribution-criteria-tables.sql",
    "classpath:db/central-server/clear-central-server-tables.sql"},
  executionPhase = AFTER_TEST_METHOD
)
@SqlMergeMode(MERGE)
class KafkaInventoryEventListenerApiTest extends BaseKafkaApiTest {
  private static final UUID RECORD_ID = UUID.randomUUID();
  private static final String TEST_TENANT_ID = "testing";
  private static final Duration ASYNC_AWAIT_TIMEOUT = Duration.ofSeconds(15);
  private static final UUID doNotContributeCode = UUID.fromString("5599f23f-d424-4fce-8a51-b7fce690cbda");

  @SpyBean
  private KafkaInventoryEventListener listener;
  @MockBean
  private InnReachExternalService innReachExternalService;
  @SpyBean
  private BatchDomainEventProcessor eventProcessor;
  @SpyBean
  private RecordContributionService service;

  @Test
  void shouldReceiveInventoryItemEvent() {
    doNothing().when(listener).handleItemEvents(any());
    var event = getItemDomainEvent(DomainEventType.DELETED);

    kafkaTemplate.send(new ProducerRecord(INVENTORY_ITEM_TOPIC, RECORD_ID.toString(), event));

    ArgumentCaptor<List<ConsumerRecord<String, DomainEvent<Item>>>> eventsCaptor = ArgumentCaptor.forClass(List.class);

    await().atMost(ASYNC_AWAIT_TIMEOUT).untilAsserted(() ->
      verify(listener).handleItemEvents(eventsCaptor.capture()));

    var records = eventsCaptor.getValue();
    assertEquals(1, records.size());

    var record = records.get(0);
    assertEquals(RECORD_ID.toString(), record.key());
    assertEquals(event, record.value());
  }

  @Test
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql"
  })
  void shouldDecontributeAnItem() {
    when(innReachExternalService.deleteInnReachApi(any(), any())).thenReturn("test");
    var event = getItemDomainEvent(DomainEventType.DELETED);
    event.setRecordId(null);

    var consumerRecord = new ConsumerRecord(INVENTORY_ITEM_TOPIC, 1, 1, RECORD_ID.toString(), event);

    listener.handleItemEvents(List.of(consumerRecord));

    ArgumentCaptor<List<DomainEvent<Item>>> eventsCaptor = ArgumentCaptor.forClass(List.class);
    verify(eventProcessor).process(eventsCaptor.capture(), any(Consumer.class));

    var capturedEvents = eventsCaptor.getValue();
    assertEquals(1, capturedEvents.size());
    var capturedEvent = capturedEvents.get(0);
    assertEquals(RECORD_ID, capturedEvent.getRecordId());

    verify(service).decontributeInventoryItemEvents(eq(event.getData().getOldEntity()), any());
    verify(innReachExternalService).deleteInnReachApi(any(), contains(RECORD_ID.toString()));
  }

  @Test
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/contribution-criteria/pre-populate-contribution-criteria.sql"
  })
  void shouldDecontributeInvalidItems() {
    when(innReachExternalService.deleteInnReachApi(any(), any())).thenReturn("test");
    var event = getItemDomainEvent(DomainEventType.UPDATED);
    event.setRecordId(null);
    event.getData().getNewEntity().setStatisticalCodeIds(List.of(doNotContributeCode));

    var consumerRecord = new ConsumerRecord(INVENTORY_ITEM_TOPIC, 1, 1, RECORD_ID.toString(), event);

    listener.handleItemEvents(List.of(consumerRecord));

    ArgumentCaptor<List<DomainEvent<Item>>> eventsCaptor = ArgumentCaptor.forClass(List.class);
    verify(eventProcessor).process(eventsCaptor.capture(), any(Consumer.class));

    var capturedEvents = eventsCaptor.getValue();
    assertEquals(1, capturedEvents.size());
    var capturedEvent = capturedEvents.get(0);
    assertEquals(RECORD_ID, capturedEvent.getRecordId());

    verify(service).decontributeInventoryItemEvents(eq(event.getData().getOldEntity()), any());
    verify(innReachExternalService).deleteInnReachApi(any(), contains(RECORD_ID.toString()));
  }

  @Test
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/contribution-criteria/pre-populate-contribution-criteria.sql"
  })
  void shouldNotDecontributeValidItems() {
    when(innReachExternalService.deleteInnReachApi(any(), any())).thenReturn("test");
    var event = getItemDomainEvent(DomainEventType.UPDATED);
    event.setRecordId(null);

    var consumerRecord = new ConsumerRecord(INVENTORY_ITEM_TOPIC, 1, 1, RECORD_ID.toString(), event);

    listener.handleItemEvents(List.of(consumerRecord));

    ArgumentCaptor<List<DomainEvent<Item>>> eventsCaptor = ArgumentCaptor.forClass(List.class);
    verify(eventProcessor).process(eventsCaptor.capture(), any(Consumer.class));

    var capturedEvents = eventsCaptor.getValue();
    assertEquals(1, capturedEvents.size());
    var capturedEvent = capturedEvents.get(0);
    assertEquals(RECORD_ID, capturedEvent.getRecordId());

    verify(service).evaluateInventoryItemForContribution(eq(event.getData().getNewEntity()), any());
    verify(innReachExternalService, never()).deleteInnReachApi(any(), contains(RECORD_ID.toString()));
  }

  @Test
  void shouldReceiveInventoryInstanceEvent() {
    doNothing().when(listener).handleInstanceEvents(any());
    var event = getInstanceDomainEvent(DomainEventType.DELETED);

    kafkaTemplate.send(new ProducerRecord(INVENTORY_INSTANCE_TOPIC, RECORD_ID.toString(), event));

    ArgumentCaptor<List<ConsumerRecord<String, DomainEvent<Instance>>>> eventsCaptor = ArgumentCaptor.forClass(List.class);

    await().atMost(ASYNC_AWAIT_TIMEOUT).untilAsserted(() ->
      verify(listener).handleInstanceEvents(eventsCaptor.capture()));

    var records = eventsCaptor.getValue();
    assertEquals(1, records.size());

    var record = records.get(0);
    assertEquals(RECORD_ID.toString(), record.key());
    assertEquals(event, record.value());
  }

  @Test
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql"
  })
  void shouldDecontributeAnInstance() {
    when(innReachExternalService.deleteInnReachApi(any(), any())).thenReturn("test");
    var event = getInstanceDomainEvent(DomainEventType.DELETED);
    event.setRecordId(null);

    var consumerRecord = new ConsumerRecord(INVENTORY_INSTANCE_TOPIC, 1, 1, RECORD_ID.toString(), event);

    listener.handleInstanceEvents(List.of(consumerRecord));

    ArgumentCaptor<List<DomainEvent<Instance>>> eventsCaptor = ArgumentCaptor.forClass(List.class);
    verify(eventProcessor).process(eventsCaptor.capture(), any(Consumer.class));

    var capturedEvents = eventsCaptor.getValue();
    assertEquals(1, capturedEvents.size());
    var capturedEvent = capturedEvents.get(0);
    assertEquals(RECORD_ID, capturedEvent.getRecordId());

    verify(service).decontributeInventoryInstanceEvents(any(), any());
    verify(innReachExternalService).deleteInnReachApi(any(), contains(RECORD_ID.toString()));
  }

  @Test
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql"
  })
  void shouldDecontributeInvalidInstances() {
    when(innReachExternalService.deleteInnReachApi(any(), any())).thenReturn("test");
    var event = getInstanceDomainEvent(DomainEventType.UPDATED);
    event.setRecordId(null);
    event.getData().getNewEntity().setStatisticalCodeIds(List.of(UUID.randomUUID(), UUID.randomUUID()));

    var consumerRecord = new ConsumerRecord(INVENTORY_INSTANCE_TOPIC, 1, 1, RECORD_ID.toString(), event);

    listener.handleInstanceEvents(List.of(consumerRecord));

    ArgumentCaptor<List<DomainEvent<Instance>>> eventsCaptor = ArgumentCaptor.forClass(List.class);
    verify(eventProcessor).process(eventsCaptor.capture(), any(Consumer.class));

    var capturedEvents = eventsCaptor.getValue();
    assertEquals(1, capturedEvents.size());
    var capturedEvent = capturedEvents.get(0);
    assertEquals(RECORD_ID, capturedEvent.getRecordId());

    verify(service).decontributeInventoryInstanceEvents(eq(event.getData().getOldEntity()), any());
    verify(innReachExternalService).deleteInnReachApi(any(), contains(RECORD_ID.toString()));
  }

  @Test
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/contribution-criteria/pre-populate-contribution-criteria.sql"
  })
  void shouldNotDecontributeValidInstances() {
    when(innReachExternalService.deleteInnReachApi(any(), any())).thenReturn("test");
    var event = getInstanceDomainEvent(DomainEventType.UPDATED);
    event.setRecordId(null);

    var consumerRecord = new ConsumerRecord(INVENTORY_INSTANCE_TOPIC, 1, 1, RECORD_ID.toString(), event);

    listener.handleInstanceEvents(List.of(consumerRecord));

    ArgumentCaptor<List<DomainEvent<Instance>>> eventsCaptor = ArgumentCaptor.forClass(List.class);
    verify(eventProcessor).process(eventsCaptor.capture(), any(Consumer.class));

    var capturedEvents = eventsCaptor.getValue();
    assertEquals(1, capturedEvents.size());
    var capturedEvent = capturedEvents.get(0);
    assertEquals(RECORD_ID, capturedEvent.getRecordId());

    verify(service).evaluateInventoryInstanceForContribution(eq(event.getData().getNewEntity()), any());
    verify(innReachExternalService, never()).deleteInnReachApi(any(), contains(RECORD_ID.toString()));
  }

  @Test
  void shouldReceiveInventoryHoldingEvent() {
    doNothing().when(listener).handleHoldingEvents(any());
    var event = getHoldingDomainEvent(DomainEventType.DELETED);

    kafkaTemplate.send(new ProducerRecord(INVENTORY_HOLDING_TOPIC, RECORD_ID.toString(), event));

    ArgumentCaptor<List<ConsumerRecord<String, DomainEvent<Holding>>>> eventsCaptor = ArgumentCaptor.forClass(List.class);

    await().atMost(ASYNC_AWAIT_TIMEOUT).untilAsserted(() ->
      verify(listener).handleHoldingEvents(eventsCaptor.capture()));

    var records = eventsCaptor.getValue();
    assertEquals(1, records.size());

    var record = records.get(0);
    assertEquals(RECORD_ID.toString(), record.key());
    assertEquals(event, record.value());
  }

  @Test
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/contribution-criteria/pre-populate-contribution-criteria.sql"
  })
  void shouldDecontributeAHolding() {
    when(innReachExternalService.deleteInnReachApi(any(), any())).thenReturn("test");
    var event = getHoldingDomainEvent(DomainEventType.DELETED);
    event.setRecordId(null);

    var consumerRecord = new ConsumerRecord(INVENTORY_HOLDING_TOPIC, 1, 1, RECORD_ID.toString(), event);

    listener.handleHoldingEvents(List.of(consumerRecord));

    ArgumentCaptor<List<DomainEvent<Holding>>> eventsCaptor = ArgumentCaptor.forClass(List.class);
    verify(eventProcessor).process(eventsCaptor.capture(), any(Consumer.class));

    var capturedEvents = eventsCaptor.getValue();
    assertEquals(1, capturedEvents.size());
    var capturedEvent = capturedEvents.get(0);
    assertEquals(RECORD_ID, capturedEvent.getRecordId());

    verify(service).decontributeInventoryHoldingEvents(any(), any());
    verify(innReachExternalService).deleteInnReachApi(any(), contains(RECORD_ID.toString()));
  }

  @Test
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/contribution-criteria/pre-populate-contribution-criteria.sql"
  })
  void shouldDecontributeInvalidHolding() {
    when(innReachExternalService.deleteInnReachApi(any(), any())).thenReturn("test");
    var event = getHoldingDomainEvent(DomainEventType.UPDATED);
    event.setRecordId(null);
    event.getData().getNewEntity().setStatisticalCodeIds(List.of(UUID.randomUUID(), UUID.randomUUID()));

    var consumerRecord = new ConsumerRecord(INVENTORY_HOLDING_TOPIC, 1, 1, RECORD_ID.toString(), event);

    listener.handleHoldingEvents(List.of(consumerRecord));

    ArgumentCaptor<List<DomainEvent<Holding>>> eventsCaptor = ArgumentCaptor.forClass(List.class);
    verify(eventProcessor).process(eventsCaptor.capture(), any(Consumer.class));

    var capturedEvents = eventsCaptor.getValue();
    assertEquals(1, capturedEvents.size());
    var capturedEvent = capturedEvents.get(0);
    assertEquals(RECORD_ID, capturedEvent.getRecordId());

    verify(service).decontributeInventoryHoldingEvents(eq(event.getData().getOldEntity()), any());
    verify(innReachExternalService).deleteInnReachApi(any(), contains(RECORD_ID.toString()));
  }

  @Test
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/contribution-criteria/pre-populate-contribution-criteria.sql"
  })
  void shouldNotDecontributeValidHoldings() {
    when(innReachExternalService.deleteInnReachApi(any(), any())).thenReturn("test");
    var event = getHoldingDomainEvent(DomainEventType.UPDATED);
    event.setRecordId(null);

    var consumerRecord = new ConsumerRecord(INVENTORY_HOLDING_TOPIC, 1, 1, RECORD_ID.toString(), event);

    listener.handleHoldingEvents(List.of(consumerRecord));

    ArgumentCaptor<List<DomainEvent<Holding>>> eventsCaptor = ArgumentCaptor.forClass(List.class);
    verify(eventProcessor).process(eventsCaptor.capture(), any(Consumer.class));

    var capturedEvents = eventsCaptor.getValue();
    assertEquals(1, capturedEvents.size());
    var capturedEvent = capturedEvents.get(0);
    assertEquals(RECORD_ID, capturedEvent.getRecordId());

    verify(service).evaluateInventoryHoldingForContribution(eq(event.getData().getNewEntity()), any());
    verify(innReachExternalService, never()).deleteInnReachApi(any(), contains(RECORD_ID.toString()));
  }

  private DomainEvent<Item> getItemDomainEvent(DomainEventType eventType) {
    var item = new Item()
      .id(RECORD_ID)
      .statisticalCodeIds(List.of(UUID.randomUUID()))
      .holdingStatisticalCodeIds(List.of(UUID.randomUUID()));

    return DomainEvent.<Item>builder()
      .recordId(RECORD_ID)
      .tenant(TEST_TENANT_ID)
      .timestamp(System.currentTimeMillis())
      .type(eventType)
      .data(new EntityChangedData<>(item, item))
      .build();
  }

  private DomainEvent<Instance> getInstanceDomainEvent(DomainEventType eventType) {
    var item = new Instance()
      .id(RECORD_ID)
      .statisticalCodeIds(List.of(UUID.randomUUID()));

    return DomainEvent.<Instance>builder()
      .recordId(RECORD_ID)
      .tenant(TEST_TENANT_ID)
      .timestamp(System.currentTimeMillis())
      .type(eventType)
      .data(new EntityChangedData<>(item, item))
      .build();
  }

  private DomainEvent<Holding> getHoldingDomainEvent(DomainEventType eventType) {
    var holding = new Holding()
      .id(RECORD_ID)
      .statisticalCodeIds(List.of(UUID.randomUUID()))
      .holdingsItems(Collections.emptyList());

    return DomainEvent.<Holding>builder()
      .recordId(RECORD_ID)
      .tenant(TEST_TENANT_ID)
      .timestamp(System.currentTimeMillis())
      .type(eventType)
      .data(new EntityChangedData<>(holding, holding))
      .build();
  }
}
