package org.folio.innreach.domain.listener;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.AFTER_TEST_METHOD;
import static org.springframework.test.context.jdbc.SqlMergeMode.MergeMode.MERGE;

import static org.folio.innreach.domain.entity.InnReachTransaction.TransactionState.FINAL_CHECKIN;
import static org.folio.innreach.domain.entity.InnReachTransaction.TransactionState.ITEM_IN_TRANSIT;
import static org.folio.innreach.domain.entity.InnReachTransaction.TransactionState.RETURN_UNCIRCULATED;
import static org.folio.innreach.domain.entity.InnReachTransaction.TransactionState.TRANSFER;
import static org.folio.innreach.dto.ItemStatus.NameEnum.AWAITING_PICKUP;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlMergeMode;

import org.folio.innreach.domain.dto.folio.circulation.RequestDTO;
import org.folio.innreach.domain.dto.folio.inventory.InventoryItemDTO;
import org.folio.innreach.domain.entity.InnReachTransaction;
import org.folio.innreach.domain.entity.TransactionItemHold;
import org.folio.innreach.domain.event.DomainEvent;
import org.folio.innreach.domain.event.DomainEventType;
import org.folio.innreach.domain.event.EntityChangedData;
import org.folio.innreach.domain.listener.base.BaseKafkaApiTest;
import org.folio.innreach.domain.service.ItemService;
import org.folio.innreach.domain.service.RequestService;
import org.folio.innreach.domain.service.impl.BatchDomainEventProcessor;
import org.folio.innreach.dto.CheckInDTO;
import org.folio.innreach.dto.LoanDTO;
import org.folio.innreach.dto.LoanStatus;
import org.folio.innreach.external.service.InnReachExternalService;
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
  private static final UUID REQUESTER_ID = UUID.randomUUID();
  private static final UUID ITEM_ID = UUID.fromString("8a326225-6530-41cc-9399-a61987bfab3c");
  private static final UUID INSTANCE_ID = UUID.fromString("ef32e52c-cd9b-462e-9bf0-65233b7a759c");
  private static final UUID HOLDING_ID = UUID.fromString("55fb31a7-1223-4214-bea6-8e35f1ae40dc");
  private static final UUID REQUEST_ID = UUID.randomUUID();
  private static final UUID PRE_POPULATED_PATRON_ID = UUID.fromString("4154a604-4d5a-4d8e-9160-057fc7b6e6b8");
  private static final UUID PRE_POPULATED_PATRON_TRANSACTION_ITEM_ID = UUID.fromString("9a326225-6530-41cc-9399-a61987bfab3c");
  private static final UUID PRE_POPULATED_TRANSACTION_ID = UUID.fromString("0aab1720-14b4-4210-9a19-0d0bf1cd64d3");
  private static final UUID PRE_POPULATED_PATRON_TRANSACTION_REQUEST_ID = UUID.fromString("ea11eba7-3c0f-4d15-9cca-c8608cd6bc8a");
  private static final String TEST_TENANT_ID = "testing";
  private static final Duration ASYNC_AWAIT_TIMEOUT = Duration.ofSeconds(15);
  private static final Date DUE_DATE = new Date();

  @SpyBean
  private KafkaCirculationEventListener listener;

  @SpyBean
  private BatchDomainEventProcessor eventProcessor;

  @SpyBean
  private InnReachTransactionRepository transactionRepository;

  @MockBean
  private InnReachExternalService innReachExternalService;

  @MockBean
  private ItemService itemService;

  @MockBean
  private RequestService requestService;

  @Test
  void shouldReceiveLoanEvent() {
    var event = createLoanDomainEvent(DomainEventType.CREATED);

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
    var event = createLoanDomainEvent(DomainEventType.CREATED);

    listener.handleLoanEvents(asSingleConsumerRecord(CIRC_LOAN_TOPIC, LOAN_ID, event));

    verify(eventProcessor).process(anyList(), any(Consumer.class));

    var updatedTransaction = transactionRepository.fetchOneById(PRE_POPULATED_TRANSACTION_ID).get();
    assertEquals(LOAN_ID, updatedTransaction.getHold().getFolioLoanId());
    assertEquals((int) DUE_DATE.toInstant().getEpochSecond(), updatedTransaction.getHold().getDueDateTime());
  }

  @Test
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/inn-reach-transaction/pre-populate-inn-reach-transaction.sql"
  })
  void shouldReturnCheckedInItem() {
    var folioLoanId = UUID.fromString("fd5109c7-8934-4294-9504-c1a4a4f07c96");
    var event = createLoanDomainEvent(DomainEventType.UPDATED);
    var loan = event.getData().getNewEntity();
    loan.setId(folioLoanId);
    loan.setStatus(new LoanStatus().name("Closed"));
    loan.setAction("checkedin");

    listener.handleLoanEvents(asSingleConsumerRecord(CIRC_LOAN_TOPIC, folioLoanId, event));

    verify(eventProcessor).process(anyList(), any(Consumer.class));
    verify(innReachExternalService).postInnReachApi(any(), any());

    var updatedTransaction = transactionRepository.fetchOneById(PRE_POPULATED_TRANSACTION_ID).get();
    assertEquals(ITEM_IN_TRANSIT, updatedTransaction.getState());
    assertNull(updatedTransaction.getHold().getDueDateTime());
  }

  @Test
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/inn-reach-transaction/pre-populate-inn-reach-transaction.sql",
  })
  void shouldRenewalLoanToUpdateTransaction() {
    var folioLoanId = UUID.fromString("fd5109c7-8934-4294-9504-c1a4a4f07c96");
    var event = createLoanDomainEvent(DomainEventType.UPDATED);
    var loan = event.getData().getNewEntity();
    loan.setId(folioLoanId);
    loan.setAction("renewed");

    when(innReachExternalService.postInnReachApi(any(), any(), any())).thenReturn("ok");

    listener.handleLoanEvents(asSingleConsumerRecord(CIRC_LOAN_TOPIC, folioLoanId, event));

    verify(eventProcessor).process(anyList(), any(Consumer.class));

    var updatedTransaction = transactionRepository.fetchActiveByLoanId(folioLoanId).orElse(null);

    assertEquals(InnReachTransaction.TransactionState.BORROWER_RENEW, updatedTransaction.getState());
    var loanDueDate = loan.getDueDate().toInstant().truncatedTo(ChronoUnit.SECONDS);
    var loanIntegerDueDate = (int) (loanDueDate.getEpochSecond());
    assertEquals(loanIntegerDueDate, updatedTransaction.getHold().getDueDateTime());
  }

  @Test
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/inn-reach-transaction/pre-populate-inn-reach-transaction.sql",
  })
  void shouldUpdateTransactionToCheckIn() {
    var folioLoanId = UUID.fromString("06e820e3-71a0-455e-8c73-3963aea677d4");
    var transactionId = UUID.fromString("ab2393a1-acc4-4849-82ac-8cc0c37339e1");
    var event = createLoanDomainEvent(DomainEventType.UPDATED);
    var loan = event.getData().getNewEntity();
    loan.setId(folioLoanId);
    loan.setAction("checkedin");
    loan.setStatus(new LoanStatus().name("Closed"));

    when(innReachExternalService.postInnReachApi(any(), any(), any())).thenReturn("ok");

    listener.handleLoanEvents(asSingleConsumerRecord(CIRC_LOAN_TOPIC, folioLoanId, event));

    verify(eventProcessor).process(anyList(), any(Consumer.class));

    var updatedTransaction = transactionRepository.fetchOneById(transactionId).orElse(null);
    var updatedHold = (TransactionItemHold) updatedTransaction.getHold();

    assertEquals(null, updatedHold.getPatronName());
    assertEquals(null, updatedHold.getPatronId());
    assertEquals(null, updatedTransaction.getHold().getDueDateTime());
    assertEquals(FINAL_CHECKIN, updatedTransaction.getState());
  }

  @Test
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/inn-reach-transaction/pre-populate-another-inn-reach-transaction.sql",
  })
  void shouldUpdateTransactionToTransfer() {
    var folioRequestId = UUID.fromString("26278b3a-de32-4deb-b81b-896637b3dbeb");
    var barcode = "4820049490886";
    var item = new InventoryItemDTO();
    item.setBarcode(barcode);
    var event = getRequestDomainEvent(DomainEventType.UPDATED);
    var request = event.getData().getNewEntity();
    request.setId(folioRequestId);

    when(itemService.find(any())).thenReturn(Optional.of(item));
    when(innReachExternalService.postInnReachApi(any(), any(), any())).thenReturn("ok");

    listener.handleRequestStorage(asSingleConsumerRecord(CIRC_REQUEST_TOPIC, folioRequestId, event));

    verify(eventProcessor).process(anyList(), any(Consumer.class));
    verify(innReachExternalService, times(1)).postInnReachApi(any(), any(), any());

    var updatedTransaction = transactionRepository.fetchActiveByRequestId(request.getId()).orElse(null);
    var updatedHold = updatedTransaction.getHold();

    assertEquals(ITEM_ID, updatedHold.getFolioItemId());
    assertEquals(INSTANCE_ID, updatedHold.getFolioInstanceId());
    assertEquals(HOLDING_ID, updatedHold.getFolioHoldingId());
    assertEquals(barcode, updatedHold.getFolioItemBarcode());
    assertEquals(TRANSFER, updatedTransaction.getState());
  }

  @Test
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/inn-reach-transaction/pre-populate-another-inn-reach-transaction.sql",
  })
  void shouldSkipTransactionThatIsNotItemHold() {
    var folioRequestId = UUID.fromString("ea11eba7-3c0f-4d15-9cca-c8608cd6bc8a");
    var event = getRequestDomainEvent(DomainEventType.UPDATED);
    var request = event.getData().getNewEntity();
    request.setId(folioRequestId);

    listener.handleRequestStorage(asSingleConsumerRecord(CIRC_REQUEST_TOPIC, folioRequestId, event));

    verify(eventProcessor).process(anyList(), any(Consumer.class));

    var transaction = transactionRepository.fetchActiveByRequestId(request.getId()).orElse(null);
    assertEquals(InnReachTransaction.TransactionState.PATRON_HOLD, transaction.getState());
  }

  @Test
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/inn-reach-transaction/pre-populate-another-inn-reach-transaction.sql"
  })
  void shouldSkipIfTransactionNotFoundByRequestId() {
    var folioRequestId = UUID.fromString("aa11eba7-3c0f-4d15-9cca-c8608cd6bc8a");
    var event = getRequestDomainEvent(DomainEventType.UPDATED);

    listener.handleRequestStorage(asSingleConsumerRecord(CIRC_REQUEST_TOPIC, folioRequestId, event));

    verify(eventProcessor).process(anyList(), any(Consumer.class));

    verify(transactionRepository, times(1)).fetchActiveByRequestId(any());
    verify(itemService, times(0)).find(any());
    verify(innReachExternalService, times(0)).postInnReachApi(any(), any(), any());
  }

  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/inn-reach-transaction/pre-populate-inn-reach-transaction.sql"
  })
  @ParameterizedTest
  @EnumSource(names = {"CLOSED_PICKUP_EXPIRED", "CLOSED_CANCELLED"})
  void shouldHandleCheckInEvent(RequestDTO.RequestStatus requestStatus) {
    var checkInId = UUID.randomUUID();
    var event = getCheckInDomainEvent(DomainEventType.CREATED);
    event.getData().getNewEntity().setItemId(PRE_POPULATED_PATRON_TRANSACTION_ITEM_ID);
    var request = new RequestDTO();
    request.setId(PRE_POPULATED_PATRON_TRANSACTION_REQUEST_ID);
    request.setStatus(requestStatus);

    when(requestService.findRequest(any())).thenReturn(request);

    listener.handleCheckInEvents(asSingleConsumerRecord(CIRC_LOAN_TOPIC, checkInId, event));

    verify(eventProcessor).process(anyList(), any(Consumer.class));
    verify(innReachExternalService).postInnReachApi(any(), any());

    var updatedTransaction = transactionRepository.fetchOneById(PRE_POPULATED_TRANSACTION_ID).get();
    assertEquals(RETURN_UNCIRCULATED, updatedTransaction.getState());
  }

  private DomainEvent<LoanDTO> createLoanDomainEvent(DomainEventType eventType) {
    var loan = new LoanDTO().id(LOAN_ID)
      .dueDate(DUE_DATE)
      .userId(PRE_POPULATED_PATRON_ID)
      .itemId(PRE_POPULATED_PATRON_TRANSACTION_ITEM_ID);

    return DomainEvent.<LoanDTO>builder()
      .tenant(TEST_TENANT_ID)
      .timestamp(System.currentTimeMillis())
      .type(eventType)
      .data(new EntityChangedData<>(null, loan))
      .build();
  }

  private DomainEvent<RequestDTO> getRequestDomainEvent(DomainEventType eventType) {
    var request = new RequestDTO();
    request.setId(REQUEST_ID);
    request.setItemId(ITEM_ID);
    request.setRequesterId(REQUESTER_ID);
    request.setInstanceId(INSTANCE_ID);
    request.setHoldingsRecordId(HOLDING_ID);

    return DomainEvent.<RequestDTO>builder()
      .tenant(TEST_TENANT_ID)
      .timestamp(System.currentTimeMillis())
      .type(eventType)
      .data(new EntityChangedData<>(null, request))
      .build();
  }

  private DomainEvent<CheckInDTO> getCheckInDomainEvent(DomainEventType eventType) {
    var checkIn = new CheckInDTO()
      .id(UUID.randomUUID())
      .itemStatusPriorToCheckIn(AWAITING_PICKUP.getValue())
      .itemId(UUID.randomUUID());

    return DomainEvent.<CheckInDTO>builder()
      .tenant(TEST_TENANT_ID)
      .timestamp(System.currentTimeMillis())
      .type(eventType)
      .data(new EntityChangedData<>(null, checkIn))
      .build();
  }
}
