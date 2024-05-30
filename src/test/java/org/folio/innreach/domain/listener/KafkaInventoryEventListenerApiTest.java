package org.folio.innreach.domain.listener;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.AFTER_TEST_METHOD;
import static org.springframework.test.context.jdbc.SqlMergeMode.MergeMode.MERGE;

import static org.folio.innreach.fixture.ContributionFixture.createHolding;
import static org.folio.innreach.fixture.ContributionFixture.createInstance;
import static org.folio.innreach.fixture.ContributionFixture.createItem;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.folio.innreach.domain.service.impl.BatchDomainEventProcessor;
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
import org.folio.innreach.domain.service.ContributionActionService;
import org.folio.innreach.dto.Holding;
import org.folio.innreach.dto.Instance;
import org.folio.innreach.dto.Item;
import org.folio.innreach.repository.InnReachTransactionRepository;

@Sql(
  scripts = {"classpath:db/inn-reach-transaction/clear-inn-reach-transaction-tables.sql",
    "classpath:db/central-server/clear-central-server-tables.sql",
  },
  executionPhase = AFTER_TEST_METHOD
)
@SqlMergeMode(MERGE)
class KafkaInventoryEventListenerApiTest extends BaseKafkaApiTest {
  private static final UUID RECORD_ID = UUID.randomUUID();
  private static final String TEST_TENANT_ID = "testing";
  private static final Duration ASYNC_AWAIT_TIMEOUT = Duration.ofSeconds(15);

  private static final UUID PRE_POPULATED_LOCAL_TRANSACTION_ID = UUID.fromString("79b0a1fb-55be-4e55-9d84-01303aaec1ce");
  private static final UUID PRE_POPULATED_LOCAL_ITEM_ID = UUID.fromString("c633da85-8112-4453-af9c-c250e417179d");

  @SpyBean
  private KafkaInventoryEventListener listener;

  @MockBean
  private ContributionActionService actionService;

  @SpyBean
  private BatchDomainEventProcessor eventProcessor;

  @SpyBean
  private InnReachTransactionRepository transactionRepository;

  @Test
  void shouldReceiveInventoryItemEvent() {
    var event = createItemDomainEvent(DomainEventType.DELETED, UUID.randomUUID());

    kafkaTemplate.send(new ProducerRecord(INVENTORY_ITEM_TOPIC, RECORD_ID.toString(), event));

    ArgumentCaptor<List<ConsumerRecord<String, DomainEvent<Item>>>> eventsCaptor = ArgumentCaptor.forClass(List.class);

    await().atMost(ASYNC_AWAIT_TIMEOUT).untilAsserted(() ->
      verify(actionService).handleItemDelete(any()));

    verify(listener).handleItemEvents(eventsCaptor.capture());

    var records = eventsCaptor.getValue();
    assertEquals(1, records.size());

    var record1 = records.get(0);
    assertEquals(RECORD_ID.toString(), record1.key());
    assertEquals(event, record1.value());
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

    var record1 = records.get(0);
    assertEquals(RECORD_ID.toString(), record1.key());
    assertEquals(event, record1.value());
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

    var record1 = records.get(0);
    assertEquals(RECORD_ID.toString(), record1.key());
    assertEquals(event, record1.value());
  }

  @Test
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/inn-reach-transaction/pre-populate-inn-reach-transaction.sql",
  })
  void shouldHandleItemBarcodeUpdate() {
    var event = createItemDomainEvent(DomainEventType.UPDATED, PRE_POPULATED_LOCAL_ITEM_ID);
    var updatedItem = event.getData().getNewEntity();

    listener.handleItemEvents(asSingleConsumerRecord(INVENTORY_ITEM_TOPIC, PRE_POPULATED_LOCAL_ITEM_ID, event));

    var transaction = transactionRepository.fetchOneById(PRE_POPULATED_LOCAL_TRANSACTION_ID).orElseThrow();

    assertEquals(updatedItem.getBarcode(), transaction.getHold().getFolioItemBarcode());
  }

  @Test
  void testKafkaListenerListeningInnReachTopics() {
    var event1 = createItemDomainEvent(DomainEventType.DELETED, UUID.randomUUID());
    var event2 = createItemDomainEvent(DomainEventType.DELETED, UUID.randomUUID());
    event2.setTenant("testing1");
    var event3 = createItemDomainEvent(DomainEventType.DELETED, UUID.randomUUID());
    event3.setTenant("testing2");

    //Event is published to 3 different topics and all are listening but there are only 2 innreach tenants
    //so testing2 event gets discarded
    kafkaTemplate.send(new ProducerRecord(INVENTORY_ITEM_TOPIC, RECORD_ID.toString(), event1));
    kafkaTemplate.send(new ProducerRecord(INVENTORY_ITEM_TOPIC1, RECORD_ID.toString(), event2));
    kafkaTemplate.send(new ProducerRecord(INVENTORY_ITEM_TOPIC2, RECORD_ID.toString(), event3));

    await().atMost(ASYNC_AWAIT_TIMEOUT).untilAsserted(() ->
      verify(actionService, times(2)).handleItemDelete(any()));

  }

  @Test
  void testKafkaListenerHandlingErrorsAfterRetryExhausted() {
    var event1 = createItemDomainEvent(DomainEventType.UPDATED, UUID.randomUUID());
    event1.setTenant("testing4");

    //Event is published to 1 Inn reach topic but exception is thrown for this tenant when tenantScoped method is used
    //Since max retry is set to 0, error will be handled by kafka error handler
    kafkaTemplate.send(new ProducerRecord(INVENTORY_ITEM_TOPIC4, RECORD_ID.toString(), event1));

    await().atMost(ASYNC_AWAIT_TIMEOUT).untilAsserted(() ->
      verify(eventProcessor, times(1)).process(any(), any()));
    await().atMost(ASYNC_AWAIT_TIMEOUT).untilAsserted(() ->
      verify(actionService, times(0)).handleItemUpdate(any(), any()));

  }

  public DomainEvent<Item> getItemDomainEvent(DomainEventType eventType, UUID recordId) {
    return createItemDomainEvent(eventType, recordId);
  }

  private DomainEvent<Item> createItemDomainEvent(DomainEventType eventType, UUID recordId) {
    var oldItem = createItem().id(recordId);
    var newItem = createItem().id(recordId);

    return DomainEvent.<Item>builder()
      .tenant(TEST_TENANT_ID)
      .timestamp(System.currentTimeMillis())
      .type(eventType)
      .data(new EntityChangedData<>(newItem, oldItem))
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
