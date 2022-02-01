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
    id = "${kafka.listener.inventory.id}",
    groupId = "${kafka.listener.inventory.group-id}",
    topicPattern = "${kafka.listener.inventory.topic-pattern}",
    concurrency = "${kafka.listener.inventory.concurrency}"
  )
  public void handleInventoryEvents(List<ConsumerRecord<String, DomainEvent>> consumerRecords) {
    log.info("Handling inventory events from Kafka [number of events: {}]", consumerRecords.size());

    var centralServers = centralServerRepository.findAll();
    var centralServerCodes = centralServers.stream()
      .map(CentralServer::getCentralServerCode)
      .collect(Collectors.toList());

    consumerRecords.forEach(event -> {

      if (Item.class.equals(event.value().getData().getNewEntity().getClass())) {
        switch (event.value().getType()) {
          case CREATED:
            Item item = (Item) event.value().getData().getNewEntity();
            evaluateService.handleItemEvent(item, centralServerCodes);
            break;
          case UPDATED:
            item = (Item) event.value().getData().getNewEntity();
            evaluateService.handleItemEvent(item, centralServerCodes);
            break;
          default:
            log.warn("Received Item event of unknown type {}", event.value().getType());
        }
      }

      if (Holding.class.equals(event.value().getData().getNewEntity().getClass())) {
        switch (event.value().getType()) {
          case CREATED:
            Holding holding = (Holding) event.value().getData().getNewEntity();
            evaluateService.handleHoldingEvent(holding, centralServerCodes);
            break;
          default:
            log.warn("Received Holding event of unknown type {}", event.value().getType());
        }
      }

      if (Instance.class.equals(event.value().getData().getNewEntity().getClass())) {
        switch (event.value().getType()) {
          case CREATED:
            Instance instance = (Instance) event.value().getData().getNewEntity();
            evaluateService.handleInstanceEvent(instance, centralServerCodes);
            break;
          default:
            log.warn("Received Instance event of unknown type {}", event.value().getType());
        }
      }
    });
  }
}
