package org.folio.innreach.domain.listener;

import static org.awaitility.Awaitility.await;
import static org.folio.innreach.domain.listener.KafkaListenersConstants.INVENTORY_HOLDING_TOPIC;
import static org.folio.innreach.domain.listener.KafkaListenersConstants.INVENTORY_INSTANCE_TOPIC;
import static org.folio.innreach.domain.listener.KafkaListenersConstants.INVENTORY_ITEM_TOPIC;
import static org.folio.innreach.domain.listener.KafkaListenersConstants.INVENTORY_ITEM_TOPIC1;
import static org.folio.innreach.domain.listener.KafkaListenersConstants.INVENTORY_ITEM_TOPIC2;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.AFTER_TEST_METHOD;
import static org.springframework.test.context.jdbc.SqlMergeMode.MergeMode.MERGE;

import static org.folio.innreach.fixture.ContributionFixture.createHolding;
import static org.folio.innreach.fixture.ContributionFixture.createInstance;
import static org.folio.innreach.fixture.ContributionFixture.createItem;

import java.time.Duration;
import java.util.UUID;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.folio.innreach.it.base.BaseTenantIT;
import org.folio.innreach.repository.OngoingContributionStatusRepository;
import org.folio.innreach.support.TestJdbcHelper;
import org.folio.tenant.domain.dto.TenantAttributes;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlMergeMode;

import org.folio.innreach.domain.event.DomainEvent;
import org.folio.innreach.domain.event.DomainEventType;
import org.folio.innreach.domain.event.EntityChangedData;
import org.folio.innreach.dto.Holding;
import org.folio.innreach.dto.Instance;
import org.folio.innreach.dto.Item;

@Sql(
  scripts = {"classpath:db/inn-reach-transaction/clear-inn-reach-transaction-tables.sql",
    "classpath:db/central-server/clear-central-server-tables.sql",
  },
  executionPhase = AFTER_TEST_METHOD
)
@SqlMergeMode(MERGE)
class KafkaInventoryEventListenerApiIT extends BaseTenantIT {
  private static final UUID RECORD_ID = UUID.randomUUID();
  private static final Duration ASYNC_AWAIT_TIMEOUT = Duration.ofSeconds(15);

  private static final String TEST_TENANT_ID = "testing";
  private static final String TEST_TENANT1_ID = "testing1";
  private static final String TEST_TENANT2_ID = "testing2";
  private static final String TEST_TENANT4_ID = "testing4";

  private static KafkaTemplate<String, DomainEvent> kafkaTemplate;

  @Autowired
  private OngoingContributionStatusRepository ongoingContributionRepository;

  @Autowired
  private TestJdbcHelper testJdbcHelper;

  @BeforeAll
  static void setUp() {
    kafkaTemplate = buildKafkaTemplate();
    enableTenant(TEST_TENANT1_ID, new TenantAttributes());
    enableTenant(TEST_TENANT2_ID, new TenantAttributes());
    enableTenant(TEST_TENANT4_ID, new TenantAttributes());
  }

  @AfterAll
  static void tearDownExtraTenants() {
    purgeTenant(TEST_TENANT1_ID);
    purgeTenant(TEST_TENANT2_ID);
    purgeTenant(TEST_TENANT4_ID);
  }

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
      assertEquals(initialSize + 4, ongoingContributionRepository.count()));
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
      assertEquals(initialSize + 3, ongoingContributionRepository.count()));
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
      assertEquals(initialSize + 6, ongoingContributionRepository.count()));
  }

  @Test
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql",
  })
  void testKafkaListenerListeningInnReachTopics() {
    testJdbcHelper.executeSqlScript(TEST_TENANT1_ID, "db/central-server/pre-populate-central-server.sql");
    long initialSize = countAcrossAllTenants();
    var event1 = createItemDomainEvent(DomainEventType.DELETED, UUID.randomUUID(), TEST_TENANT_ID);
    var event2 = createItemDomainEvent(DomainEventType.DELETED, UUID.randomUUID(), TEST_TENANT1_ID);
    var event3 = createItemDomainEvent(DomainEventType.DELETED, UUID.randomUUID(), TEST_TENANT2_ID);

    // Events published to 3 different topics matching the inventory.item pattern.
    // The consumer's topicPattern picks up all matching topics regardless of the tenant ID.
    // InnReachTenants = testing|testing1|testing4.
    // Events for testing and testing1 are persisted (one record each with 1 central server),
    // events for testing2 are skipped (not in InnReachTenants), no events for testing4.
    kafkaTemplate.send(new ProducerRecord(INVENTORY_ITEM_TOPIC, RECORD_ID.toString(), event1));
    kafkaTemplate.send(new ProducerRecord(INVENTORY_ITEM_TOPIC1, RECORD_ID.toString(), event2));
    kafkaTemplate.send(new ProducerRecord(INVENTORY_ITEM_TOPIC2, RECORD_ID.toString(), event3));

    await().atMost(ASYNC_AWAIT_TIMEOUT).untilAsserted(() ->
      assertEquals(initialSize + 2, countAcrossAllTenants()));

    testJdbcHelper.executeSqlScript(TEST_TENANT1_ID, "db/central-server/clear-central-server-tables.sql");
  }

  public DomainEvent<Item> getItemDomainEvent(DomainEventType eventType, UUID recordId) {
    return createItemDomainEvent(eventType, recordId);
  }

  private DomainEvent<Item> createItemDomainEvent(DomainEventType eventType, UUID recordId) {
    return createItemDomainEvent(eventType, recordId, TEST_TENANT_ID);
  }

  private DomainEvent<Item> createItemDomainEvent(DomainEventType eventType, UUID recordId, String tenantId) {
    var oldItem = createItem().id(recordId);
    var newItem = createItem().id(recordId);

    return DomainEvent.<Item>builder()
      .tenant(tenantId)
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

  private long countAcrossAllTenants() {
    return testJdbcHelper.count(TEST_TENANT_ID, "ongoing_contribution_status")
      + testJdbcHelper.count(TEST_TENANT1_ID, "ongoing_contribution_status")
      + testJdbcHelper.count(TEST_TENANT2_ID, "ongoing_contribution_status")
      + testJdbcHelper.count(TEST_TENANT4_ID, "ongoing_contribution_status");
  }
}
