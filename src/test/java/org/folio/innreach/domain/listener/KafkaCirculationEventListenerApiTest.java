package org.folio.innreach.domain.listener;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.AFTER_TEST_METHOD;
import static org.springframework.test.context.jdbc.SqlMergeMode.MergeMode.MERGE;

import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlMergeMode;

import org.folio.innreach.domain.event.DomainEvent;
import org.folio.innreach.domain.event.DomainEventType;
import org.folio.innreach.domain.event.EntityChangedData;
import org.folio.innreach.domain.listener.base.BaseKafkaApiTest;
import org.folio.innreach.domain.service.impl.BatchDomainEventProcessor;
import org.folio.innreach.dto.LoanDTO;
import org.folio.innreach.repository.InnReachTransactionRepository;

@Sql(
  scripts = {"classpath:db/inn-reach-transaction/clear-inn-reach-transaction-tables.sql",
    "classpath:db/central-server/clear-central-server-tables.sql",
  },
  executionPhase = AFTER_TEST_METHOD
)
@SqlMergeMode(MERGE)
class KafkaCirculationEventListenerApiTest extends BaseKafkaApiTest {
  private static final UUID LOAN_ID = UUID.randomUUID();
  private static final UUID PRE_POPULATED_PATRON_ID = UUID.fromString("4154a604-4d5a-4d8e-9160-057fc7b6e6b8");
  private static final UUID PRE_POPULATED_ITEM_ID = UUID.fromString("9a326225-6530-41cc-9399-a61987bfab3c");
  private static final UUID PRE_POPULATED_TRANSACTION_ID = UUID.fromString("0aab1720-14b4-4210-9a19-0d0bf1cd64d3");
  private static final String TEST_TENANT_ID = "testing";
  private static final Duration ASYNC_AWAIT_TIMEOUT = Duration.ofSeconds(15);

  @SpyBean
  private KafkaCirculationEventListener listener;

  @SpyBean
  private BatchDomainEventProcessor eventProcessor;

  @SpyBean
  private InnReachTransactionRepository transactionRepository;

  @Test
  void shouldReceiveLoanEvent() {
    var event = getLoanDomainEvent();

    kafkaTemplate.send(new ProducerRecord(CIRC_LOAN_TOPIC, LOAN_ID.toString(), event));

    ArgumentCaptor<List<ConsumerRecord<String, DomainEvent<LoanDTO>>>> eventsCaptor = ArgumentCaptor.forClass(List.class);

    await().atMost(ASYNC_AWAIT_TIMEOUT).untilAsserted(() ->
      verify(listener).handleLoanEvents(eventsCaptor.capture()));

    var records = eventsCaptor.getValue();
    assertEquals(1, records.size());

    var record = records.get(0);
    assertEquals(LOAN_ID.toString(), record.key());
    assertEquals(event, record.value());
  }

  @Test
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/inn-reach-transaction/pre-populate-inn-reach-transaction.sql"
  })
  void shouldLinkLoanToOpenTransaction() {
    var event = getLoanDomainEvent();
    event.setRecordId(null); // the listener should set this field from event key value

    var consumerRecord = new ConsumerRecord(CIRC_LOAN_TOPIC, 1, 1, LOAN_ID.toString(), event);

    listener.handleLoanEvents(List.of(consumerRecord));

    ArgumentCaptor<List<DomainEvent<LoanDTO>>> eventsCaptor = ArgumentCaptor.forClass(List.class);
    verify(eventProcessor).process(eventsCaptor.capture(), any(Consumer.class));

    var capturedEvents = eventsCaptor.getValue();
    assertEquals(1, capturedEvents.size());
    var capturedEvent = capturedEvents.get(0);
    assertEquals(LOAN_ID, capturedEvent.getRecordId());

    var transaction = transactionRepository.fetchOneById(PRE_POPULATED_TRANSACTION_ID).get();
    assertEquals(LOAN_ID, transaction.getHold().getFolioLoanId());
  }

  private DomainEvent<LoanDTO> getLoanDomainEvent() {
    var loan = new LoanDTO().id(LOAN_ID).userId(PRE_POPULATED_PATRON_ID).itemId(PRE_POPULATED_ITEM_ID);

    return DomainEvent.<LoanDTO>builder()
      .recordId(LOAN_ID)
      .tenant(TEST_TENANT_ID)
      .timestamp(System.currentTimeMillis())
      .type(DomainEventType.CREATED)
      .data(new EntityChangedData<>(null, loan))
      .build();
  }

}
