package org.folio.innreach.domain.listener;

import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.kafka.clients.consumer.ConsumerRecord;
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

  public static final Function<ConsumerRecord<String, DomainEvent<LoanDTO>>, DomainEvent<LoanDTO>> CONSUMER_REC_MAPPER =
    consumerRec -> {
      var event = consumerRec.value();
      var recordId = UUID.fromString(consumerRec.key());
      event.setRecordId(recordId);
      return event;
    };

  @KafkaListener(
    id = "mod-inn-reach-circ-loan-listener",
    containerFactory = "kafkaListenerContainerFactory",
    topicPattern = "${kafka.listener.loan.topic-pattern}",
    groupId = "${kafka.listener.loan.group-id}",
    concurrency = "${kafka.listener.loan.concurrency}")
  public void handleLoanEvents(List<ConsumerRecord<String, DomainEvent<LoanDTO>>> consumerRecords) {
    log.info("Handling circulation Loan events from Kafka [number of events: {}]", consumerRecords.size());

    var events = consumerRecords.stream()
      .map(CONSUMER_REC_MAPPER)
      .collect(Collectors.toList());

    eventProcessor.process(events, event -> {
      switch (event.getType()) {
        case CREATED:
          transactionActionService.linkNewLoanToOpenTransaction(event.getData());
          break;
        default:
          log.warn("Received event of unknown type {}", event.getType());
      }
    });
  }
}
