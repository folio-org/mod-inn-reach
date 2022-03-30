package org.folio.innreach.domain.listener;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

import static org.folio.innreach.fixture.ContributionFixture.createHolding;
import static org.folio.innreach.fixture.ContributionFixture.createInstance;
import static org.folio.innreach.fixture.ContributionFixture.createItem;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;

import org.folio.innreach.domain.event.DomainEvent;
import org.folio.innreach.domain.event.DomainEventType;
import org.folio.innreach.domain.event.EntityChangedData;
import org.folio.innreach.domain.listener.base.BaseKafkaApiTest;
import org.folio.innreach.domain.service.ContributionActionService;
import org.folio.innreach.dto.Holding;
import org.folio.innreach.dto.Instance;
import org.folio.innreach.dto.Item;

class KafkaInventoryEventListenerApiTest extends BaseKafkaApiTest {
  private static final UUID RECORD_ID = UUID.randomUUID();
  private static final String TEST_TENANT_ID = "testing";
  private static final Duration ASYNC_AWAIT_TIMEOUT = Duration.ofSeconds(15);

  @SpyBean
  private KafkaInventoryEventListener listener;

  @MockBean
  private ContributionActionService actionService;

  @Test
  void shouldReceiveInventoryItemEvent() {
    var event = createItemDomainEvent(DomainEventType.DELETED);

    kafkaTemplate.send(new ProducerRecord(INVENTORY_ITEM_TOPIC, RECORD_ID.toString(), event));

    ArgumentCaptor<List<ConsumerRecord<String, DomainEvent<Item>>>> eventsCaptor = ArgumentCaptor.forClass(List.class);

    await().atMost(ASYNC_AWAIT_TIMEOUT).untilAsserted(() ->
      verify(actionService).handleItemDelete(any()));

    verify(listener).handleItemEvents(eventsCaptor.capture());

    var records = eventsCaptor.getValue();
    assertEquals(1, records.size());

    var record = records.get(0);
    assertEquals(RECORD_ID.toString(), record.key());
    assertEquals(event, record.value());
  }

  @Test
  void shouldReceiveInventoryHoldingEvent() {
    var event = createHoldingDomainEvent(DomainEventType.DELETED);

    kafkaTemplate.send(new ProducerRecord(INVENTORY_HOLDING_TOPIC, RECORD_ID.toString(), event));

    ArgumentCaptor<List<ConsumerRecord<String, DomainEvent<Holding>>>> eventsCaptor = ArgumentCaptor.forClass(List.class);

    await().atMost(ASYNC_AWAIT_TIMEOUT).untilAsserted(() ->
      verify(actionService).handleHoldingDelete(any()));

    verify(listener).handleHoldingEvents(eventsCaptor.capture());

    var records = eventsCaptor.getValue();
    assertEquals(1, records.size());

    var record = records.get(0);
    assertEquals(RECORD_ID.toString(), record.key());
    assertEquals(event, record.value());
  }

  @Test
  void shouldReceiveInventoryInstanceEvent() {
    var event = createInstanceDomainEvent(DomainEventType.CREATED);

    kafkaTemplate.send(new ProducerRecord(INVENTORY_INSTANCE_TOPIC, RECORD_ID.toString(), event));

    ArgumentCaptor<List<ConsumerRecord<String, DomainEvent<Instance>>>> eventsCaptor = ArgumentCaptor.forClass(List.class);

    await().atMost(ASYNC_AWAIT_TIMEOUT).untilAsserted(() ->
      verify(actionService).handleInstanceCreation(any()));

    verify(listener).handleInstanceEvents(eventsCaptor.capture());

    var records = eventsCaptor.getValue();
    assertEquals(1, records.size());

    var record = records.get(0);
    assertEquals(RECORD_ID.toString(), record.key());
    assertEquals(event, record.value());
  }

  private DomainEvent<Item> createItemDomainEvent(DomainEventType eventType) {
    var item = createItem();

    return DomainEvent.<Item>builder()
      .tenant(TEST_TENANT_ID)
      .timestamp(System.currentTimeMillis())
      .type(eventType)
      .data(new EntityChangedData<>(item, item))
      .build();
  }

  private DomainEvent<Instance> createInstanceDomainEvent(DomainEventType eventType) {
    var instance = createInstance();

    return DomainEvent.<Instance>builder()
      .tenant(TEST_TENANT_ID)
      .timestamp(System.currentTimeMillis())
      .type(eventType)
      .data(new EntityChangedData<>(instance, instance))
      .build();
  }

  private DomainEvent<Holding> createHoldingDomainEvent(DomainEventType eventType) {
    var holding = createHolding();

    return DomainEvent.<Holding>builder()
      .tenant(TEST_TENANT_ID)
      .timestamp(System.currentTimeMillis())
      .type(eventType)
      .data(new EntityChangedData<>(holding, holding))
      .build();
  }
}
