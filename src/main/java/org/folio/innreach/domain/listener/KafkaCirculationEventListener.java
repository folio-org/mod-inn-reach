package org.folio.innreach.domain.listener;

import static org.folio.innreach.config.KafkaListenerConfiguration.KAFKA_CONTAINER_FACTORY;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import org.folio.innreach.domain.dto.folio.circulation.RequestDTO;
import org.folio.innreach.domain.event.DomainEvent;
import org.folio.innreach.domain.service.InnReachTransactionActionService;
import org.folio.innreach.domain.service.impl.BatchDomainEventProcessor;
import org.folio.innreach.dto.CheckInDTO;
import org.folio.innreach.dto.LoanDTO;

@Log4j2
@Component
@RequiredArgsConstructor
public class KafkaCirculationEventListener {

  private final BatchDomainEventProcessor eventProcessor;
  private final InnReachTransactionActionService transactionActionService;

  @KafkaListener(
    containerFactory = KAFKA_CONTAINER_FACTORY,
    id = "${kafka.listener.loan.id}",
    groupId = "${kafka.listener.loan.group-id}",
    topicPattern = "${kafka.listener.loan.topic-pattern}",
    concurrency = "${kafka.listener.loan.concurrency}")
  public void handleLoanEvents(List<ConsumerRecord<String, DomainEvent<LoanDTO>>> consumerRecords) {
    log.info("Handling circulation Loan events from Kafka [number of events: {}]", consumerRecords.size());

    var events = getEvents(consumerRecords);

    eventProcessor.process(events, event -> {
      switch (event.getType()) {
        case CREATED:
          transactionActionService.associateNewLoanWithTransaction(event.getData().getNewEntity());
          break;
        case UPDATED:
          transactionActionService.handleLoanUpdate(event.getData().getNewEntity());
          break;
        default:
          log.warn("Received event of unknown type {}", event.getType());
      }
    });
  }

  @KafkaListener(
    containerFactory = KAFKA_CONTAINER_FACTORY,
    id = "${kafka.listener.request.id}",
    groupId = "${kafka.listener.request.group-id}",
    topicPattern = "${kafka.listener.request.topic-pattern}",
    concurrency = "${kafka.listener.request.concurrency}")
  public void handleRequestEvents(List<ConsumerRecord<String, DomainEvent<RequestDTO>>> consumerRecords) {
    log.info("Handling circulation Requests events from Kafka [number of events: {}]", consumerRecords.size());

    var events = getEvents(consumerRecords);

    eventProcessor.process(events, event -> {
      switch (event.getType()) {
        case CREATED:
          //
          break;
        case UPDATED:
          transactionActionService.handleRequestUpdate(event.getData().getNewEntity());
          break;
        default:
          log.warn("Received event of unknown type {}", event.getType());
      }
    });
  }

  @KafkaListener(
    containerFactory = KAFKA_CONTAINER_FACTORY,
    id = "${kafka.listener.check-in.id}",
    groupId = "${kafka.listener.check-in.group-id}",
    topicPattern = "${kafka.listener.check-in.topic-pattern}",
    concurrency = "${kafka.listener.check-in.concurrency}")
  public void handleCheckInEvents(List<ConsumerRecord<String, DomainEvent<CheckInDTO>>> consumerRecords) {
    log.info("Handling circulation Check-In events from Kafka [number of events: {}]", consumerRecords.size());

    var events = getEvents(consumerRecords);

    eventProcessor.process(events, event -> {
      switch (event.getType()) {
        case CREATED:
          transactionActionService.handleCheckInCreation(event.getData().getNewEntity());
          break;
        default:
          log.warn("Received event of unknown type {}", event.getType());
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
