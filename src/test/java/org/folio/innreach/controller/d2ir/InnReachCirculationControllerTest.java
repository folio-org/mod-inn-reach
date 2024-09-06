package org.folio.innreach.controller.d2ir;

import static org.awaitility.Awaitility.await;
import static org.folio.innreach.domain.entity.InnReachTransaction.TransactionState.BORROWER_RENEW;
import static org.folio.innreach.domain.entity.InnReachTransaction.TransactionState.BORROWING_SITE_CANCEL;
import static org.folio.innreach.domain.entity.InnReachTransaction.TransactionState.CANCEL_REQUEST;
import static org.folio.innreach.domain.entity.InnReachTransaction.TransactionState.CLAIMS_RETURNED;
import static org.folio.innreach.domain.entity.InnReachTransaction.TransactionState.FINAL_CHECKIN;
import static org.folio.innreach.domain.entity.InnReachTransaction.TransactionState.ITEM_HOLD;
import static org.folio.innreach.domain.entity.InnReachTransaction.TransactionState.ITEM_IN_TRANSIT;
import static org.folio.innreach.domain.entity.InnReachTransaction.TransactionState.ITEM_RECEIVED;
import static org.folio.innreach.domain.entity.InnReachTransaction.TransactionState.ITEM_SHIPPED;
import static org.folio.innreach.domain.entity.InnReachTransaction.TransactionState.RECALL;
import static org.folio.innreach.domain.entity.InnReachTransaction.TransactionState.RECEIVE_UNANNOUNCED;
import static org.folio.innreach.domain.entity.InnReachTransaction.TransactionState.RETURN_UNCIRCULATED;
import static org.folio.innreach.domain.entity.InnReachTransaction.TransactionState.TRANSFER;
import static org.folio.innreach.fixture.CirculationFixture.createCancelRequestDTO;
import static org.folio.innreach.fixture.CirculationFixture.createClaimsItemReturnedDTO;
import static org.folio.innreach.fixture.CirculationFixture.createItemShippedDTO;
import static org.folio.innreach.fixture.CirculationFixture.createRecallDTO;
import static org.folio.innreach.fixture.CirculationFixture.createTransactionHoldDTO;
import static org.folio.innreach.fixture.InventoryFixture.createInventoryItemDTO;
import static org.folio.innreach.fixture.RequestFixture.createRequestDTO;
import static org.folio.innreach.fixture.ServicePointUserFixture.createServicePointUserDTO;
import static org.folio.innreach.fixture.TestUtil.circHeaders;
import static org.folio.innreach.fixture.TestUtil.deserializeFromJsonFile;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.AFTER_TEST_METHOD;
import static org.springframework.test.context.jdbc.SqlMergeMode.MergeMode.MERGE;

import org.apache.commons.lang3.ObjectUtils;
import org.folio.innreach.batch.contribution.IterationEventReaderFactory;
import org.folio.innreach.client.CirculationClient;
import org.folio.innreach.client.HridSettingsClient;
import org.folio.innreach.client.ServicePointsUsersClient;
import org.folio.innreach.controller.base.BaseControllerTest;
import org.folio.innreach.domain.dto.folio.ResultList;
import org.folio.innreach.domain.dto.folio.User;
import org.folio.innreach.domain.dto.folio.circulation.RequestDTO;
import org.folio.innreach.domain.dto.folio.configuration.ConfigurationDTO;
import org.folio.innreach.domain.entity.InnReachTransaction;
import org.folio.innreach.domain.entity.TransactionPatronHold;
import org.folio.innreach.domain.service.ConfigurationService;
import org.folio.innreach.domain.service.HoldingsService;
import org.folio.innreach.domain.service.InstanceService;
import org.folio.innreach.domain.service.InventoryService;
import org.folio.innreach.domain.service.ItemService;
import org.folio.innreach.domain.service.PatronHoldService;
import org.folio.innreach.domain.service.RequestService;
import org.folio.innreach.domain.service.UserService;
import org.folio.innreach.domain.service.VirtualRecordService;
import org.folio.innreach.dto.CheckOutRequestDTO;
import org.folio.innreach.dto.InnReachResponseDTO;
import org.folio.innreach.dto.LoanDTO;
import org.folio.innreach.dto.RenewLoanDTO;
import org.folio.innreach.external.dto.InnReachResponse;
import org.folio.innreach.external.service.InnReachExternalService;
import org.folio.innreach.mapper.InnReachTransactionHoldMapper;
import org.folio.innreach.repository.InnReachTransactionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.event.annotation.BeforeTestMethod;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlMergeMode;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

@Sql(
  scripts = {
    "classpath:db/central-server/clear-central-server-tables.sql",
    "classpath:db/inn-reach-transaction/clear-inn-reach-transaction-tables.sql",
    "classpath:db/patron-type-mapping/clear-patron-type-mapping-tables.sql",
    "classpath:db/agency-loc-mapping/clear-agency-location-mapping.sql",
    "classpath:db/item-type-mapping/clear-item-type-mapping-tables.sql",
    "classpath:db/inn-reach-recall-user/clear-inn-reach-recall-user.sql"
  },
  executionPhase = AFTER_TEST_METHOD
)
@SqlMergeMode(MERGE)
class InnReachCirculationControllerTest extends BaseControllerTest {

  private static final String ITEM_IN_TRANSIT_ENDPOINT = "/inn-reach/d2ir/circ/intransit/{trackingId}/{centralCode}";
  private static final String CIRCULATION_OPERATION_ENDPOINT = "/inn-reach/d2ir/circ/{circulationOperationName}/{trackingId}/{centralCode}";
  private static final String CANCEL_ITEM_HOLD_PATH = "/inn-reach/d2ir/circ/cancelitemhold/{trackingId}/{centralCode}";
  private static final String ITEM_RECEIVED_PATH = "/inn-reach/d2ir/circ/itemreceived/{trackingId}/{centralCode}";
  private static final String RECALL_REQUEST_PATH = "/inn-reach/d2ir/circ/recall/{trackingId}/{centralCode}";
  private static final String CANCEL_REQUEST_PATH = "/inn-reach/d2ir/circ/cancelrequest/{trackingId}/{centralCode}";

  private static final String PATRON_HOLD_OPERATION = "patronhold";
  private static final String ITEM_SHIPPED_OPERATION = "itemshipped";
  private static final String LOCAL_HOLD_OPERATION = "localhold";

  private static final String UNEXPECTED_TRANSACTION_STATE = "Unexpected transaction state: ";

  private static final UUID NEW_LOAN_ID = UUID.fromString("dc02b484-4217-4207-8b2c-6e7f092b7057");
  private static final UUID PRE_POPULATED_INSTANCE_ID = UUID.fromString("76834d5a-08e8-45ea-84ca-4d9b10aa341c");
  private static final UUID PRE_POPULATED_HOLDINGS_RECORD_ID = UUID.fromString("76834d5a-08e8-45ea-84ca-4d9b10aa342c");
  private static final UUID PRE_POPULATED_ITEM_ID = UUID.fromString("9a326225-6530-41cc-9399-a61987bfab3c");
  private static final UUID PRE_POPULATED_REQUESTER_ID = UUID.fromString("f75ffab1-2e2f-43be-b159-3031e2cfc458");
  private static final UUID PRE_POPULATED_PATRON_ID = UUID.fromString("4154a604-4d5a-4d8e-9160-057fc7b6e6b8");
  private static final UUID PRE_POPULATED_PATRON2_ID = UUID.fromString("a7853dda-520b-4f7a-a1fb-9383665ea770");
  private static final UUID PICK_IP_SERVICE_POINT = UUID.fromString("d08b7bbe-a978-4db8-b5af-a80556254a99");
  private static final UUID PRE_POPULATE_SERVICE_ID = UUID.fromString("74a215e6-e3a1-475d-b7d6-f23b3a5d3c47");
  private static final UUID PRE_POPULATE_PATRON_GROUP_ID = UUID.fromString("54e17c4c-e315-4d20-8879-efc694dea1ce");

  private static final String PRE_POPULATED_TRACKING1_ID = "tracking1";
  private static final String PRE_POPULATED_TRACKING2_ID = "tracking2";
  private static final String NEW_TRANSACTION_TRACKING_ID = "tracking99";
  private static final String PRE_POPULATED_CENTRAL_CODE = "d2ir";
  private static final String PRE_POPULATED_LOCAL_AGENCY_CODE1 = "q1w2e";
  private static final String PRE_POPULATED_LOCAL_AGENCY_CODE2 = "w2e3r";
  private static final String PRE_POPULATED_ANOTHER_LOCAL_AGENCY_CODE1 = "g91ub";
  private static final Integer PRE_POPULATED_CENTRAL_PATRON_TYPE = 1;
  private static final String CENTRAL_PATRON_NAME = "Atreides, Paul";
  private static final Duration ASYNC_AWAIT_TIMEOUT = Duration.ofSeconds(15);

  @Autowired
  private TestRestTemplate testRestTemplate;

  @SpyBean
  private InnReachTransactionRepository repository;

  @MockBean
  private ItemService itemService;
  @MockBean
  private CirculationClient circulationClient;
  @SpyBean
  private RequestService requestService;
  @MockBean
  private ServicePointsUsersClient servicePointsUsersClient;
  @MockBean
  private InnReachExternalService innReachExternalService;
  @MockBean
  private UserService userService;
  @MockBean
  private InventoryService inventoryService;
  @MockBean
  private InstanceService instanceService;
  @MockBean
  private PatronHoldService patronHoldService;
  @MockBean
  private HoldingsService holdingsService;
  @MockBean
  VirtualRecordService virtualRecordService;
  @MockBean
  ConfigurationService configurationService;

  @Autowired
  private InnReachTransactionRepository transactionRepository;
  @Autowired
  private InnReachTransactionHoldMapper transactionHoldMapper;

  @MockBean
  IterationEventReaderFactory iterationEventReaderFactory;

  private HttpHeaders headers = circHeaders();

  @Captor
  ArgumentCaptor<RequestDTO> requestDtoCaptor;

  @Test
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/inn-reach-transaction/pre-populate-inn-reach-transaction.sql",
    "classpath:db/patron-type-mapping/pre-populate-patron-type-mapping.sql",
    "classpath:db/agency-loc-mapping/pre-populate-agency-location-mapping.sql",
    "classpath:db/item-type-mapping/pre-populate-item-type-mapping.sql"
  })
  void processPatronHoldCirculationRequest_createNewPatronHold() {
    var transactionHoldDTO = createTransactionHoldDTO();
    var user = populateUser();

    when(userService.getUserById(any())).thenReturn(Optional.of(user));

    var responseEntity = testRestTemplate.postForEntity(
      CIRCULATION_OPERATION_ENDPOINT,
      new HttpEntity<>(transactionHoldDTO, headers), InnReachResponseDTO.class,
      PATRON_HOLD_OPERATION, NEW_TRANSACTION_TRACKING_ID, PRE_POPULATED_CENTRAL_CODE);

    assertEquals(HttpStatus.OK, responseEntity.getStatusCode());

    var responseBody = responseEntity.getBody();

    assertNotNull(responseBody);
    assertNotNull(responseBody.getErrors());
    assertEquals(0, responseBody.getErrors().size());
    assertEquals(InnReachResponse.OK_STATUS, responseBody.getStatus());

    var innReachTransaction = repository.findByTrackingIdAndCentralServerCode(
      NEW_TRANSACTION_TRACKING_ID, PRE_POPULATED_CENTRAL_CODE).orElse(null);

    assertNotNull(innReachTransaction);
    assertNotNull(innReachTransaction.getHold());
    assertEquals(CENTRAL_PATRON_NAME, innReachTransaction.getHold().getPatronName());
    assertEquals(PRE_POPULATED_CENTRAL_PATRON_TYPE, innReachTransaction.getHold().getCentralPatronType());
  }

  @Test
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/inn-reach-transaction/pre-populate-inn-reach-transaction.sql",
    "classpath:db/patron-type-mapping/pre-populate-patron-type-mapping.sql",
    "classpath:db/agency-loc-mapping/pre-populate-agency-location-mapping.sql",
    "classpath:db/item-type-mapping/pre-populate-item-type-mapping.sql"
  })
  void processPatronHoldCirculationRequest_updateExitingPatronHold() {
    var existing = transactionRepository.findByTrackingIdAndCentralServerCode(
      PRE_POPULATED_TRACKING1_ID, PRE_POPULATED_CENTRAL_CODE).get();
    var transactionHoldDTO = transactionHoldMapper.toPatronHoldDTO((TransactionPatronHold) existing.getHold());
    var user = populateUser();

    when(userService.getUserById(any())).thenReturn(Optional.of(user));
    when(inventoryService.getHridSettings()).thenReturn(new HridSettingsClient.HridSettings());

    var responseEntity = testRestTemplate.postForEntity(
      CIRCULATION_OPERATION_ENDPOINT,
      new HttpEntity<>(transactionHoldDTO, headers), InnReachResponseDTO.class,
      PATRON_HOLD_OPERATION, PRE_POPULATED_TRACKING1_ID, PRE_POPULATED_CENTRAL_CODE);

    assertEquals(HttpStatus.OK, responseEntity.getStatusCode());

    var responseBody = responseEntity.getBody();

    assertNotNull(responseBody);
    assertNotNull(responseBody.getErrors());
    assertEquals(0, responseBody.getErrors().size());
    assertEquals(InnReachResponse.OK_STATUS, responseBody.getStatus());

    var updatedTransaction = repository.findByTrackingIdAndCentralServerCode(
      PRE_POPULATED_TRACKING1_ID, PRE_POPULATED_CENTRAL_CODE);

    assertTrue(updatedTransaction.isPresent());

    var innReachTransaction = updatedTransaction.get();

    assertEquals(transactionHoldDTO.getTransactionTime(), innReachTransaction.getHold().getTransactionTime());
    assertEquals(transactionHoldDTO.getPatronId(), innReachTransaction.getHold().getPatronId());
    assertEquals(transactionHoldDTO.getPatronAgencyCode(), innReachTransaction.getHold().getPatronAgencyCode());
    assertEquals(CENTRAL_PATRON_NAME, innReachTransaction.getHold().getPatronName());
    assertEquals(PRE_POPULATED_CENTRAL_PATRON_TYPE, updatedTransaction.get().getHold().getCentralPatronType());
  }

  @Test
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/inn-reach-transaction/pre-populate-inn-reach-transaction.sql",
    "classpath:db/patron-type-mapping/pre-populate-patron-type-mapping.sql",
    "classpath:db/agency-loc-mapping/pre-populate-agency-location-mapping.sql",
    "classpath:db/item-type-mapping/pre-populate-item-type-mapping.sql"
  })
  void processItemShippedCircRequest_updateFolioItem_whenAssociatedItemExists() {
    var user = populateUser();
    var item = createInventoryItemDTO();

    when(userService.getUserById(any())).thenReturn(Optional.of(user));
    when(itemService.find(any())).thenReturn(Optional.of(item));
    when(itemService.findItemByBarcode(any())).thenReturn(Optional.of(item));
    when(itemService.changeAndUpdate(any(), any())).thenReturn(Optional.of(item));

    var itemShippedDTO = createItemShippedDTO();

    var responseEntity = testRestTemplate.exchange(
      CIRCULATION_OPERATION_ENDPOINT, HttpMethod.PUT,
      new HttpEntity<>(itemShippedDTO, headers), InnReachResponseDTO.class,
      ITEM_SHIPPED_OPERATION, PRE_POPULATED_TRACKING1_ID, PRE_POPULATED_CENTRAL_CODE);

    assertEquals(HttpStatus.OK, responseEntity.getStatusCode());

    var responseEntityBody = responseEntity.getBody();

    assertNotNull(responseEntityBody);
    assertEquals("ok", responseEntityBody.getStatus());
  }

  @Test
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/inn-reach-transaction/pre-populate-inn-reach-transaction.sql"
  })
  void processItemShippedCircRequest_returnFailedStatus_whenAssociatedItemDoesNotExist() {
    when(itemService.findItemByBarcode(any())).thenReturn(Optional.of(createInventoryItemDTO()));
    when(itemService.changeAndUpdate(any(), any(), any())).thenThrow(new IllegalArgumentException("Not found"));

    var transactionHoldDTO = createTransactionHoldDTO();

    var responseEntity = testRestTemplate.exchange(
      CIRCULATION_OPERATION_ENDPOINT, HttpMethod.PUT,
      new HttpEntity<>(transactionHoldDTO, headers), InnReachResponseDTO.class,
      ITEM_SHIPPED_OPERATION, PRE_POPULATED_TRACKING1_ID, PRE_POPULATED_CENTRAL_CODE);

    verify(itemService, times(0)).update(any());

    assertEquals(BAD_REQUEST, responseEntity.getStatusCode());

    var responseEntityBody = responseEntity.getBody();

    assertNotNull(responseEntityBody);
    assertEquals("failed", responseEntityBody.getStatus());
  }

  @Test
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/inn-reach-transaction/pre-populate-inn-reach-transaction.sql"
  })
  void processCancelItemHoldRequest_whenItemIsNotCheckedOut() {
    when(circulationClient.findRequest(any())).thenReturn(Optional.of(new RequestDTO()));
    doNothing().when(circulationClient).updateRequest(any(), any());

    var transaction = fetchTransactionByTrackingId(PRE_POPULATED_TRACKING2_ID);
    transaction.getHold().setFolioLoanId(null);
    repository.save(transaction);

    var transactionHoldDTO = createTransactionHoldDTO();

    var responseEntity = testRestTemplate.exchange(
      CANCEL_ITEM_HOLD_PATH, HttpMethod.PUT, new HttpEntity<>(transactionHoldDTO, headers), InnReachResponseDTO.class,
      PRE_POPULATED_TRACKING2_ID, PRE_POPULATED_CENTRAL_CODE);

    assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
    var responseEntityBody = responseEntity.getBody();
    assertNotNull(responseEntityBody);
    assertEquals("ok", responseEntityBody.getStatus());

    verify(requestService).cancelRequest(anyString(), any(UUID.class), any(UUID.class), eq("Request cancelled at borrowing site"));
    var transactionUpdated = fetchTransactionByTrackingId(PRE_POPULATED_TRACKING2_ID);
    assertEquals(BORROWING_SITE_CANCEL, transactionUpdated.getState());
  }

//  reproducing failed test case 6
  @Test
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/inn-reach-transaction/pre-populate-inn-reach-transaction.sql"
  })
  void processCancelItemHoldRequest_whenItemIsCheckedOut() {
    var transactionHoldDTO = createTransactionHoldDTO();

    var responseEntity = testRestTemplate.exchange(
      CANCEL_ITEM_HOLD_PATH, HttpMethod.PUT, new HttpEntity<>(transactionHoldDTO, headers), InnReachResponseDTO.class,
      PRE_POPULATED_TRACKING2_ID, PRE_POPULATED_CENTRAL_CODE);

    assertEquals(BAD_REQUEST, responseEntity.getStatusCode());
    var responseEntityBody = responseEntity.getBody();
    assertNotNull(responseEntityBody);
    assertEquals("Requested item is already checked out.",
      responseEntityBody.getReason());

    verifyNoInteractions(requestService);
    var transactionUpdated = fetchPrePopulatedTransaction();
    assertNotEquals(BORROWING_SITE_CANCEL, transactionUpdated.getState());
  }

  @Test
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/inn-reach-transaction/pre-populate-inn-reach-transaction.sql"
  })
  void precessReportUnshippedItemReceived_whenTransactionItemHold() {
    var transaction = fetchTransactionByTrackingId(PRE_POPULATED_TRACKING2_ID);
    transaction.setState(ITEM_HOLD);
    repository.save(transaction);

    var transactionHoldDTO = createTransactionHoldDTO();

    when(circulationClient.findRequest(transaction.getHold().getFolioRequestId())).thenReturn(Optional.of(createRequestDTO()));
    when(servicePointsUsersClient.findServicePointsUsers(eq(PRE_POPULATED_PATRON2_ID))).thenReturn(ResultList.asSinglePage(createServicePointUserDTO()));
    when(circulationClient.checkOutByBarcode(any(CheckOutRequestDTO.class))).thenReturn(new LoanDTO().id(NEW_LOAN_ID));

    var responseEntity = testRestTemplate.exchange(
      "/inn-reach/d2ir/circ/receiveunshipped/{trackingId}/{centralCode}", HttpMethod.PUT,
      new HttpEntity<>(transactionHoldDTO, headers), InnReachResponseDTO.class,
      PRE_POPULATED_TRACKING2_ID, PRE_POPULATED_CENTRAL_CODE);

    assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
    var responseEntityBody = responseEntity.getBody();
    assertNotNull(responseEntityBody);
    assertEquals("ok", responseEntityBody.getStatus());

    var transactionUpdated = fetchTransactionByTrackingId(PRE_POPULATED_TRACKING2_ID);
    assertEquals(RECEIVE_UNANNOUNCED, transactionUpdated.getState());
    assertEquals(NEW_LOAN_ID, transactionUpdated.getHold().getFolioLoanId());
  }

  @Test
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/inn-reach-transaction/pre-populate-inn-reach-transaction.sql"
  })
  void precessReportUnshippedItemReceived_whenTransactionItemShipped() {
    var transaction = fetchTransactionByTrackingId(PRE_POPULATED_TRACKING2_ID);
    transaction.setState(ITEM_SHIPPED);
    repository.save(transaction);

    var transactionHoldDTO = createTransactionHoldDTO();

    var responseEntity = testRestTemplate.exchange(
      "/inn-reach/d2ir/circ/receiveunshipped/{trackingId}/{centralCode}", HttpMethod.PUT,
      new HttpEntity<>(transactionHoldDTO, headers), InnReachResponseDTO.class,
      PRE_POPULATED_TRACKING2_ID, PRE_POPULATED_CENTRAL_CODE);

    assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
    var responseEntityBody = responseEntity.getBody();
    assertNotNull(responseEntityBody);
    assertEquals(UNEXPECTED_TRANSACTION_STATE + transaction.getState(),
      responseEntityBody.getReason());

    var transactionUpdated = fetchTransactionByTrackingId(PRE_POPULATED_TRACKING2_ID);
    assertNotEquals(RECEIVE_UNANNOUNCED, transactionUpdated.getState());
  }

  @Test
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/inn-reach-transaction/pre-populate-inn-reach-transaction.sql"
  })
  void processItemInTransit_updateTransactionState() {
    var transaction = fetchPrePopulatedTransaction();
    transaction.setState(ITEM_RECEIVED);
    repository.save(transaction);

    var transactionHoldDTO = createTransactionHoldDTO();

    var responseEntity = testRestTemplate.exchange(ITEM_IN_TRANSIT_ENDPOINT, HttpMethod.PUT,
      new HttpEntity<>(transactionHoldDTO, headers), InnReachResponseDTO.class,
      PRE_POPULATED_TRACKING1_ID, PRE_POPULATED_CENTRAL_CODE);

    assertEquals(OK, responseEntity.getStatusCode());

    var response = responseEntity.getBody();
    assertNotNull(response);
    assertEquals("success", response.getReason());

    var transactionUpdated = fetchPrePopulatedTransaction();
    assertEquals(ITEM_IN_TRANSIT, transactionUpdated.getState());
  }

  @Test
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/inn-reach-transaction/pre-populate-inn-reach-transaction.sql"
  })
  void processItemInTransit_unexpectedTransactionState() {
    var transaction = fetchPrePopulatedTransaction();
    transaction.setState(ITEM_HOLD);
    repository.save(transaction);

    var transactionHoldDTO = createTransactionHoldDTO();

    var responseEntity = testRestTemplate.exchange(ITEM_IN_TRANSIT_ENDPOINT, HttpMethod.PUT,
      new HttpEntity<>(transactionHoldDTO, headers), InnReachResponseDTO.class,
      PRE_POPULATED_TRACKING1_ID, PRE_POPULATED_CENTRAL_CODE);

    assertEquals(BAD_REQUEST, responseEntity.getStatusCode());

    var response = responseEntity.getBody();
    assertNotNull(response);

    assertEquals(UNEXPECTED_TRANSACTION_STATE + transaction.getState(), response.getReason());

    var transactionUpdated = fetchPrePopulatedTransaction();
    assertEquals(transaction.getState(), transactionUpdated.getState());
  }

  @ParameterizedTest
  @EnumSource(names = {"ITEM_RECEIVED", "RECEIVE_UNANNOUNCED"})
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/inn-reach-transaction/pre-populate-inn-reach-transaction.sql"
  })
  void checkTransactionIsInStateItemReceivedOrReceiveUnannounced(InnReachTransaction.TransactionState testEnums) {
    var transactionHoldDTO = createTransactionHoldDTO();
    var transactionBefore = fetchTransactionByTrackingId(PRE_POPULATED_TRACKING2_ID);

    transactionBefore.setState(testEnums);
    repository.save(transactionBefore);

    var responseEntity = testRestTemplate.exchange(
      "/inn-reach/d2ir/circ/returnuncirculated/{trackingId}/{centralCode}", HttpMethod.PUT,
      new HttpEntity<>(transactionHoldDTO, headers), InnReachResponseDTO.class,
      PRE_POPULATED_TRACKING2_ID, PRE_POPULATED_CENTRAL_CODE);

    var transactionAfter = fetchTransactionByTrackingId(PRE_POPULATED_TRACKING2_ID);

    assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
    assertEquals(RETURN_UNCIRCULATED, transactionAfter.getState());
  }

  @Test
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/inn-reach-transaction/pre-populate-inn-reach-transaction.sql"
  })
  void checkTransactionIsNotInStateItemReceivedOrReceiveUnannounced() {
    var transactionHoldDTO = createTransactionHoldDTO();
    var transactionBefore = fetchPrePopulatedTransaction();

    transactionBefore.setState(TRANSFER);
    repository.save(transactionBefore);

    var responseEntity = testRestTemplate.exchange(
      "/inn-reach/d2ir/circ/returnuncirculated/{trackingId}/{centralCode}", HttpMethod.PUT,
      new HttpEntity<>(transactionHoldDTO, headers), InnReachResponseDTO.class,
      PRE_POPULATED_TRACKING1_ID, PRE_POPULATED_CENTRAL_CODE);

    var transactionAfter = fetchPrePopulatedTransaction();

    assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
    assertEquals(TRANSFER, transactionAfter.getState());
  }

  @Test
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql"
  })
  void processLocalHoldCirculationRequest_createNew() {
    var transactionHoldDTO = createTransactionHoldDTO();
    transactionHoldDTO.setItemAgencyCode(PRE_POPULATED_LOCAL_AGENCY_CODE1);
    transactionHoldDTO.setPatronAgencyCode(PRE_POPULATED_LOCAL_AGENCY_CODE2);

    var responseEntity = testRestTemplate.exchange(
      CIRCULATION_OPERATION_ENDPOINT, HttpMethod.PUT,
      new HttpEntity<>(transactionHoldDTO, headers), InnReachResponseDTO.class,
      LOCAL_HOLD_OPERATION, NEW_TRANSACTION_TRACKING_ID, PRE_POPULATED_CENTRAL_CODE
    );

    assertEquals(HttpStatus.OK, responseEntity.getStatusCode());

    var responseBody = responseEntity.getBody();

    assertNotNull(responseBody);
    assertNotNull(responseBody.getErrors());
    assertEquals(0, responseBody.getErrors().size());
    assertEquals(InnReachResponse.OK_STATUS, responseBody.getStatus());

    var innReachTransaction = fetchTransactionByTrackingId(NEW_TRANSACTION_TRACKING_ID);
    assertNotNull(innReachTransaction);

    assertNotNull(innReachTransaction.getHold());
  }

  @Test
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/inn-reach-transaction/pre-populate-inn-reach-transaction.sql"
  })
  void processLocalHoldCirculationRequest_updateExiting() {
    var transactionHoldDTO = createTransactionHoldDTO();
    transactionHoldDTO.setItemAgencyCode(PRE_POPULATED_LOCAL_AGENCY_CODE1);
    transactionHoldDTO.setPatronAgencyCode(PRE_POPULATED_LOCAL_AGENCY_CODE2);

    var responseEntity = testRestTemplate.exchange(
      CIRCULATION_OPERATION_ENDPOINT, HttpMethod.PUT,
      new HttpEntity<>(transactionHoldDTO, headers), InnReachResponseDTO.class,
      LOCAL_HOLD_OPERATION, PRE_POPULATED_TRACKING1_ID, PRE_POPULATED_CENTRAL_CODE
    );

    assertEquals(HttpStatus.OK, responseEntity.getStatusCode());

    var responseBody = responseEntity.getBody();

    assertNotNull(responseBody);
    assertNotNull(responseBody.getErrors());
    assertEquals(0, responseBody.getErrors().size());
    assertEquals(InnReachResponse.OK_STATUS, responseBody.getStatus());

    var innReachTransaction = fetchPrePopulatedTransaction();
    assertNotNull(innReachTransaction);

    assertEquals(transactionHoldDTO.getTransactionTime(), innReachTransaction.getHold().getTransactionTime());
    assertEquals(transactionHoldDTO.getPatronId(), innReachTransaction.getHold().getPatronId());
    assertEquals(transactionHoldDTO.getPatronAgencyCode(), innReachTransaction.getHold().getPatronAgencyCode());
  }

  @Test
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/central-server/pre-populate-another-central-server.sql",
    "classpath:db/inn-reach-transaction/pre-populate-inn-reach-transaction.sql"
  })
  void processLocalHoldCirculationRequest_invalidAgencyCodes() {
    var transactionHoldDTO = createTransactionHoldDTO();
    transactionHoldDTO.setPatronAgencyCode(PRE_POPULATED_LOCAL_AGENCY_CODE1);
    transactionHoldDTO.setItemAgencyCode(PRE_POPULATED_ANOTHER_LOCAL_AGENCY_CODE1);

    var responseEntity = testRestTemplate.exchange(
      CIRCULATION_OPERATION_ENDPOINT, HttpMethod.PUT,
      new HttpEntity<>(transactionHoldDTO, headers), InnReachResponseDTO.class,
      LOCAL_HOLD_OPERATION, PRE_POPULATED_TRACKING1_ID, PRE_POPULATED_CENTRAL_CODE
    );
    var responseBody = responseEntity.getBody();

    assertEquals(BAD_REQUEST, responseEntity.getStatusCode());
    assertNotNull(responseBody);
    assertEquals("The patron and item agencies should be on the same local server", responseBody.getReason());
  }

  private InnReachTransaction fetchPrePopulatedTransaction() {
    return fetchTransactionByTrackingId(PRE_POPULATED_TRACKING1_ID);
  }

  private InnReachTransaction fetchTransactionByTrackingId(String trackingId) {
    return repository.findByTrackingIdAndCentralServerCode(trackingId, PRE_POPULATED_CENTRAL_CODE).get();
  }

  @Test
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/inn-reach-transaction/pre-populate-inn-reach-transaction.sql"
  })
  void processItemReceivedRequest_whenItemIsShipped() {
    var transaction = repository.findByTrackingIdAndCentralServerCode(PRE_POPULATED_TRACKING2_ID,
      PRE_POPULATED_CENTRAL_CODE).get();
    transaction.setState(ITEM_SHIPPED);
    repository.save(transaction);

    var transactionHoldDTO = createTransactionHoldDTO();

    var responseEntity = testRestTemplate.exchange(
      ITEM_RECEIVED_PATH, HttpMethod.PUT, new HttpEntity<>(transactionHoldDTO, headers), InnReachResponseDTO.class,
      PRE_POPULATED_TRACKING2_ID, PRE_POPULATED_CENTRAL_CODE);

    assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
    var responseEntityBody = responseEntity.getBody();
    assertNotNull(responseEntityBody);
    assertEquals("ok", responseEntityBody.getStatus());

    var transactionUpdated = repository.findByTrackingIdAndCentralServerCode(PRE_POPULATED_TRACKING2_ID,
      PRE_POPULATED_CENTRAL_CODE).get();
    assertEquals(ITEM_RECEIVED, transactionUpdated.getState());
  }

  @Test
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/inn-reach-transaction/pre-populate-inn-reach-transaction.sql"
  })
  void processItemReceivedRequest_whenItemIsNotShipped() {
    when(circulationClient.findRequest(any())).thenReturn(Optional.of(createRequestDTO()));
    when(circulationClient.checkOutByBarcode(any(CheckOutRequestDTO.class))).thenReturn(new LoanDTO().id(NEW_LOAN_ID));
    when(inventoryService.findDefaultServicePointIdForUser(PRE_POPULATED_PATRON2_ID)).thenReturn(Optional.of(PRE_POPULATE_SERVICE_ID));
    var transactionHoldDTO = createTransactionHoldDTO();

    var responseEntity = testRestTemplate.exchange(
      ITEM_RECEIVED_PATH, HttpMethod.PUT, new HttpEntity<>(transactionHoldDTO, headers), InnReachResponseDTO.class,
      PRE_POPULATED_TRACKING2_ID, PRE_POPULATED_CENTRAL_CODE);

    assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
    var responseEntityBody = responseEntity.getBody();
    assertNotNull(responseEntityBody);
    assertEquals("ok", responseEntityBody.getStatus());

    var transactionUpdated = fetchTransactionByTrackingId(PRE_POPULATED_TRACKING2_ID);
    assertEquals(ITEM_RECEIVED, transactionUpdated.getState());
    assertEquals(NEW_LOAN_ID, transactionUpdated.getHold().getFolioLoanId());
  }

  @Test
  @Sql(scripts = {
    "classpath:db/inn-reach-recall-user/pre-populate-inn-reach-recall-user.sql",
    "classpath:db/central-server/pre-populate-central-server-with-recall-user.sql",
    "classpath:db/inn-reach-transaction/pre-populate-inn-reach-transaction.sql",
    "classpath:db/patron-type-mapping/pre-populate-patron-type-mapping.sql"
  })
  void processRecallRequest_whenItemIsOnLoanToThePatron() {
    var recallDTO = createRecallDTO();
    var user = populateUser();

    when(userService.getUserById(PRE_POPULATED_PATRON_ID)).thenReturn(Optional.of(user));
    when(circulationClient.findRequest(any())).thenReturn(Optional.of(new RequestDTO()));
    when(circulationClient.sendRequest(requestDtoCaptor.capture())).thenReturn(new RequestDTO());
    when(inventoryService.findDefaultServicePointIdForUser(PRE_POPULATED_REQUESTER_ID))
      .thenReturn(Optional.of(PICK_IP_SERVICE_POINT));

    var responseEntity = testRestTemplate.exchange(
      RECALL_REQUEST_PATH, HttpMethod.PUT, new HttpEntity<>(recallDTO, headers), InnReachResponseDTO.class,
      PRE_POPULATED_TRACKING1_ID, PRE_POPULATED_CENTRAL_CODE);

    assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
    var responseEntityBody = responseEntity.getBody();
    assertNotNull(responseEntityBody);
    assertEquals("ok", responseEntityBody.getStatus());

    verify(requestService).createRecallRequest(any(), any(), any(), any());
    var transactionUpdated = repository.findByTrackingIdAndCentralServerCode(PRE_POPULATED_TRACKING1_ID,
      PRE_POPULATED_CENTRAL_CODE).get();
    assertEquals(RECALL, transactionUpdated.getState());

    RequestDTO requestDTO = requestDtoCaptor.getValue();
    assertEquals(RequestDTO.RequestLevel.ITEM.getName(), requestDTO.getRequestLevel());
    assertEquals(PRE_POPULATED_INSTANCE_ID, requestDTO.getInstanceId());
    assertEquals(PRE_POPULATED_HOLDINGS_RECORD_ID, requestDTO.getHoldingsRecordId());
    assertEquals(PRE_POPULATED_ITEM_ID, requestDTO.getItemId());
    assertEquals(PRE_POPULATED_REQUESTER_ID, requestDTO.getRequesterId());
  }

  @Test
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/inn-reach-transaction/pre-populate-inn-reach-transaction.sql",
    "classpath:db/patron-type-mapping/pre-populate-patron-type-mapping.sql"
  })
  void processRecallRequest_whenItemIsOnTheHoldShelf() {
    var requestDTO = new RequestDTO();
    requestDTO.setStatus(RequestDTO.RequestStatus.OPEN_AWAITING_PICKUP);
    var user = populateUser();

    when(userService.getUserById(PRE_POPULATED_PATRON_ID)).thenReturn(Optional.of(user));
    when(circulationClient.findRequest(any())).thenReturn(Optional.of(new RequestDTO()));
    when(circulationClient.sendRequest(requestDtoCaptor.capture())).thenReturn(new RequestDTO());
    when(circulationClient.findRequest(any())).thenReturn(Optional.of(requestDTO));
    doNothing().when(circulationClient).updateRequest(any(), any());

    var transaction = repository.findByTrackingIdAndCentralServerCode(PRE_POPULATED_TRACKING1_ID,
      PRE_POPULATED_CENTRAL_CODE).get();
    transaction.setState(ITEM_SHIPPED);
    transaction.getHold().setFolioLoanId(null);
    repository.save(transaction);

    var recallDTO = createRecallDTO();

    var responseEntity = testRestTemplate.exchange(
      RECALL_REQUEST_PATH, HttpMethod.PUT, new HttpEntity<>(recallDTO, headers), InnReachResponseDTO.class,
      PRE_POPULATED_TRACKING1_ID, PRE_POPULATED_CENTRAL_CODE);

    assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
    var responseEntityBody = responseEntity.getBody();
    assertNotNull(responseEntityBody);
    assertEquals("ok", responseEntityBody.getStatus());

    verify(requestService).cancelRequest(anyString(), any(UUID.class), any(UUID.class), eq("Item has been recalled."));
    var transactionUpdated = repository.findByTrackingIdAndCentralServerCode(PRE_POPULATED_TRACKING1_ID,
      PRE_POPULATED_CENTRAL_CODE).get();
    assertEquals(RECALL, transactionUpdated.getState());
  }

  @Test
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/inn-reach-transaction/pre-populate-inn-reach-transaction.sql",
    "classpath:db/patron-type-mapping/pre-populate-patron-type-mapping.sql"
  })
  void processRecallRequest_whenBadRequest() {
    var requestDTO = new RequestDTO();
    requestDTO.setStatus(RequestDTO.RequestStatus.OPEN_AWAITING_PICKUP);
    var user = populateUser();

    when(userService.getUserById(PRE_POPULATED_PATRON_ID)).thenReturn(Optional.of(user));
    when(circulationClient.sendRequest(requestDtoCaptor.capture())).thenReturn(new RequestDTO());
    when(circulationClient.findRequest(any())).thenReturn(Optional.of(requestDTO));
    doThrow(new IllegalArgumentException("Test exception.")).when(requestService).findRequest(any(UUID.class));

    var recallDTO = createRecallDTO();

    var responseEntity = testRestTemplate.exchange(
      RECALL_REQUEST_PATH, HttpMethod.PUT, new HttpEntity<>(recallDTO, headers), InnReachResponseDTO.class,
      PRE_POPULATED_TRACKING1_ID, PRE_POPULATED_CENTRAL_CODE);

    assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
    var responseEntityBody = responseEntity.getBody();
    assertNotNull(responseEntityBody);
    assertTrue(responseEntityBody.getReason().contains("Test exception."));

    var transactionUpdated = repository.findByTrackingIdAndCentralServerCode(PRE_POPULATED_TRACKING1_ID,
      PRE_POPULATED_CENTRAL_CODE).get();
    assertNotEquals(RECALL, transactionUpdated.getState());
  }

  @Test
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/inn-reach-transaction/pre-populate-inn-reach-transaction.sql",
    "classpath:db/patron-type-mapping/pre-populate-patron-type-mapping.sql"
  })
  void processRecallRequest_whenRecallUserIsNotSet() {
    var user = populateUser();

    when(userService.getUserById(PRE_POPULATED_PATRON_ID)).thenReturn(Optional.of(user));
    when(circulationClient.findRequest(any())).thenReturn(Optional.of(new RequestDTO()));
    when(circulationClient.sendRequest(requestDtoCaptor.capture())).thenReturn(new RequestDTO());

    when(circulationClient.findRequest(any())).thenReturn(Optional.of(new RequestDTO()));
    var recallDTO = createRecallDTO();

    var responseEntity = testRestTemplate.exchange(
      RECALL_REQUEST_PATH, HttpMethod.PUT, new HttpEntity<>(recallDTO, headers), InnReachResponseDTO.class,
      PRE_POPULATED_TRACKING1_ID, PRE_POPULATED_CENTRAL_CODE);

    assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
    var responseEntityBody = responseEntity.getBody();
    assertNotNull(responseEntityBody);
    assertTrue(responseEntityBody.getReason().contains("Recall user is not set for central server with code = " + PRE_POPULATED_CENTRAL_CODE));

    var transactionUpdated = repository.findByTrackingIdAndCentralServerCode(PRE_POPULATED_TRACKING1_ID,
      PRE_POPULATED_CENTRAL_CODE).get();
    assertNotEquals(RECALL, transactionUpdated.getState());
  }

  @Test
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/inn-reach-transaction/pre-populate-inn-reach-transaction.sql"
  })
  void borrowerRenewRequestCalculatedDueDateAfterBorrowerDueDate() {
    var loan = new LoanDTO();
    loan.setDueDate(new Date(Instant.now().toEpochMilli()));
    when(circulationClient.findLoan(any())).thenReturn(Optional.of(loan));

    var renew = new LoanDTO();
    renew.setDueDate(new Date(Instant.now().plus(1, ChronoUnit.DAYS).toEpochMilli()));
    when(circulationClient.renewLoan(any())).thenReturn(renew);

    var dueDateTime = (int) Instant.now().minus(1, ChronoUnit.DAYS).getEpochSecond();
    var borrowerItem = new RenewLoanDTO();
    borrowerItem.setDueDateTime(dueDateTime);

    var transactionHoldDTO = createTransactionHoldDTO();
    borrowerItem.setTransactionTime(transactionHoldDTO.getTransactionTime());
    borrowerItem.setPatronId(transactionHoldDTO.getPatronId());
    borrowerItem.setPatronAgencyCode(transactionHoldDTO.getPatronAgencyCode());
    borrowerItem.setItemAgencyCode(transactionHoldDTO.getItemAgencyCode());
    borrowerItem.setItemId(transactionHoldDTO.getItemId());

    var responseEntity = testRestTemplate.exchange(
      "/inn-reach/d2ir/circ/borrowerrenew/{trackingId}/{centralCode}", HttpMethod.PUT,
      new HttpEntity<>(borrowerItem, headers), RenewLoanDTO.class,
      PRE_POPULATED_TRACKING1_ID, PRE_POPULATED_CENTRAL_CODE);

    var transactionState = fetchPrePopulatedTransaction().getState();

    assertEquals(OK, responseEntity.getStatusCode());
    assertEquals(BORROWER_RENEW, transactionState);
  }

  @Test
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/inn-reach-transaction/pre-populate-inn-reach-transaction.sql"
  })
  void borrowerRenewRequestCalculatedDueDateBeforeBorrowerDueDate() {
    when(innReachExternalService.postInnReachApi(any(), any(), any())).thenReturn("ok");

    var loan = new LoanDTO();
    loan.setDueDate(new Date(Instant.now().toEpochMilli()));
    when(circulationClient.findLoan(any())).thenReturn(Optional.of(loan));

    var renew = new LoanDTO();
    renew.setDueDate(new Date(Instant.now().minus(1, ChronoUnit.DAYS).toEpochMilli()));
    when(circulationClient.renewLoan(any())).thenReturn(renew);

    var dueDateTime = (int) Instant.now().plus(1, ChronoUnit.DAYS).getEpochSecond();
    var borrowerItem = new RenewLoanDTO();
    borrowerItem.setDueDateTime(dueDateTime);

    var transactionHoldDTO = createTransactionHoldDTO();
    borrowerItem.setTransactionTime(transactionHoldDTO.getTransactionTime());
    borrowerItem.setPatronId(transactionHoldDTO.getPatronId());
    borrowerItem.setPatronAgencyCode(transactionHoldDTO.getPatronAgencyCode());
    borrowerItem.setItemAgencyCode(transactionHoldDTO.getItemAgencyCode());
    borrowerItem.setItemId(transactionHoldDTO.getItemId());

    var responseEntity = testRestTemplate.exchange(
      "/inn-reach/d2ir/circ/borrowerrenew/{trackingId}/{centralCode}", HttpMethod.PUT,
      new HttpEntity<>(borrowerItem, headers), RenewLoanDTO.class,
      PRE_POPULATED_TRACKING1_ID, PRE_POPULATED_CENTRAL_CODE);

    var transactionState = fetchPrePopulatedTransaction().getState();

    assertEquals(OK, responseEntity.getStatusCode());
    assertEquals(RECALL, transactionState);
  }

  @Test
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/inn-reach-transaction/pre-populate-inn-reach-transaction.sql"
  })
  void borrowerRenewRequestExistingDueDateBeforeRequestedDueDateAndExceptionOccurs() {
    when(innReachExternalService.postInnReachApi(any(), any(), any())).thenReturn("ok");

    var loan = new LoanDTO();
    loan.setDueDate(new Date(Instant.now().toEpochMilli()));
    when(circulationClient.findLoan(any())).thenReturn(Optional.of(loan));

    when(circulationClient.renewLoan(any())).thenThrow(IllegalArgumentException.class);

    var dueDateTime = (int) Instant.now().plus(1, ChronoUnit.DAYS).getEpochSecond();
    var borrowerItem = new RenewLoanDTO();
    borrowerItem.setDueDateTime(dueDateTime);

    var transactionHoldDTO = createTransactionHoldDTO();
    borrowerItem.setTransactionTime(transactionHoldDTO.getTransactionTime());
    borrowerItem.setPatronId(transactionHoldDTO.getPatronId());
    borrowerItem.setPatronAgencyCode(transactionHoldDTO.getPatronAgencyCode());
    borrowerItem.setItemAgencyCode(transactionHoldDTO.getItemAgencyCode());
    borrowerItem.setItemId(transactionHoldDTO.getItemId());

    var responseEntity = testRestTemplate.exchange(
      "/inn-reach/d2ir/circ/borrowerrenew/{trackingId}/{centralCode}", HttpMethod.PUT,
      new HttpEntity<>(borrowerItem, headers), RenewLoanDTO.class,
      PRE_POPULATED_TRACKING1_ID, PRE_POPULATED_CENTRAL_CODE);

    var transactionState = fetchPrePopulatedTransaction().getState();

    assertEquals(OK, responseEntity.getStatusCode());
    assertEquals(RECALL, transactionState);
  }

  @Test
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/inn-reach-transaction/pre-populate-inn-reach-transaction.sql"
  })
  void borrowerRenewRequestFailedRenewLoan() {
    var loan = new LoanDTO();
    loan.setDueDate(new Date(Instant.now().toEpochMilli()));
    when(circulationClient.findLoan(any())).thenReturn(Optional.of(loan));

    when(circulationClient.renewLoan(any())).thenThrow(IllegalArgumentException.class);

    var dueDateTime = (int) Instant.now().getEpochSecond();
    var borrowerItem = new RenewLoanDTO();
    borrowerItem.setDueDateTime(dueDateTime);

    var transactionHoldDTO = createTransactionHoldDTO();
    borrowerItem.setTransactionTime(transactionHoldDTO.getTransactionTime());
    borrowerItem.setPatronId(transactionHoldDTO.getPatronId());
    borrowerItem.setPatronAgencyCode(transactionHoldDTO.getPatronAgencyCode());
    borrowerItem.setItemAgencyCode(transactionHoldDTO.getItemAgencyCode());
    borrowerItem.setItemId(transactionHoldDTO.getItemId());

    var transactionStateBefore = fetchPrePopulatedTransaction().getState();

    var responseEntity = testRestTemplate.exchange(
      "/inn-reach/d2ir/circ/borrowerrenew/{trackingId}/{centralCode}", HttpMethod.PUT,
      new HttpEntity<>(borrowerItem, headers), RenewLoanDTO.class,
      PRE_POPULATED_TRACKING1_ID, PRE_POPULATED_CENTRAL_CODE);

    var transactionStateAfter = fetchPrePopulatedTransaction().getState();

    assertEquals(BAD_REQUEST, responseEntity.getStatusCode());
    assertEquals(transactionStateBefore, transactionStateAfter);
  }

  @Test
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/inn-reach-transaction/pre-populate-inn-reach-transaction.sql"
  })
  void borrowerRenewRequestFailedRecallRequest() {
    when(innReachExternalService.postInnReachApi(any(), any(), any())).thenThrow(IllegalArgumentException.class);

    var loan = new LoanDTO();
    loan.setDueDate(new Date(Instant.now().toEpochMilli()));
    when(circulationClient.findLoan(any())).thenReturn(Optional.of(loan));

    var renew = new LoanDTO();
    renew.setDueDate(new Date(Instant.now().minus(1, ChronoUnit.DAYS).toEpochMilli()));
    when(circulationClient.renewLoan(any())).thenReturn(renew);

    var dueDateTime = (int) Instant.now().getEpochSecond();
    var borrowerItem = new RenewLoanDTO();
    borrowerItem.setDueDateTime(dueDateTime);

    var transactionHoldDTO = createTransactionHoldDTO();
    borrowerItem.setTransactionTime(transactionHoldDTO.getTransactionTime());
    borrowerItem.setPatronId(transactionHoldDTO.getPatronId());
    borrowerItem.setPatronAgencyCode(transactionHoldDTO.getPatronAgencyCode());
    borrowerItem.setItemAgencyCode(transactionHoldDTO.getItemAgencyCode());
    borrowerItem.setItemId(transactionHoldDTO.getItemId());

    var transactionStateBefore = fetchPrePopulatedTransaction().getState();

    var responseEntity = testRestTemplate.exchange(
      "/inn-reach/d2ir/circ/borrowerrenew/{trackingId}/{centralCode}", HttpMethod.PUT,
      new HttpEntity<>(borrowerItem, headers), RenewLoanDTO.class,
      PRE_POPULATED_TRACKING1_ID, PRE_POPULATED_CENTRAL_CODE);

    var transactionStateAfter = fetchPrePopulatedTransaction().getState();

    assertEquals(BAD_REQUEST, responseEntity.getStatusCode());
    assertEquals(transactionStateBefore, transactionStateAfter);
  }

  @Test
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/inn-reach-transaction/pre-populate-inn-reach-transaction.sql"
  })
  void shouldNotProcessCircRequest_whenRequiredHeadersAreNotPresent() {
    var transactionHoldDTO = createTransactionHoldDTO();

    var responseEntity = testRestTemplate.exchange(
      ITEM_RECEIVED_PATH, HttpMethod.PUT, new HttpEntity<>(transactionHoldDTO), InnReachResponseDTO.class,
      PRE_POPULATED_TRACKING1_ID, PRE_POPULATED_CENTRAL_CODE);

    assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
    var responseEntityBody = responseEntity.getBody();
    assertNotNull(responseEntityBody);
    assertEquals("Required request header 'X-To-Code' for method parameter type String is not present", responseEntityBody.getReason());
  }

  @ParameterizedTest
  @EnumSource(names = {"ITEM_IN_TRANSIT", "RETURN_UNCIRCULATED", "ITEM_RECEIVED", "ITEM_SHIPPED"})
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/inn-reach-transaction/pre-populate-inn-reach-transaction.sql"
  })
  void checkTransactionIsNotInStatePatronHoldOrTransfer(InnReachTransaction.TransactionState state) {
    var transactionHoldDTO = createTransactionHoldDTO();
    var transactionBefore = fetchPrePopulatedTransaction();
    var configurationDto =
            deserializeFromJsonFile("/configuration/configuration-details-example.json", ConfigurationDTO.class);
    when(configurationService.fetchConfigurationsDetailsByModule(any())).
            thenReturn(ResultList.asSinglePage(configurationDto));

    transactionBefore.setState(state);
    repository.save(transactionBefore);

    var responseEntity = testRestTemplate.exchange(
      "/inn-reach/d2ir/circ/finalcheckin/{trackingId}/{centralCode}", HttpMethod.PUT,
      new HttpEntity<>(transactionHoldDTO, headers), InnReachResponseDTO.class,
      PRE_POPULATED_TRACKING1_ID, PRE_POPULATED_CENTRAL_CODE);

    var transactionAfter = fetchPrePopulatedTransaction();

    assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
    assertEquals(FINAL_CHECKIN, transactionAfter.getState());
    assertPatronHoldFieldsAreNull((TransactionPatronHold) transactionAfter.getHold());
  }

  @ParameterizedTest
  @EnumSource(names = {"PATRON_HOLD", "TRANSFER"})
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/inn-reach-transaction/pre-populate-inn-reach-transaction.sql"
  })
  void checkTransactionIsInStatePatronHoldOrTransfer(InnReachTransaction.TransactionState state) {
    var transactionHoldDTO = createTransactionHoldDTO();
    var transactionBefore = fetchPrePopulatedTransaction();

    transactionBefore.setState(state);
    repository.save(transactionBefore);

    var responseEntity = testRestTemplate.exchange(
      "/inn-reach/d2ir/circ/finalcheckin/{trackingId}/{centralCode}", HttpMethod.PUT,
      new HttpEntity<>(transactionHoldDTO, headers), InnReachResponseDTO.class,
      PRE_POPULATED_TRACKING1_ID, PRE_POPULATED_CENTRAL_CODE);

    var transaction = fetchPrePopulatedTransaction();

    assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
    assertEquals(state, transaction.getState());
  }

  @Test
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/inn-reach-transaction/pre-populate-inn-reach-transaction.sql"
  })
  void testClaimsItemReturned() {
    var request = createClaimsItemReturnedDTO();
    var date = Instant.now().truncatedTo(ChronoUnit.SECONDS);
    request.setClaimsReturnedDate((int) date.getEpochSecond());

    var responseEntity = testRestTemplate.exchange(
      "/inn-reach/d2ir/circ/claimsreturned/{trackingId}/{centralCode}", HttpMethod.PUT,
      new HttpEntity<>(request, headers), InnReachResponseDTO.class,
      PRE_POPULATED_TRACKING1_ID, PRE_POPULATED_CENTRAL_CODE);

    assertEquals(HttpStatus.OK, responseEntity.getStatusCode());

    var updatedTransaction = fetchPrePopulatedTransaction();

    assertEquals(CLAIMS_RETURNED, updatedTransaction.getState());
    assertNull(updatedTransaction.getHold().getPatronId());
    assertNull(updatedTransaction.getHold().getPatronName());

    verify(circulationClient).claimItemReturned(any(), argThat(req -> date.equals(req.getItemClaimedReturnedDateTime().toInstant())));
  }

  @Test
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/inn-reach-transaction/pre-populate-inn-reach-transaction.sql"
  })
  void testClaimsItemReturned_UnknownDate() {
    var request = createClaimsItemReturnedDTO();
    request.setClaimsReturnedDate(-1);

    var responseEntity = testRestTemplate.exchange(
      "/inn-reach/d2ir/circ/claimsreturned/{trackingId}/{centralCode}", HttpMethod.PUT,
      new HttpEntity<>(request, headers), InnReachResponseDTO.class,
      PRE_POPULATED_TRACKING1_ID, PRE_POPULATED_CENTRAL_CODE);

    assertEquals(HttpStatus.OK, responseEntity.getStatusCode());

    var updatedTransaction = fetchPrePopulatedTransaction();

    assertEquals(CLAIMS_RETURNED, updatedTransaction.getState());
    assertNull(updatedTransaction.getHold().getPatronId());
    assertNull(updatedTransaction.getHold().getPatronName());

    verify(circulationClient).claimItemReturned(any(), argThat(req -> req.getItemClaimedReturnedDateTime() != null));
  }

  @Test
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/inn-reach-transaction/pre-populate-inn-reach-transaction.sql",
    "classpath:db/patron-type-mapping/pre-populate-patron-type-mapping.sql",
  })
  void processCancelRequest() {
    doNothing().when(requestService).cancelRequest(anyString(), any(UUID.class), any(UUID.class), anyString());
    doNothing().when(virtualRecordService).deleteVirtualRecords(any(UUID.class), any(UUID.class), any(UUID.class), any(UUID.class));
    when(userService.getUserById(any(UUID.class))).thenReturn(Optional.of(populateUser()));

    var cancelRequestDTO = createCancelRequestDTO();

    var responseEntity = testRestTemplate.exchange(
      CANCEL_REQUEST_PATH, HttpMethod.PUT,
      new HttpEntity<>(cancelRequestDTO, headers), InnReachResponseDTO.class,
      PRE_POPULATED_TRACKING1_ID, PRE_POPULATED_CENTRAL_CODE);

    verify(requestService).cancelRequest(anyString(), any(UUID.class), any(UUID.class), anyString());
    verify(virtualRecordService).deleteVirtualRecords(any(UUID.class), any(UUID.class), any(UUID.class), any(UUID.class));

    var transactionAfter = fetchPrePopulatedTransaction();

    assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
    assertEquals(CANCEL_REQUEST, transactionAfter.getState());
    assertPatronHoldFieldsAreNull((TransactionPatronHold) transactionAfter.getHold());
  }

  @ParameterizedTest
  @EnumSource(names = {"ITEM_RECEIVED"})
  @Sql(scripts = {
          "classpath:db/central-server/pre-populate-central-server.sql",
          "classpath:db/inn-reach-transaction/pre-populate-inn-reach-transaction.sql"
  })
  void testFinalCheckInWithDeleteVirtualRecord(InnReachTransaction.TransactionState state) {
    var transactionHoldDTO = createTransactionHoldDTO();
    var transactionBefore = fetchPrePopulatedTransaction();
    var configurationDto =
            deserializeFromJsonFile("/configuration/configuration-details-example.json", ConfigurationDTO.class);
    when(configurationService.fetchConfigurationsDetailsByModule(any())).
            thenReturn(ResultList.asSinglePage(configurationDto));

    transactionBefore.setState(state);
    repository.save(transactionBefore);

    var responseEntity = testRestTemplate.exchange(
            "/inn-reach/d2ir/circ/finalcheckin/{trackingId}/{centralCode}", HttpMethod.PUT,
            new HttpEntity<>(transactionHoldDTO, headers), InnReachResponseDTO.class,
            PRE_POPULATED_TRACKING1_ID, PRE_POPULATED_CENTRAL_CODE);

    await().atMost(ASYNC_AWAIT_TIMEOUT).untilAsserted(() -> {
      var transactionAfter = fetchPrePopulatedTransaction();
      assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
      assertEquals(FINAL_CHECKIN, transactionAfter.getState());
      assertPatronHoldFieldsAreNull((TransactionPatronHold) transactionAfter.getHold());
    });
  }

  private void assertPatronHoldFieldsAreNull(TransactionPatronHold hold) {
    assertNull(hold.getPatronId());
    assertNull(hold.getPatronName());
    assertNull(hold.getFolioPatronId());
    assertNull(hold.getFolioPatronBarcode());
    assertNull(hold.getFolioItemId());
    assertNull(hold.getFolioHoldingId());
    assertNull(hold.getFolioInstanceId());
    assertNull(hold.getFolioLoanId());
    assertNull(hold.getFolioItemBarcode());
  }

  private User populateUser() {
    var user = new User();
    user.setId(PRE_POPULATED_PATRON_ID);
    user.setActive(true);
    user.setUsername("test");
    user.setPatronGroupId(PRE_POPULATE_PATRON_GROUP_ID);
    var personal = new User.Personal();
    personal.setPreferredFirstName("Paul");
    personal.setFirstName("MuaDibs");
    personal.setLastName("Atreides");
    user.setPersonal(personal);
    return user;
  }

  private static boolean isCentralPatronInfoCleared(InnReachTransaction transaction) {
    var hold = transaction.getHold();
    return ObjectUtils.allNull(hold.getPatronId(), hold.getPatronName());
  }
}
