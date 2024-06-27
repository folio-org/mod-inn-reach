package org.folio.innreach.domain.listener;

import static org.folio.innreach.config.KafkaListenerConfiguration.KAFKA_CONTAINER_FACTORY;
import static org.folio.innreach.domain.event.DomainEventType.CREATED;
import static org.folio.innreach.domain.event.DomainEventType.DELETED;
import static org.folio.innreach.domain.event.DomainEventType.UPDATED;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.folio.innreach.domain.entity.OngoingContributionStatus;
import org.folio.innreach.domain.service.KafkaEventProcessorService;
import org.folio.innreach.mapper.OngoingContributionStatusMapper;
import org.folio.innreach.repository.CentralServerRepository;
import org.folio.innreach.repository.OngoingContributionStatusRepository;
import org.folio.spring.data.OffsetRequest;
import org.springframework.data.domain.Page;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import org.folio.innreach.domain.event.DomainEvent;
import org.folio.innreach.domain.service.ContributionActionService;
import org.folio.innreach.domain.service.InnReachTransactionActionService;
import org.folio.innreach.domain.service.impl.BatchDomainEventProcessor;
import org.folio.innreach.dto.Holding;
import org.folio.innreach.dto.Instance;
import org.folio.innreach.dto.Item;

@Log4j2
@Component
@RequiredArgsConstructor
public class KafkaInventoryEventListener {
  private final BatchDomainEventProcessor eventProcessor;
  private final ContributionActionService contributionActionService;
  private final InnReachTransactionActionService transactionActionService;
  private final OngoingContributionStatusMapper ongoingContributionStatusMapper;
  private final OngoingContributionStatusRepository ongoingContributionStatusRepository;
  private final KafkaEventProcessorService kafkaEventProcessorService;
  private final CentralServerRepository centralServerRepository;

  @KafkaListener(
    containerFactory = KAFKA_CONTAINER_FACTORY,
    id = "${kafka.listener.item.id}",
    groupId = "${kafka.listener.item.group-id}",
    topicPattern = "${kafka.listener.item.topic-pattern}",
    concurrency = "${kafka.listener.item.concurrency}")
  public void handleItemEvents(List<ConsumerRecord<String, DomainEvent<Item>>> consumerRecords) {
    log.info("Handling inventory item events from Kafka [number of events: {}]", consumerRecords.size());
    processEvents(ongoingContributionStatusMapper::convertItemListToEntities, consumerRecords);
  }

  @KafkaListener(
    containerFactory = KAFKA_CONTAINER_FACTORY,
    id = "${kafka.listener.instance.id}",
    groupId = "${kafka.listener.instance.group-id}",
    topicPattern = "${kafka.listener.instance.topic-pattern}",
    concurrency = "${kafka.listener.instance.concurrency}")
  public void handleInstanceEvents(List<ConsumerRecord<String, DomainEvent<Instance>>> consumerRecords) {
    log.info("Handling inventory instance events from Kafka [number of events: {}]", consumerRecords.size());
    var events = getEvents(consumerRecords);
    processEvents(ongoingContributionStatusMapper::convertInstanceListToEntities, consumerRecords);
  }

  @KafkaListener(
    containerFactory = KAFKA_CONTAINER_FACTORY,
    id = "${kafka.listener.holding.id}",
    groupId = "${kafka.listener.holding.group-id}",
    topicPattern = "${kafka.listener.holding.topic-pattern}",
    concurrency = "${kafka.listener.holding.concurrency}")
  public void handleHoldingEvents(List<ConsumerRecord<String, DomainEvent<Holding>>> consumerRecords) {
    log.info("Handling inventory holding events from Kafka [number of events: {}]", consumerRecords.size());
    processEvents(ongoingContributionStatusMapper::convertHoldingListToEntities, consumerRecords);
  }

  private <T> void processEvents(Function<List<DomainEvent<T>>, List<OngoingContributionStatus>> convertDomainEventToEntities,
                                 List<ConsumerRecord<String, DomainEvent<T>>> consumerRecords) {
    var events = getEvents(consumerRecords);
    logEvents(events);
    kafkaEventProcessorService.process(events, (tenantGroupedEvents, tenant) -> getCentralServerIds().forEach(centralServerId -> {
      var ongoingContributionStatusList = convertDomainEventToEntities.apply(tenantGroupedEvents);
      ongoingContributionStatusList.forEach(ongoingContributionStatus -> {
        ongoingContributionStatus.setCentralServerId(centralServerId);
        ongoingContributionStatus.setTenant(tenant);
      });
      ongoingContributionStatusRepository.saveAll(ongoingContributionStatusList);
    }));
  }

  private static <T> List<DomainEvent<T>> getEvents(List<ConsumerRecord<String, DomainEvent<T>>> consumerRecords) {
    return consumerRecords.stream()
      .map(ConsumerRecord::value)
      .filter(Objects::nonNull)
      .collect(Collectors.toList());
  }

  public <T> void logEvents(List<DomainEvent<T>> events) {
    for (DomainEvent<T> event : events) {
      var oldEntity = event.getData().getOldEntity();
      var newEntity = event.getData().getNewEntity();
      log.info("handleEvents:: Event type: {}, tenant: {}, timestamp: {}, data: {}", event.getType(), event.getTenant(), event.getTimestamp(), event.getData());
      if (event.getType().equals(CREATED)) {
        log.info("created handleEvents:: New Entity: {}", newEntity);
      }
      else if (event.getType().equals(UPDATED)) {
        log.info("updated handleEvents:: Old Entity: {}, New Entity: {}", oldEntity, newEntity);
      }
      else if (event.getType().equals(DELETED)) {
        log.info("deleted handleEvents:: Old Entity: {}", oldEntity);
      }
    }
  }

  private List<UUID> getCentralServerIds() {
    Page<UUID> ids = centralServerRepository.getIds(new OffsetRequest(0, 2000));
    return ids.getContent();
  }

}
