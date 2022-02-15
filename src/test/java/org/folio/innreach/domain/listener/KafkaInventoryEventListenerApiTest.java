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
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.function.Consumer;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlMergeMode;

import org.folio.innreach.domain.dto.folio.inventorystorage.LocationDTO;
import org.folio.innreach.domain.event.DomainEvent;
import org.folio.innreach.domain.event.DomainEventType;
import org.folio.innreach.domain.event.EntityChangedData;
import org.folio.innreach.domain.listener.base.BaseKafkaApiTest;
import org.folio.innreach.domain.service.ContributionValidationService;
import org.folio.innreach.domain.service.RecordContributionService;
import org.folio.innreach.domain.service.impl.BatchDomainEventProcessor;
import org.folio.innreach.domain.service.impl.FolioLocationService;
import org.folio.innreach.dto.Holding;
import org.folio.innreach.dto.Instance;
import org.folio.innreach.dto.Item;
import org.folio.innreach.dto.ItemStatus;
import org.folio.innreach.external.service.InnReachExternalService;

@Sql(
  scripts = {"classpath:db/contribution-criteria/clear-contribution-criteria-tables.sql",
    "classpath:db/itm-contrib-opt-conf/clear-itm-contrib-opt-conf-tables.sql",
    "classpath:db/central-server/clear-central-server-tables.sql"},
  executionPhase = AFTER_TEST_METHOD
)
@SqlMergeMode(MERGE)
class KafkaInventoryEventListenerApiTest extends BaseKafkaApiTest {
  private static final UUID RECORD_ID = UUID.randomUUID();
  private static final String TEST_TENANT_ID = "testing";
  private static final Duration ASYNC_AWAIT_TIMEOUT = Duration.ofSeconds(15);
  private static final UUID doNotContributeCode = UUID.fromString("5599f23f-d424-4fce-8a51-b7fce690cbda");
  private static final UUID EFFECTIVE_LOCATION_ID = UUID.randomUUID();
  private static final UUID LIBRARY_ID = UUID.fromString("7C244444-AE7C-11EB-8529-0242AC130004");

  @SpyBean
  private KafkaInventoryEventListener listener;
  @MockBean
  private InnReachExternalService innReachExternalService;
  @MockBean
  private FolioLocationService locationService;
  @SpyBean
  private BatchDomainEventProcessor eventProcessor;
  @SpyBean
  private RecordContributionService service;
  @SpyBean
  private ContributionValidationService validationService;

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
    when(locationService.getLocationById(any())).thenReturn(new LocationDTO(UUID.randomUUID(), LIBRARY_ID));
    var event = getItemDomainEvent(DomainEventType.DELETED);

    var consumerRecord = new ConsumerRecord(INVENTORY_ITEM_TOPIC, 1, 1, RECORD_ID.toString(), event);

    listener.handleItemEvents(List.of(consumerRecord));

    ArgumentCaptor<List<DomainEvent<Item>>> eventsCaptor = ArgumentCaptor.forClass(List.class);
    verify(eventProcessor).process(eventsCaptor.capture(), any(Consumer.class));

    var capturedEvents = eventsCaptor.getValue();
    assertEquals(1, capturedEvents.size());

    verify(service).decontributeInventoryItemEvents(event.getData().getOldEntity());
    verify(innReachExternalService).deleteInnReachApi(any(), contains(event.getData().getOldEntity().getHrid()));
  }

  @Test
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/contribution-criteria/pre-populate-contribution-criteria.sql"
  })
  void shouldDecontributeInvalidItems() {
    when(innReachExternalService.deleteInnReachApi(any(), any())).thenReturn("test");
    when(locationService.getLocationById(any())).thenReturn(new LocationDTO(UUID.randomUUID(), LIBRARY_ID));
    var event = getItemDomainEvent(DomainEventType.UPDATED);
    event.getData().getNewEntity().setStatisticalCodeIds(List.of(doNotContributeCode));

    var consumerRecord = new ConsumerRecord(INVENTORY_ITEM_TOPIC, 1, 1, RECORD_ID.toString(), event);

    listener.handleItemEvents(List.of(consumerRecord));

    ArgumentCaptor<List<DomainEvent<Item>>> eventsCaptor = ArgumentCaptor.forClass(List.class);
    verify(eventProcessor).process(eventsCaptor.capture(), any(Consumer.class));

    var capturedEvents = eventsCaptor.getValue();
    assertEquals(1, capturedEvents.size());

    verify(service).decontributeInventoryItemEvents(event.getData().getOldEntity());
    verify(innReachExternalService).deleteInnReachApi(any(), contains(event.getData().getOldEntity().getHrid()));
  }

  @Test
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/contribution-criteria/pre-populate-contribution-criteria.sql"
  })
  void shouldNotDecontributeValidItems() {
    when(innReachExternalService.deleteInnReachApi(any(), any())).thenReturn("test");
    when(locationService.getLocationById(any())).thenReturn(new LocationDTO(UUID.randomUUID(), LIBRARY_ID));
    var event = getItemDomainEvent(DomainEventType.UPDATED);

    var consumerRecord = new ConsumerRecord(INVENTORY_ITEM_TOPIC, 1, 1, RECORD_ID.toString(), event);

    listener.handleItemEvents(List.of(consumerRecord));

    ArgumentCaptor<List<DomainEvent<Item>>> eventsCaptor = ArgumentCaptor.forClass(List.class);
    verify(eventProcessor).process(eventsCaptor.capture(), any(Consumer.class));

    var capturedEvents = eventsCaptor.getValue();
    assertEquals(1, capturedEvents.size());
    verify(validationService).isEligibleForContribution(any(), eq(event.getData().getNewEntity()));
    verify(innReachExternalService, never()).deleteInnReachApi(any(), contains(event.getData().getOldEntity().getHrid()));
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
    when(locationService.getLocationById(any())).thenReturn(new LocationDTO(UUID.randomUUID(), LIBRARY_ID));
    var event = getInstanceDomainEvent(DomainEventType.DELETED);

    var consumerRecord = new ConsumerRecord(INVENTORY_INSTANCE_TOPIC, 1, 1, RECORD_ID.toString(), event);

    listener.handleInstanceEvents(List.of(consumerRecord));

    ArgumentCaptor<List<DomainEvent<Instance>>> eventsCaptor = ArgumentCaptor.forClass(List.class);
    verify(eventProcessor).process(eventsCaptor.capture(), any(Consumer.class));

    var capturedEvents = eventsCaptor.getValue();
    assertEquals(1, capturedEvents.size());

    verify(service).decontributeInventoryInstanceEvents(any());
    verify(innReachExternalService).deleteInnReachApi(any(), contains(event.getData().getOldEntity().getItems().get(0).getHrid()));
  }

  @Test
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/contribution-criteria/pre-populate-contribution-criteria.sql"
  })
  void shouldDecontributeInvalidInstances() {
    when(innReachExternalService.deleteInnReachApi(any(), any())).thenReturn("test");
    when(locationService.getLocationById(any())).thenReturn(new LocationDTO(UUID.randomUUID(), LIBRARY_ID));
    var event = getInstanceDomainEvent(DomainEventType.UPDATED);
    event.getData().getNewEntity().setStatisticalCodeIds(List.of(UUID.randomUUID(), UUID.randomUUID()));

    var consumerRecord = new ConsumerRecord(INVENTORY_INSTANCE_TOPIC, 1, 1, RECORD_ID.toString(), event);

    listener.handleInstanceEvents(List.of(consumerRecord));

    ArgumentCaptor<List<DomainEvent<Instance>>> eventsCaptor = ArgumentCaptor.forClass(List.class);
    verify(eventProcessor).process(eventsCaptor.capture(), any(Consumer.class));

    var capturedEvents = eventsCaptor.getValue();
    assertEquals(1, capturedEvents.size());

    verify(service).decontributeInventoryInstanceEvents(event.getData().getOldEntity());
    verify(innReachExternalService).deleteInnReachApi(any(), contains(event.getData().getOldEntity().getItems().get(0).getHrid()));
  }

  @Test
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/contribution-criteria/pre-populate-contribution-criteria.sql",
    "classpath:db/itm-contrib-opt-conf/pre-populate-itm-contrib-opt-conf.sql"
  })
  void shouldNotDecontributeValidInstances() {
    when(innReachExternalService.deleteInnReachApi(any(), any())).thenReturn("test");
    when(locationService.getLocationById(any())).thenReturn(new LocationDTO(UUID.randomUUID(), LIBRARY_ID));
    var event = getInstanceDomainEvent(DomainEventType.UPDATED);

    var consumerRecord = new ConsumerRecord(INVENTORY_INSTANCE_TOPIC, 1, 1, RECORD_ID.toString(), event);

    listener.handleInstanceEvents(List.of(consumerRecord));

    ArgumentCaptor<List<DomainEvent<Instance>>> eventsCaptor = ArgumentCaptor.forClass(List.class);
    verify(eventProcessor).process(eventsCaptor.capture(), any(Consumer.class));

    var capturedEvents = eventsCaptor.getValue();
    assertEquals(1, capturedEvents.size());

    verify(validationService).isEligibleForContribution(any(), eq(event.getData().getNewEntity()));
    verify(innReachExternalService, never()).deleteInnReachApi(any(), contains(event.getData().getOldEntity().getItems().get(0).getHrid()));
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
    when(locationService.getLocationById(any())).thenReturn(new LocationDTO(UUID.randomUUID(), LIBRARY_ID));
    var event = getHoldingDomainEvent(DomainEventType.DELETED);

    var consumerRecord = new ConsumerRecord(INVENTORY_HOLDING_TOPIC, 1, 1, RECORD_ID.toString(), event);

    listener.handleHoldingEvents(List.of(consumerRecord));

    ArgumentCaptor<List<DomainEvent<Holding>>> eventsCaptor = ArgumentCaptor.forClass(List.class);
    verify(eventProcessor).process(eventsCaptor.capture(), any(Consumer.class));

    var capturedEvents = eventsCaptor.getValue();
    assertEquals(1, capturedEvents.size());

    verify(service).decontributeInventoryHoldingEvents(any());
    verify(innReachExternalService).deleteInnReachApi(any(), contains(event.getData().getOldEntity().getHoldingsItems().get(0).getHrid()));
  }

  @Test
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/contribution-criteria/pre-populate-contribution-criteria.sql"
  })
  void shouldDecontributeInvalidHolding() {
    when(innReachExternalService.deleteInnReachApi(any(), any())).thenReturn("test");
    when(locationService.getLocationById(any())).thenReturn(new LocationDTO(UUID.randomUUID(), LIBRARY_ID));
    var event = getHoldingDomainEvent(DomainEventType.UPDATED);
    event.getData().getNewEntity().setStatisticalCodeIds(List.of(UUID.randomUUID(), UUID.randomUUID()));

    var consumerRecord = new ConsumerRecord(INVENTORY_HOLDING_TOPIC, 1, 1, RECORD_ID.toString(), event);

    listener.handleHoldingEvents(List.of(consumerRecord));

    ArgumentCaptor<List<DomainEvent<Holding>>> eventsCaptor = ArgumentCaptor.forClass(List.class);
    verify(eventProcessor).process(eventsCaptor.capture(), any(Consumer.class));

    var capturedEvents = eventsCaptor.getValue();
    assertEquals(1, capturedEvents.size());

    verify(service).decontributeInventoryHoldingEvents(event.getData().getOldEntity());
    verify(innReachExternalService).deleteInnReachApi(any(), contains(event.getData().getOldEntity().getHoldingsItems().get(0).getHrid()));
  }

  @Test
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/contribution-criteria/pre-populate-contribution-criteria.sql"
  })
  void shouldNotDecontributeValidHoldings() {
    when(innReachExternalService.deleteInnReachApi(any(), any())).thenReturn("test");
    when(locationService.getLocationById(any())).thenReturn(new LocationDTO(UUID.randomUUID(), LIBRARY_ID));
    var event = getHoldingDomainEvent(DomainEventType.UPDATED);

    var consumerRecord = new ConsumerRecord(INVENTORY_HOLDING_TOPIC, 1, 1, RECORD_ID.toString(), event);

    listener.handleHoldingEvents(List.of(consumerRecord));

    ArgumentCaptor<List<DomainEvent<Holding>>> eventsCaptor = ArgumentCaptor.forClass(List.class);
    verify(eventProcessor).process(eventsCaptor.capture(), any(Consumer.class));

    var capturedEvents = eventsCaptor.getValue();
    assertEquals(1, capturedEvents.size());

    verify(innReachExternalService, never()).deleteInnReachApi(any(), contains(event.getData().getOldEntity().getHoldingsItems().get(0).getHrid()));
  }

  private DomainEvent<Item> getItemDomainEvent(DomainEventType eventType) {
    var item = new Item()
      .id(RECORD_ID)
      .effectiveLocationId(EFFECTIVE_LOCATION_ID)
      .hrid(RandomStringUtils.randomAlphanumeric(10).toLowerCase(Locale.ROOT))
      .statisticalCodeIds(List.of(UUID.randomUUID()))
      .holdingStatisticalCodeIds(List.of(UUID.randomUUID()));

    return DomainEvent.<Item>builder()
      .tenant(TEST_TENANT_ID)
      .timestamp(System.currentTimeMillis())
      .type(eventType)
      .data(new EntityChangedData<>(item, item))
      .build();
  }

  private DomainEvent<Instance> getInstanceDomainEvent(DomainEventType eventType) {
    var status = new ItemStatus().name(ItemStatus.NameEnum.AVAILABLE);
    var instance = new Instance()
      .id(RECORD_ID)
      .hrid(RandomStringUtils.randomAlphanumeric(10).toLowerCase(Locale.ROOT))
      .statisticalCodeIds(List.of(UUID.randomUUID()))
      .source("MARC")
      .items(List.of(new Item()
        .hrid(RandomStringUtils.randomAlphanumeric(10).toLowerCase(Locale.ROOT))
        .effectiveLocationId(EFFECTIVE_LOCATION_ID)
        .statisticalCodeIds(List.of(UUID.randomUUID()))
        .status(status)));

    return DomainEvent.<Instance>builder()
      .tenant(TEST_TENANT_ID)
      .timestamp(System.currentTimeMillis())
      .type(eventType)
      .data(new EntityChangedData<>(instance, instance))
      .build();
  }

  private DomainEvent<Holding> getHoldingDomainEvent(DomainEventType eventType) {
    var status = new ItemStatus().name(ItemStatus.NameEnum.AVAILABLE);
    var holding = new Holding()
      .id(RECORD_ID)
      .permanentLocationId((EFFECTIVE_LOCATION_ID))
      .hrid(RandomStringUtils.randomAlphanumeric(10).toLowerCase(Locale.ROOT))
      .statisticalCodeIds(List.of(UUID.randomUUID()))
      .holdingsItems(List.of(new Item()
        .hrid(RandomStringUtils.randomAlphanumeric(10).toLowerCase(Locale.ROOT))
        .effectiveLocationId(EFFECTIVE_LOCATION_ID)
        .statisticalCodeIds(List.of(UUID.randomUUID()))
        .status(status)));

    return DomainEvent.<Holding>builder()
      .tenant(TEST_TENANT_ID)
      .timestamp(System.currentTimeMillis())
      .type(eventType)
      .data(new EntityChangedData<>(holding, holding))
      .build();
  }
}
