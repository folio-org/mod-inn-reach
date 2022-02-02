package org.folio.innreach.domain.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.folio.innreach.domain.entity.CentralServer;
import org.folio.innreach.domain.event.DomainEvent;
import org.folio.innreach.domain.service.EvaluateService;
import org.folio.innreach.dto.Holding;
import org.folio.innreach.dto.Instance;
import org.folio.innreach.dto.Item;
import org.folio.innreach.repository.CentralServerRepository;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

import static org.folio.innreach.config.KafkaListenerConfiguration.KAFKA_CONTAINER_FACTORY;

@Log4j2
@RequiredArgsConstructor
@Component
public class KafkaInventoryEventListener {

  private final CentralServerRepository centralServerRepository;
  private final EvaluateService evaluateService;

  @KafkaListener(
    containerFactory = KAFKA_CONTAINER_FACTORY,
    id = "${kafka.listener.item.id}",
    groupId = "${kafka.listener.item.group-id}",
    topicPattern = "${kafka.listener.item.topic-pattern}",
    concurrency = "${kafka.listener.item.concurrency}"
  )
  public void handleInventoryItemEvents(List<ConsumerRecord<String, DomainEvent<Item>>> consumerRecords) {
    log.info("Handling inventory events from Kafka [number of events: {}]", consumerRecords.size());

    var centralServers = centralServerRepository.findAll();
    var centralServerCodes = centralServers.stream()
      .map(CentralServer::getCentralServerCode)
      .collect(Collectors.toList());

    consumerRecords.forEach(event -> {
      var newItem = event.value().getData().getNewEntity();
      switch (event.value().getType()) {
        case CREATED:
          evaluateService.handleItemEvent(newItem, centralServerCodes);
          break;
        case UPDATED:
          evaluateService.handleItemEvent(newItem, centralServerCodes);
          break;
        default:
          log.warn("Received Item event of unknown type {}", event.value().getType());
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
  public void handleInventoryHoldingEvents(List<ConsumerRecord<String, DomainEvent<Holding>>> consumerRecords) {
    log.info("Handling inventory events from Kafka [number of events: {}]", consumerRecords.size());

    var centralServers = centralServerRepository.findAll();
    var centralServerCodes = centralServers.stream()
      .map(CentralServer::getCentralServerCode)
      .collect(Collectors.toList());

    consumerRecords.forEach(event -> {
      var newHolding = event.value().getData().getNewEntity();
      switch (event.value().getType()) {
        case CREATED:
          evaluateService.handleHoldingEvent(newHolding, centralServerCodes);
          break;
        default:
          log.warn("Received Holding event of unknown type {}", event.value().getType());
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
  public void handleInventoryInstanceEvents(List<ConsumerRecord<String, DomainEvent<Instance>>> consumerRecords) {
    log.info("Handling inventory events from Kafka [number of events: {}]", consumerRecords.size());

    var centralServers = centralServerRepository.findAll();
    var centralServerCodes = centralServers.stream()
      .map(CentralServer::getCentralServerCode)
      .collect(Collectors.toList());

    consumerRecords.forEach(event -> {
      var newInstance = event.value().getData().getNewEntity();
      switch (event.value().getType()) {
        case CREATED:
          evaluateService.handleInstanceEvent(newInstance, centralServerCodes);
          break;
        default:
          log.warn("Received Instance event of unknown type {}", event.value().getType());
      }
    });
  }

}
