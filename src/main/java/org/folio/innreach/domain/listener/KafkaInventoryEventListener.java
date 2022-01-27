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
import org.folio.innreach.repository.CentralServerRepository;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.folio.innreach.config.KafkaListenerConfiguration.KAFKA_CONTAINER_FACTORY;

@Log4j2
@Component
@RequiredArgsConstructor
public class KafkaInventoryEventListener {
  private static final String UNKNOWN_TYPE_MESSAGE = "Received event of unknown type {}";

  private final BatchDomainEventProcessor eventProcessor;
  private final RecordContributionService recordService;
  private final CentralServerRepository centralServerRepository;

  @KafkaListener(
    containerFactory = KAFKA_CONTAINER_FACTORY,
    id = "${kafka.listener.item.id}",
    groupId = "${kafka.listener.item.group-id}",
    topicPattern = "${kafka.listener.item.topic-pattern}",
    concurrency = "${kafka.listener.item.concurrency}")
  public void handleItemEvents(List<ConsumerRecord<String, DomainEvent<Item>>> consumerRecords) {
    log.info("Handling inventory item events from Kafka [number of events: {}]", consumerRecords.size());

    var events = consumerRecords.stream()
      .filter(e -> e.value() != null)
      .map(this::toDomainEventWithRecordId)
      .collect(Collectors.toList());

    eventProcessor.process(events, event -> {
      var centralServersCodes = centralServerRepository.getIds();
      switch (event.getType()) {
        case UPDATED:
          var notValidCodes = recordService.evaluateInventoryItemForContribution(event.getData().getNewEntity(), centralServersCodes);
          if (!notValidCodes.isEmpty()) {
            recordService.decontributeInventoryItemEvents(event.getData().getOldEntity(), notValidCodes);
          }
          break;
        case DELETED:
          recordService.decontributeInventoryItemEvents(event.getData().getOldEntity(), centralServersCodes);
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

    var events = consumerRecords.stream()
      .filter(e -> e.value() != null)
      .map(this::toDomainEventWithRecordId)
      .collect(Collectors.toList());

    eventProcessor.process(events, event -> {
      var centralServersCodes = centralServerRepository.getIds();
      switch (event.getType()) {
        case UPDATED:
          var notValidCodes = recordService.evaluateInventoryInstanceForContribution(event.getData().getNewEntity(), centralServersCodes);
          if (!notValidCodes.isEmpty()) {
            recordService.decontributeInventoryInstanceEvents(event.getData().getOldEntity(), notValidCodes);
          }
          break;
        case DELETED:
          recordService.decontributeInventoryInstanceEvents(event.getData().getOldEntity(), centralServersCodes);
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

    var events = consumerRecords.stream()
      .filter(e -> e.value() != null)
      .map(this::toDomainEventWithRecordId)
      .collect(Collectors.toList());

    eventProcessor.process(events, event -> {
      var centralServersCodes = centralServerRepository.getIds();
      switch (event.getType()) {
        case UPDATED:
          var notValidCodes = recordService.evaluateInventoryHoldingForContribution(event.getData().getNewEntity(), centralServersCodes);
          if (!notValidCodes.isEmpty()) {
            recordService.decontributeInventoryHoldingEvents(event.getData().getOldEntity(), notValidCodes);
          }
          break;
        case DELETED:
          recordService.decontributeInventoryHoldingEvents(event.getData().getOldEntity(), centralServersCodes);
          break;
        default:
          log.warn(UNKNOWN_TYPE_MESSAGE, event.getType());
      }
    });
  }

  private <T> DomainEvent<T> toDomainEventWithRecordId(ConsumerRecord<String, DomainEvent<T>> consumerRecord) {
    var key = consumerRecord.key();
    var event = consumerRecord.value();
    event.setRecordId(UUID.fromString(key));
    return event;
  }
}
