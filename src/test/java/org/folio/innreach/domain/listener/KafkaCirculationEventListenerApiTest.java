package org.folio.innreach.domain.listener;

import static org.awaitility.Awaitility.await;
import static org.folio.innreach.fixture.InventoryFixture.createInventoryItemDTO;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.AFTER_TEST_METHOD;
import static org.springframework.test.context.jdbc.SqlMergeMode.MergeMode.MERGE;

import static org.folio.innreach.domain.dto.folio.circulation.RequestDTO.RequestStatus.CLOSED_CANCELLED;
import static org.folio.innreach.domain.entity.InnReachTransaction.TransactionState.BORROWING_SITE_CANCEL;
import static org.folio.innreach.domain.entity.InnReachTransaction.TransactionState.CANCEL_REQUEST;
import static org.folio.innreach.domain.entity.InnReachTransaction.TransactionState.CLAIMS_RETURNED;
import static org.folio.innreach.domain.entity.InnReachTransaction.TransactionState.FINAL_CHECKIN;
import static org.folio.innreach.domain.entity.InnReachTransaction.TransactionState.LOCAL_CHECKOUT;
import static org.folio.innreach.domain.entity.InnReachTransaction.TransactionState.ITEM_IN_TRANSIT;
import static org.folio.innreach.domain.entity.InnReachTransaction.TransactionState.ITEM_SHIPPED;
import static org.folio.innreach.domain.entity.InnReachTransaction.TransactionState.RECALL;
import static org.folio.innreach.domain.entity.InnReachTransaction.TransactionState.RETURN_UNCIRCULATED;
import static org.folio.innreach.domain.entity.InnReachTransaction.TransactionState.TRANSFER;
import static org.folio.innreach.dto.ItemStatus.NameEnum.AWAITING_PICKUP;
import static org.folio.innreach.util.DateHelper.toEpochSec;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.folio.innreach.client.InventoryClient;
import org.folio.innreach.domain.service.ContributionActionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlMergeMode;

import org.folio.innreach.client.CirculationClient;
import org.folio.innreach.client.InstanceStorageClient;
import org.folio.innreach.domain.dto.folio.circulation.RequestDTO;
import org.folio.innreach.domain.dto.folio.inventory.InventoryItemDTO;
import org.folio.innreach.domain.entity.InnReachTransaction;
import org.folio.innreach.domain.entity.TransactionHold;
import org.folio.innreach.domain.entity.TransactionItemHold;
import org.folio.innreach.domain.event.DomainEvent;
import org.folio.innreach.domain.event.DomainEventType;
import org.folio.innreach.domain.event.EntityChangedData;
import org.folio.innreach.domain.listener.base.BaseKafkaApiTest;
import org.folio.innreach.domain.service.impl.BatchDomainEventProcessor;
import org.folio.innreach.dto.CheckInDTO;
import org.folio.innreach.dto.Instance;
import org.folio.innreach.dto.LoanStatus;
import org.folio.innreach.dto.StorageLoanDTO;
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
  private static final UUID PRE_POPULATED_LOCAL_LOAN_ID = UUID.fromString("7b43b4bc-3a57-4506-815a-78b01c38a2a1");
  private static final UUID REQUESTER_ID = UUID.randomUUID();
  private static final UUID ITEM_ID = UUID.fromString("8a326225-6530-41cc-9399-a61987bfab3c");
  private static final UUID PRE_POPULATED_LOCAL_ITEM_ID = UUID.fromString("c633da85-8112-4453-af9c-c250e417179d");
  private static final UUID INSTANCE_ID = UUID.fromString("ef32e52c-cd9b-462e-9bf0-65233b7a759c");
  private static final UUID HOLDING_ID = UUID.fromString("55fb31a7-1223-4214-bea6-8e35f1ae40dc");
  private static final UUID REQUEST_ID = UUID.fromString("26278b3a-de32-4deb-b81b-896637b3dbeb");
  private static final UUID PRE_POPULATED_PATRON_ID = UUID.fromString("4154a604-4d5a-4d8e-9160-057fc7b6e6b8");
  private static final UUID PRE_POPULATED_LOCAL_PATRON_ID = UUID.fromString("a8ffe3cb-f682-499d-893b-47ff9efb3803");
  private static final UUID PRE_POPULATED_PATRON_TRANSACTION_ID = UUID.fromString("0aab1720-14b4-4210-9a19-0d0bf1cd64d3");
  private static final UUID PRE_POPULATED_PATRON_TRANSACTION_ITEM_ID = UUID.fromString("9a326225-6530-41cc-9399-a61987bfab3c");
  private static final UUID PRE_POPULATED_PATRON_TRANSACTION_REQUEST_ID = UUID.fromString("ea11eba7-3c0f-4d15-9cca-c8608cd6bc8a");
  private static final UUID PRE_POPULATED_PATRON_TRANSACTION_LOAN_ID = UUID.fromString("fd5109c7-8934-4294-9504-c1a4a4f07c96");
  private static final UUID PRE_POPULATED_ITEM_TRANSACTION_ID = UUID.fromString("ab2393a1-acc4-4849-82ac-8cc0c37339e1");
  private static final UUID PRE_POPULATED_ITEM_TRANSACTION_LOAN_ID = UUID.fromString("06e820e3-71a0-455e-8c73-3963aea677d4");
  private static final UUID PRE_POPULATED_LOCAL_TRANSACTION_ID = UUID.fromString("79b0a1fb-55be-4e55-9d84-01303aaec1ce");
  private static final String TEST_TENANT_ID = "testing";
  private static final String TEST_PATRON_NAME = "patronName2";
  private static final Duration ASYNC_AWAIT_TIMEOUT = Duration.ofSeconds(15);
  private static final Date DUE_DATE = new Date();
  private static final UUID CHECKIN_ID = UUID.randomUUID();

  @SpyBean
  private KafkaCirculationEventListener listener;

  @SpyBean
  private BatchDomainEventProcessor eventProcessor;

  @SpyBean
  private InnReachTransactionRepository transactionRepository;

  @MockBean
  private InnReachExternalService innReachExternalService;

  @MockBean
  private ContributionActionService contributionActionService;

  @MockBean
  private InventoryClient inventoryClient;

  @MockBean
  private CirculationClient circulationClient;

  @MockBean
  private InstanceStorageClient instanceStorageClient;

  @Test
  void shouldReceiveLoanEvent() {
    var event = createLoanDomainEvent(DomainEventType.CREATED);

    kafkaTemplate.send(new ProducerRecord(CIRC_LOAN_TOPIC, LOAN_ID.toString(), event));

    ArgumentCaptor<List<ConsumerRecord<String, DomainEvent<StorageLoanDTO>>>> eventsCaptor = ArgumentCaptor.forClass(List.class);

    await().atMost(ASYNC_AWAIT_TIMEOUT).untilAsserted(() ->
      verify(listener).handleLoanEvents(eventsCaptor.capture()));

    var records = eventsCaptor.getValue();
    assertEquals(1, records.size());

    var record = records.get(0);
    assertEquals(LOAN_ID.toString(), record.key());
    assertEquals(event, record.value());
  }

  @Test
  void shouldReceiveRequestEvent() {
    var event = createRequestDomainEvent(DomainEventType.CREATED);

    kafkaTemplate.send(new ProducerRecord(CIRC_REQUEST_TOPIC, REQUEST_ID.toString(), event));

    ArgumentCaptor<List<ConsumerRecord<String, DomainEvent<RequestDTO>>>> eventsCaptor = ArgumentCaptor.forClass(List.class);

    await().atMost(ASYNC_AWAIT_TIMEOUT).untilAsserted(() ->
      verify(listener).handleRequestEvents(eventsCaptor.capture()));

    var records = eventsCaptor.getValue();
    assertEquals(1, records.size());

    var record = records.get(0);
    assertEquals(REQUEST_ID.toString(), record.key());
    assertEquals(event, record.value());
  }

  @Test
  void shouldReceiveCheckInEvent() {
    var event = createCheckInDomainEvent(DomainEventType.CREATED);

    kafkaTemplate.send(new ProducerRecord(CIRC_CHECKIN_TOPIC, CHECKIN_ID.toString(), event));

    ArgumentCaptor<List<ConsumerRecord<String, DomainEvent<CheckInDTO>>>> eventsCaptor = ArgumentCaptor.forClass(List.class);

    await().atMost(ASYNC_AWAIT_TIMEOUT).untilAsserted(() ->
      verify(listener).handleCheckInEvents(eventsCaptor.capture()));

    var records = eventsCaptor.getValue();
    assertEquals(1, records.size());

    var record = records.get(0);
    assertEquals(CHECKIN_ID.toString(), record.key());
    assertEquals(event, record.value());
  }

  @Test
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/inn-reach-transaction/pre-populate-inn-reach-transaction.sql"
  })
  void shouldLinkLoanToPatronTransaction() {
    var event = createLoanDomainEvent(DomainEventType.CREATED);

    listener.handleLoanEvents(asSingleConsumerRecord(CIRC_LOAN_TOPIC, LOAN_ID, event));

    verify(eventProcessor).process(anyList(), any(Consumer.class));

    var updatedTransaction = transactionRepository.fetchOneById(PRE_POPULATED_PATRON_TRANSACTION_ID).orElseThrow();
    assertEquals(LOAN_ID, updatedTransaction.getHold().getFolioLoanId());
    assertEquals((int) DUE_DATE.toInstant().getEpochSecond(), updatedTransaction.getHold().getDueDateTime());
  }

  @Test
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/inn-reach-transaction/pre-populate-inn-reach-transaction.sql",
  })
  void shouldLinkLoanToLocalTransaction() {
    var hrid = "12343435";
    var barcode = "4820049490886";
    var item = new InventoryItemDTO();
    item.setHrid(hrid);
    item.setBarcode(barcode);
    var event = createLoanDomainEvent(DomainEventType.CREATED);
    var storageLoanDTO = event.getData().getNewEntity();
    storageLoanDTO.setId(PRE_POPULATED_LOCAL_LOAN_ID);
    storageLoanDTO.setItemId(PRE_POPULATED_LOCAL_ITEM_ID);
    storageLoanDTO.setUserId(PRE_POPULATED_LOCAL_PATRON_ID);

    when(inventoryClient.findItem(PRE_POPULATED_LOCAL_ITEM_ID)).thenReturn(Optional.of(item));
    listener.handleLoanEvents(asSingleConsumerRecord(CIRC_LOAN_TOPIC, PRE_POPULATED_LOCAL_LOAN_ID, event));

    verify(eventProcessor).process(anyList(), any(Consumer.class));
    verify(innReachExternalService).postInnReachApi(any(), any(), any());
    var updatedTransaction = transactionRepository.fetchOneById(PRE_POPULATED_LOCAL_TRANSACTION_ID).orElseThrow();
    assertEquals(LOCAL_CHECKOUT, updatedTransaction.getState());

    assertPatronAndItemInfoCleared(updatedTransaction.getHold());
  }

  @Test
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/inn-reach-transaction/pre-populate-inn-reach-transaction.sql"
  })
  void shouldUpdatePatronTransactionOnLoanClosure() {
    var folioLoanId = PRE_POPULATED_PATRON_TRANSACTION_LOAN_ID;
    var event = createLoanDomainEvent(DomainEventType.UPDATED);
    var loan = event.getData().getNewEntity();
    loan.setId(folioLoanId);
    loan.setStatus(new LoanStatus().name("Closed"));
    loan.setAction("checkedin");

    listener.handleLoanEvents(asSingleConsumerRecord(CIRC_LOAN_TOPIC, folioLoanId, event));

    verify(eventProcessor).process(anyList(), any(Consumer.class));
    verify(innReachExternalService).postInnReachApi(any(), any());

    var updatedTransaction = transactionRepository.fetchOneById(PRE_POPULATED_PATRON_TRANSACTION_ID).orElseThrow();
    assertEquals(ITEM_IN_TRANSIT, updatedTransaction.getState());
    assertNull(updatedTransaction.getHold().getDueDateTime());
  }

  @Test
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/inn-reach-transaction/pre-populate-inn-reach-transaction.sql",
  })
  void shouldUpdatePatronTransactionOnLoanRenewal() {
    var folioLoanId = PRE_POPULATED_PATRON_TRANSACTION_LOAN_ID;
    var event = createLoanDomainEvent(DomainEventType.UPDATED);
    var loan = event.getData().getNewEntity();
    loan.setId(folioLoanId);
    loan.setAction("renewed");

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
  void shouldUpdatePatronTransactionOnLoanClaimsReturned() {
    var event = createLoanDomainEvent(DomainEventType.UPDATED);
    var claimedReturnedDate = new Date();
    var loan = event.getData().getNewEntity();
    loan.setId(PRE_POPULATED_PATRON_TRANSACTION_LOAN_ID);
    loan.setClaimedReturnedDate(claimedReturnedDate);
    loan.setAction("claimedReturned");

    listener.handleLoanEvents(asSingleConsumerRecord(CIRC_LOAN_TOPIC, PRE_POPULATED_PATRON_TRANSACTION_LOAN_ID, event));

    ArgumentCaptor<Map<Object, Object>> payloadCaptor = ArgumentCaptor.forClass(Map.class);

    verify(eventProcessor).process(anyList(), any(Consumer.class));
    verify(innReachExternalService).postInnReachApi(any(), any(), payloadCaptor.capture());

    var payload = payloadCaptor.getValue();
    var updatedTransaction = transactionRepository.fetchOneById(PRE_POPULATED_PATRON_TRANSACTION_ID).orElseThrow();
    assertEquals(CLAIMS_RETURNED, updatedTransaction.getState());

    var itemHold = updatedTransaction.getHold();
    assertPatronAndItemInfoCleared(itemHold);

    assertEquals(toEpochSec(claimedReturnedDate), payload.get("claimsReturnedDate"));
  }

  @Test
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/inn-reach-transaction/pre-populate-inn-reach-transaction.sql",
  })
  void shouldUpdateItemTransactionOnLoanClosure() {
    var folioLoanId = PRE_POPULATED_ITEM_TRANSACTION_LOAN_ID;
    var transactionId = PRE_POPULATED_ITEM_TRANSACTION_ID;
    var event = createLoanDomainEvent(DomainEventType.UPDATED);
    var loan = event.getData().getNewEntity();
    loan.setId(folioLoanId);
    loan.setAction("checkedin");
    loan.setStatus(new LoanStatus().name("Closed"));

    listener.handleLoanEvents(asSingleConsumerRecord(CIRC_LOAN_TOPIC, folioLoanId, event));

    verify(eventProcessor).process(anyList(), any(Consumer.class));

    var updatedTransaction = transactionRepository.fetchOneById(transactionId).orElse(null);
    var updatedHold = (TransactionItemHold) updatedTransaction.getHold();

    assertNull(updatedHold.getPatronName());
    assertNull(updatedHold.getPatronId());
    assertNull(updatedTransaction.getHold().getDueDateTime());
    assertEquals(FINAL_CHECKIN, updatedTransaction.getState());
  }

  @Test
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/inn-reach-transaction/pre-populate-another-inn-reach-transaction.sql",
  })
  void shouldUpdateItemTransactionOnRequestMoving() {
    var barcode = "4820049490886";
    var item = new InventoryItemDTO();
    item.setBarcode(barcode);
    var event = createRequestDomainEvent(DomainEventType.UPDATED);

    when(inventoryClient.findItem(any())).thenReturn(Optional.of(item));

    listener.handleRequestEvents(asSingleConsumerRecord(CIRC_REQUEST_TOPIC, REQUEST_ID, event));

    verify(eventProcessor).process(anyList(), any(Consumer.class));
    verify(innReachExternalService, times(1)).postInnReachApi(any(), any(), any());

    var updatedTransaction = transactionRepository.fetchActiveByRequestId(REQUEST_ID).orElse(null);
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
    var folioRequestId = PRE_POPULATED_PATRON_TRANSACTION_REQUEST_ID;
    var event = createRequestDomainEvent(DomainEventType.UPDATED);
    var request = event.getData().getNewEntity();
    request.setId(folioRequestId);

    listener.handleRequestEvents(asSingleConsumerRecord(CIRC_REQUEST_TOPIC, folioRequestId, event));

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
    var event = createRequestDomainEvent(DomainEventType.UPDATED);
    var request = event.getData().getNewEntity();
    request.setId(folioRequestId);

    listener.handleRequestEvents(asSingleConsumerRecord(CIRC_REQUEST_TOPIC, folioRequestId, event));

    verify(eventProcessor).process(anyList(), any(Consumer.class));

    verify(transactionRepository, times(1)).fetchActiveByRequestId(any());
    verify(inventoryClient, times(0)).findItem(any());
    verify(innReachExternalService, times(0)).postInnReachApi(any(), any(), any());
  }

  @Test
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/inn-reach-transaction/pre-populate-inn-reach-transaction.sql"
  })
  void shouldUpdateTransactionWithItemHoldWhenCancelRequest() {
    var event = createRequestDomainEvent(DomainEventType.UPDATED);
    var request = event.getData().getNewEntity();
    var instance = new Instance();
    request.setId(REQUEST_ID);
    request.setInstanceId(INSTANCE_ID);
    request.setStatus(CLOSED_CANCELLED);

    var payload = new HashMap<>();
    payload.put("localBibId", instance.getHrid());
    payload.put("reasonCode", 7);
    payload.put("patronName", TEST_PATRON_NAME);

    when(instanceStorageClient.getInstanceById(request.getInstanceId())).thenReturn(instance);

    listener.handleRequestEvents(asSingleConsumerRecord(CIRC_REQUEST_TOPIC, REQUEST_ID, event));

    verify(eventProcessor).process(anyList(), any(Consumer.class));
    verify(instanceStorageClient, times(1)).getInstanceById(any());
    verify(innReachExternalService, times(1)).postInnReachApi(any(), any(), eq(payload));
    verify(innReachExternalService, times(1)).postInnReachApi(any(), any(), any());
    Mockito.verifyNoMoreInteractions(inventoryClient);

    var updatedTransaction = transactionRepository.fetchOneById(PRE_POPULATED_ITEM_TRANSACTION_ID).orElse(null);
    assertEquals(CANCEL_REQUEST, updatedTransaction.getState());
    assertNull(updatedTransaction.getHold().getPatronId());
    assertNull(updatedTransaction.getHold().getPatronName());
  }

  @Test
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/inn-reach-transaction/pre-populate-inn-reach-transaction.sql"
  })
  void shouldUpdateTransactionWithPatronHoldWhenCancelRequest() {
    var event = createRequestDomainEvent(DomainEventType.UPDATED);
    var request = event.getData().getNewEntity();
    request.setId(PRE_POPULATED_PATRON_TRANSACTION_REQUEST_ID);
    request.setStatus(CLOSED_CANCELLED);
    var inventoryItemDTO = createInventoryItemDTO();
    inventoryItemDTO.setId(ITEM_ID);

    when(inventoryClient.findItem(ITEM_ID)).thenReturn(Optional.of(inventoryItemDTO));

    listener.handleRequestEvents(asSingleConsumerRecord(CIRC_REQUEST_TOPIC, PRE_POPULATED_PATRON_TRANSACTION_REQUEST_ID, event));

    verify(eventProcessor).process(anyList(), any(Consumer.class));
    verify(innReachExternalService, times(1)).postInnReachApi(any(), any());
    verify(inventoryClient, times(1)).findItem(any());
    verify(inventoryClient).updateItem(eq(ITEM_ID), argThat(i -> i.getBarcode() == null));

    var updatedTransaction = transactionRepository.fetchOneById(PRE_POPULATED_PATRON_TRANSACTION_ID).orElse(null);
    assertEquals(BORROWING_SITE_CANCEL, updatedTransaction.getState());
    assertPatronAndItemInfoCleared(updatedTransaction.getHold());
  }

  @Test
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/inn-reach-transaction/pre-populate-inn-reach-transaction.sql"
  })
  void shouldNotUpdateTransactionWithPatronHoldWhenCancelRequestAndWrongTransactionState() {
    var transaction = transactionRepository.fetchOneById(PRE_POPULATED_PATRON_TRANSACTION_ID).get();
    var state = ITEM_SHIPPED;
    transaction.setState(state);
    transactionRepository.save(transaction);

    var event = createRequestDomainEvent(DomainEventType.UPDATED);
    var request = event.getData().getNewEntity();
    request.setId(PRE_POPULATED_PATRON_TRANSACTION_REQUEST_ID);
    request.setStatus(CLOSED_CANCELLED);

    listener.handleRequestEvents(asSingleConsumerRecord(CIRC_REQUEST_TOPIC, PRE_POPULATED_PATRON_TRANSACTION_REQUEST_ID, event));

    verify(eventProcessor).process(anyList(), any(Consumer.class));
    verify(innReachExternalService, never()).postInnReachApi(any(), any());

    var updatedTransaction = transactionRepository.fetchOneById(PRE_POPULATED_PATRON_TRANSACTION_ID).orElse(null);
    assertEquals(state, updatedTransaction.getState());
  }

  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/inn-reach-transaction/pre-populate-inn-reach-transaction.sql"
  })
  @ParameterizedTest
  @EnumSource(names = {"CLOSED_PICKUP_EXPIRED", "CLOSED_CANCELLED"})
  void shouldUpdatePatronTransactionOnCheckinCreation(RequestDTO.RequestStatus requestStatus) {
    var checkInId = CHECKIN_ID;
    var event = createCheckInDomainEvent(DomainEventType.CREATED);
    event.getData().getNewEntity().setItemId(PRE_POPULATED_PATRON_TRANSACTION_ITEM_ID);
    var request = new RequestDTO();
    request.setId(PRE_POPULATED_PATRON_TRANSACTION_REQUEST_ID);
    request.setStatus(requestStatus);

    when(circulationClient.findRequest(any())).thenReturn(Optional.of(request));

    listener.handleCheckInEvents(asSingleConsumerRecord(CIRC_LOAN_TOPIC, checkInId, event));

    verify(eventProcessor).process(anyList(), any(Consumer.class));
    verify(innReachExternalService).postInnReachApi(any(), any());

    var updatedTransaction = transactionRepository.fetchOneById(PRE_POPULATED_PATRON_TRANSACTION_ID).orElseThrow();
    assertEquals(RETURN_UNCIRCULATED, updatedTransaction.getState());
  }

  @Test
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/inn-reach-transaction/pre-populate-inn-reach-transaction.sql",
  })
  void shouldUpdateItemTransactionOnLoanRecallRequested() {
    var event = createLoanDomainEvent(DomainEventType.UPDATED);
    var loan = event.getData().getNewEntity();
    loan.setId(PRE_POPULATED_ITEM_TRANSACTION_LOAN_ID);
    loan.setAction("recallrequested");

    listener.handleLoanEvents(asSingleConsumerRecord(CIRC_LOAN_TOPIC, PRE_POPULATED_ITEM_TRANSACTION_LOAN_ID, event));

    ArgumentCaptor<Map<Object, Object>> payloadCaptor = ArgumentCaptor.forClass(Map.class);

    verify(eventProcessor).process(anyList(), any(Consumer.class));
    verify(innReachExternalService).postInnReachApi(any(), any(), payloadCaptor.capture());

    var payload = payloadCaptor.getValue();
    var updatedTransaction = transactionRepository.fetchOneById(PRE_POPULATED_ITEM_TRANSACTION_ID).orElseThrow();
    assertEquals(RECALL, updatedTransaction.getState());
    assertEquals((long) toEpochSec(loan.getDueDate()), payload.get("dueDateTime"));
  }

  private static DomainEvent<StorageLoanDTO> createLoanDomainEvent(DomainEventType eventType) {
    var loan = new StorageLoanDTO().id(LOAN_ID)
      .dueDate(DUE_DATE)
      .userId(PRE_POPULATED_PATRON_ID)
      .itemId(PRE_POPULATED_PATRON_TRANSACTION_ITEM_ID);

    return DomainEvent.<StorageLoanDTO>builder()
      .tenant(TEST_TENANT_ID)
      .timestamp(System.currentTimeMillis())
      .type(eventType)
      .data(new EntityChangedData<>(null, loan))
      .build();
  }

  private static DomainEvent<RequestDTO> createRequestDomainEvent(DomainEventType eventType) {
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

  private static DomainEvent<CheckInDTO> createCheckInDomainEvent(DomainEventType eventType) {
    var checkIn = new CheckInDTO()
      .itemStatusPriorToCheckIn(AWAITING_PICKUP.getValue())
      .itemId(CHECKIN_ID);

    return DomainEvent.<CheckInDTO>builder()
      .tenant(TEST_TENANT_ID)
      .timestamp(System.currentTimeMillis())
      .type(eventType)
      .data(new EntityChangedData<>(null, checkIn))
      .build();
  }

  private static void assertPatronAndItemInfoCleared(TransactionHold itemHold) {
    assertNull(itemHold.getPatronId());
    assertNull(itemHold.getPatronName());
    assertNull(itemHold.getFolioPatronId());
    assertNull(itemHold.getFolioPatronBarcode());
    assertNull(itemHold.getFolioItemId());
    assertNull(itemHold.getFolioHoldingId());
    assertNull(itemHold.getFolioInstanceId());
    assertNull(itemHold.getFolioRequestId());
    assertNull(itemHold.getFolioLoanId());
    assertNull(itemHold.getFolioItemBarcode());
  }

}
