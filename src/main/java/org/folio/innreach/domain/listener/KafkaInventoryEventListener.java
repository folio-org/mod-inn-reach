package org.folio.innreach.domain.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.folio.innreach.domain.event.DomainEvent;
import org.folio.innreach.domain.service.RecordContributionService;
import org.folio.innreach.domain.service.impl.BatchDomainEventProcessor;
import org.folio.innreach.dto.Holding;
import org.folio.innreach.dto.Instance;
import org.folio.innreach.dto.Item;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static org.folio.innreach.config.KafkaListenerConfiguration.KAFKA_CONTAINER_FACTORY;

@Log4j2
@RequiredArgsConstructor
@Component
public class KafkaInventoryEventListener {

  private final BatchDomainEventProcessor eventProcessor;
  private final RecordContributionService recordService;

  @KafkaListener(
    containerFactory = KAFKA_CONTAINER_FACTORY,
    id = "${kafka.listener.item.id}",
    groupId = "${kafka.listener.item.group-id}",
    topicPattern = "${kafka.listener.item.topic-pattern}",
    concurrency = "${kafka.listener.item.concurrency}"
  )
  public void handleItemEvents(List<ConsumerRecord<String, DomainEvent<Item>>> consumerRecords) {
    log.info("Handling inventory events from Kafka [number of events: {}]", consumerRecords.size());

    var events = getEvents(consumerRecords);

    eventProcessor.process(events, event -> {
      var newItem = event.getData().getNewEntity();
      var oldItem = event.getData().getOldEntity();

      switch (event.getType()) {
        case CREATED:
          recordService.contributeInventoryItemEvents(newItem);
          break;
        case UPDATED:
          recordService.updateInventoryItem(oldItem, newItem);
          break;
        case DELETED:
          recordService.decontributeInventoryItemEvents(event.getData().getOldEntity());
          break;
        default:
          log.warn("Received Item event of unknown type {}", event.getType());
      }
    });
  }

  @KafkaListener(
    containerFactory = KAFKA_CONTAINER_FACTORY,
    id = "${kafka.listener.holding.id}",
    groupId = "${kafka.listener.holding.group-id}",
    topicPattern = "${kafka.listener.holding.topic-pattern}",
    concurrency = "${kafka.listener.holding.concurrency}"
  )
  public void handleHoldingEvents(List<ConsumerRecord<String, DomainEvent<Holding>>> consumerRecords) {
    log.info("Handling inventory events from Kafka [number of events: {}]", consumerRecords.size());

    var events = getEvents(consumerRecords);

    eventProcessor.process(events, event -> {
      var newHolding = event.getData().getNewEntity();

      switch (event.getType()) {
        case CREATED:
          recordService.contributeInventoryHoldingEvents(newHolding);
          break;
        case UPDATED:
          recordService.updateInventoryHolding(event.getData().getOldEntity(), event.getData().getNewEntity());
          break;
        case DELETED:
          recordService.decontributeInventoryHoldingEvents(event.getData().getOldEntity());
          break;
        default:
          log.warn("Received Holding event of unknown type {}", event.getType());
      }
    });
  }

  @KafkaListener(
    containerFactory = KAFKA_CONTAINER_FACTORY,
    id = "${kafka.listener.instance.id}",
    groupId = "${kafka.listener.instance.group-id}",
    topicPattern = "${kafka.listener.instance.topic-pattern}",
    concurrency = "${kafka.listener.instance.concurrency}"
  )
  public void handleInstanceEvents(List<ConsumerRecord<String, DomainEvent<Instance>>> consumerRecords) {
    log.info("Handling inventory events from Kafka [number of events: {}]", consumerRecords.size());

    var events = getEvents(consumerRecords);

    eventProcessor.process(events, event -> {
      var newInstance = event.getData().getNewEntity();

      switch (event.getType()) {
        case CREATED:
          recordService.contributeInventoryInstanceEvents(newInstance);
          break;
        case UPDATED:
          recordService.updateInventoryInstance(event.getData().getOldEntity(), event.getData().getNewEntity());
          break;
        case DELETED:
          recordService.decontributeInventoryInstanceEvents(event.getData().getOldEntity());
          break;
        default:
          log.warn("Received Instance event of unknown type {}", event.getType());
      }
    });
  }

  private static <T> List<DomainEvent<T>> getEvents(List<ConsumerRecord<String, DomainEvent<T>>> consumerRecords) {
    return consumerRecords.stream()
      .map(ConsumerRecord::value)
      .filter(Objects::nonNull)
      .collect(Collectors.toList());
  }
}
