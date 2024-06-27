package org.folio.innreach.domain.listener;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.AFTER_TEST_METHOD;
import static org.springframework.test.context.jdbc.SqlMergeMode.MergeMode.MERGE;

import static org.folio.innreach.fixture.ContributionFixture.createHolding;
import static org.folio.innreach.fixture.ContributionFixture.createInstance;
import static org.folio.innreach.fixture.ContributionFixture.createItem;

import java.time.Duration;
import java.util.UUID;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.folio.innreach.batch.contribution.service.ContributionJobRunner;
import org.folio.innreach.domain.service.impl.BatchDomainEventProcessor;
import org.folio.innreach.repository.OngoingContributionStatusRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
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

  @Autowired
  private OngoingContributionStatusRepository ongoingContributionRepository;

  @Test
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/central-server/pre-populate-another-central-server.sql",
  })
  void shouldReceiveInventoryItemEvent() {
    long initialSize = ongoingContributionRepository.count();
    var event1 = createItemDomainEvent(DomainEventType.DELETED, UUID.randomUUID());
    var event2 = createItemDomainEvent(DomainEventType.ALL_DELETED, UUID.randomUUID());

    kafkaTemplate.send(new ProducerRecord(INVENTORY_ITEM_TOPIC, RECORD_ID.toString(), event1));
    kafkaTemplate.send(new ProducerRecord(INVENTORY_ITEM_TOPIC, RECORD_ID.toString(), event2));

    // As there are 2 central servers, there will be an entry against each centralServerId
    await().atMost(ASYNC_AWAIT_TIMEOUT).untilAsserted(() ->
      assertEquals(initialSize+4, ongoingContributionRepository.count()));
  }

  @Test
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql"
  })
  void shouldReceiveInventoryHoldingEvent() {
    long initialSize = ongoingContributionRepository.count();
    var event1 = createHoldingDomainEvent(DomainEventType.DELETED);
    var event2 = createHoldingDomainEvent(DomainEventType.CREATED);
    var event3 = createHoldingDomainEvent(DomainEventType.UPDATED);

    kafkaTemplate.send(new ProducerRecord(INVENTORY_HOLDING_TOPIC, RECORD_ID.toString(), event1));
    kafkaTemplate.send(new ProducerRecord(INVENTORY_HOLDING_TOPIC, RECORD_ID.toString(), event2));
    kafkaTemplate.send(new ProducerRecord(INVENTORY_HOLDING_TOPIC, RECORD_ID.toString(), event3));

    // As there is 1 central server, there will be an entry against one centralServerId
    await().atMost(ASYNC_AWAIT_TIMEOUT).untilAsserted(() ->
      assertEquals(initialSize+3, ongoingContributionRepository.count()));
  }

  @Test
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/central-server/pre-populate-another-central-server.sql",
  })
  void shouldReceiveInventoryInstanceEvent() {
    long initialSize = ongoingContributionRepository.count();
    var event1 = createInstanceDomainEvent(DomainEventType.CREATED);
    var event2 = createInstanceDomainEvent(DomainEventType.DELETED);
    var event3 = createInstanceDomainEvent(DomainEventType.UPDATED);

    kafkaTemplate.send(new ProducerRecord(INVENTORY_INSTANCE_TOPIC, RECORD_ID.toString(), event1));
    kafkaTemplate.send(new ProducerRecord(INVENTORY_INSTANCE_TOPIC, RECORD_ID.toString(), event2));
    kafkaTemplate.send(new ProducerRecord(INVENTORY_INSTANCE_TOPIC, RECORD_ID.toString(), event3));

    // As there is 2 central server, there will be 2 entry against each centralServerId
    await().atMost(ASYNC_AWAIT_TIMEOUT).untilAsserted(() ->
      assertEquals(initialSize+6, ongoingContributionRepository.count()));
  }

  @Test
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql",
  })
  void testKafkaListenerListeningInnReachTopics() {
    long initialSize = ongoingContributionRepository.count();
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
      assertEquals(initialSize+2, ongoingContributionRepository.count()));

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
