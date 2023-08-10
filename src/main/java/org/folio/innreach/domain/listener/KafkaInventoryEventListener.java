package org.folio.innreach.domain.listener;

import static org.folio.innreach.config.KafkaListenerConfiguration.KAFKA_CONTAINER_FACTORY;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.folio.innreach.external.exception.InnReachConnectionException;
import org.folio.innreach.external.exception.ServiceSuspendedException;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import org.folio.innreach.domain.event.DomainEvent;
import org.folio.innreach.domain.service.ContributionActionService;
import org.folio.innreach.domain.service.InnReachTransactionActionService;
import org.folio.innreach.domain.service.impl.BatchDomainEventProcessor;
import org.folio.innreach.dto.Holding;
import org.folio.innreach.dto.Instance;
import org.folio.innreach.dto.Item;

import static org.folio.innreach.domain.event.DomainEventType.CREATED;
import static org.folio.innreach.domain.event.DomainEventType.DELETED;
import static org.folio.innreach.domain.event.DomainEventType.UPDATED;

@Log4j2
@Component
@RequiredArgsConstructor
public class KafkaInventoryEventListener {
  private static final String UNKNOWN_TYPE_MESSAGE = "Received event of unknown type {}";

  private final BatchDomainEventProcessor eventProcessor;
  private final ContributionActionService contributionActionService;
  private final InnReachTransactionActionService transactionActionService;

  @KafkaListener(
    containerFactory = KAFKA_CONTAINER_FACTORY,
    id = "${kafka.listener.item.id}",
    groupId = "${kafka.listener.item.group-id}",
    topicPattern = "${kafka.listener.item.topic-pattern}",
    concurrency = "${kafka.listener.item.concurrency}")
  public void handleItemEvents(List<ConsumerRecord<String, DomainEvent<Item>>> consumerRecords) {
    log.info("Handling inventory item events from Kafka [number of events: {}]", consumerRecords.size());

    var events = getEvents(consumerRecords);
    logEvents(events);
    eventProcessor.process(events, event -> {
      var oldEntity = event.getData().getOldEntity();
      var newEntity = event.getData().getNewEntity();
      switch (event.getType()) {
        case CREATED:
          contributionActionService.handleItemCreation(newEntity);
          break;
        case UPDATED:
          contributionActionService.handleItemUpdate(newEntity, oldEntity);
          transactionActionService.handleItemUpdate(newEntity, oldEntity);
          break;
        case DELETED:
          contributionActionService.handleItemDelete(oldEntity);
          break;
        default:
          log.warn(UNKNOWN_TYPE_MESSAGE, event.getType());
      }
    });
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
    logEvents(events);
    eventProcessor.process(events, event -> {
      var oldEntity = event.getData().getOldEntity();
      var newEntity = event.getData().getNewEntity();
        switch (event.getType()) {
          case CREATED:
            contributionActionService.handleInstanceCreation(newEntity);
            break;
          case UPDATED:
            contributionActionService.handleInstanceUpdate(newEntity);
            break;
          case DELETED:
            contributionActionService.handleInstanceDelete(oldEntity);
            break;
          default:
            log.warn(UNKNOWN_TYPE_MESSAGE, event.getType());
        }
    });
  }

  @KafkaListener(
    containerFactory = KAFKA_CONTAINER_FACTORY,
    id = "${kafka.listener.holding.id}",
    groupId = "${kafka.listener.holding.group-id}",
    topicPattern = "${kafka.listener.holding.topic-pattern}",
    concurrency = "${kafka.listener.holding.concurrency}")
  public void handleHoldingEvents(List<ConsumerRecord<String, DomainEvent<Holding>>> consumerRecords) {
    log.info("Handling inventory holding events from Kafka [number of events: {}]", consumerRecords.size());

    var events = getEvents(consumerRecords);
    logEvents(events);
    eventProcessor.process(events, event -> {
      var oldEntity = event.getData().getOldEntity();
      var newEntity = event.getData().getNewEntity();
      switch (event.getType()) {
        case UPDATED:
          contributionActionService.handleHoldingUpdate(newEntity);
          break;
        case DELETED:
          contributionActionService.handleHoldingDelete(oldEntity);
          break;
        default:
          log.warn(UNKNOWN_TYPE_MESSAGE, event.getType());
      }
    });
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
}
