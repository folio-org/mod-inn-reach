package org.folio.innreach.domain.listener;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.AFTER_TEST_METHOD;
import static org.springframework.test.context.jdbc.SqlMergeMode.MergeMode.MERGE;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlMergeMode;

import org.folio.innreach.domain.event.DomainEvent;
import org.folio.innreach.domain.event.DomainEventType;
import org.folio.innreach.domain.event.EntityChangedData;
import org.folio.innreach.domain.listener.base.KafkaTest;
import org.folio.innreach.domain.service.InnReachTransactionActionService;
import org.folio.innreach.domain.service.impl.TenantScopedExecutionService;
import org.folio.innreach.dto.LoanDTO;
import org.folio.innreach.repository.InnReachTransactionRepository;

@Sql(
  scripts = {"classpath:db/inn-reach-transaction/clear-inn-reach-transaction-tables.sql",
    "classpath:db/central-server/clear-central-server-tables.sql",
    "classpath:db/mtype-mapping/clear-material-type-mapping-table.sql",
    "classpath:db/central-patron-type-mapping/clear-central-patron-type-mapping-table.sql"
  },
  executionPhase = AFTER_TEST_METHOD
)
@SqlMergeMode(MERGE)
class KafkaCirculationEventListenerIT extends KafkaTest {
  private static final UUID LOAN_ID = UUID.randomUUID();
  private static final UUID PRE_POPULATED_PATRON_ID = UUID.fromString("4154a604-4d5a-4d8e-9160-057fc7b6e6b8");
  private static final UUID PRE_POPULATED_ITEM_ID = UUID.fromString("9a326225-6530-41cc-9399-a61987bfab3c");
  private static final String TEST_TENANT_ID = "testing";
  private static final Duration ASYNC_AWAIT_TIMEOUT = Duration.ofSeconds(15);

  @SpyBean
  private InnReachTransactionRepository transactionRepository;

  @SpyBean
  private KafkaCirculationEventListener listener;

  @SpyBean
  private TenantScopedExecutionService tenantScopedExecutionService;

  @SpyBean
  private InnReachTransactionActionService transactionActionService;

  @Test
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/inn-reach-transaction/pre-populate-inn-reach-transaction.sql"
  })
  void shouldHandleLoanEvent() {
    var loan = new LoanDTO().id(LOAN_ID).userId(PRE_POPULATED_PATRON_ID).itemId(PRE_POPULATED_ITEM_ID);
    var event = DomainEvent.builder()
      .recordId(LOAN_ID)
      .tenant(TEST_TENANT_ID)
      .timestamp(System.currentTimeMillis())
      .type(DomainEventType.CREATED)
      .data(new EntityChangedData<>(null, loan))
      .build();

    kafkaTemplate.send(new ProducerRecord(CIRC_LOAN_TOPIC, LOAN_ID.toString(), event));

    ArgumentCaptor<List<ConsumerRecord<String, DomainEvent<LoanDTO>>>> eventsCaptor = ArgumentCaptor.forClass(List.class);

    await().atMost(ASYNC_AWAIT_TIMEOUT).untilAsserted(() ->
      Mockito.verify(listener).handleLoanEvents(eventsCaptor.capture()));

    var records = eventsCaptor.getValue();
    assertEquals(1, records.size());

    var record = records.get(0);
    assertEquals(LOAN_ID.toString(), record.key());
    assertEquals(event, record.value());
  }

}
