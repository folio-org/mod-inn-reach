package org.folio.innreach.domain.listener;

import static org.folio.innreach.config.KafkaListenerConfiguration.KAFKA_CONTAINER_FACTORY;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.folio.innreach.domain.dto.folio.circulation.RequestDTO;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import org.folio.innreach.domain.event.DomainEvent;
import org.folio.innreach.domain.service.InnReachTransactionActionService;
import org.folio.innreach.domain.service.impl.BatchDomainEventProcessor;
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

    var events = consumerRecords.stream()
      .filter(e -> e.value() != null)
      .map(this::toDomainEventWithRecordId)
      .collect(Collectors.toList());

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
  public void handleRequestStorage(List<ConsumerRecord<String, DomainEvent<RequestDTO>>> consumerRecords) {
    log.info("Handling circulation Requests events from Kafka [number of events: {}]", consumerRecords.size());
    var events = consumerRecords.stream()
      .filter(e -> e.value() != null)
      .map(this::toDomainEventWithRecordId)
      .collect(Collectors.toList());

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

  private <T> DomainEvent<T> toDomainEventWithRecordId(ConsumerRecord<String, DomainEvent<T>> consumerRecord) {
    var key = consumerRecord.key();
    var event = consumerRecord.value();
    event.setRecordId(UUID.fromString(key));
    return event;
  }
}
