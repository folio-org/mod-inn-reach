package org.folio.innreach.controller.d2ir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
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

import static org.folio.innreach.domain.entity.InnReachTransaction.TransactionState.BORROWER_RENEW;
import static org.folio.innreach.domain.entity.InnReachTransaction.TransactionState.BORROWING_SITE_CANCEL;
import static org.folio.innreach.domain.entity.InnReachTransaction.TransactionState.FINAL_CHECKIN;
import static org.folio.innreach.domain.entity.InnReachTransaction.TransactionState.ITEM_HOLD;
import static org.folio.innreach.domain.entity.InnReachTransaction.TransactionState.ITEM_IN_TRANSIT;
import static org.folio.innreach.domain.entity.InnReachTransaction.TransactionState.ITEM_RECEIVED;
import static org.folio.innreach.domain.entity.InnReachTransaction.TransactionState.ITEM_SHIPPED;
import static org.folio.innreach.domain.entity.InnReachTransaction.TransactionState.PATRON_HOLD;
import static org.folio.innreach.domain.entity.InnReachTransaction.TransactionState.RECALL;
import static org.folio.innreach.domain.entity.InnReachTransaction.TransactionState.RECEIVE_UNANNOUNCED;
import static org.folio.innreach.domain.entity.InnReachTransaction.TransactionState.RETURN_UNCIRCULATED;
import static org.folio.innreach.domain.entity.InnReachTransaction.TransactionState.TRANSFER;
import static org.folio.innreach.fixture.CirculationFixture.createItemShippedDTO;
import static org.folio.innreach.fixture.CirculationFixture.createRecallDTO;
import static org.folio.innreach.fixture.CirculationFixture.createTransactionHoldDTO;
import static org.folio.innreach.fixture.ServicePointUserFixture.createServicePointUserDTO;
import static org.folio.innreach.fixture.TestUtil.circHeaders;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlMergeMode;

import org.folio.innreach.client.CirculationClient;
import org.folio.innreach.client.ServicePointsUsersClient;
import org.folio.innreach.controller.base.BaseControllerTest;
import org.folio.innreach.domain.dto.folio.ResultList;
import org.folio.innreach.domain.dto.folio.circulation.RequestDTO;
import org.folio.innreach.domain.dto.folio.inventory.InventoryItemDTO;
import org.folio.innreach.domain.entity.InnReachTransaction;
import org.folio.innreach.domain.service.ItemService;
import org.folio.innreach.domain.service.RequestService;
import org.folio.innreach.dto.CheckOutRequestDTO;
import org.folio.innreach.dto.InnReachResponseDTO;
import org.folio.innreach.dto.BorrowerRenewDTO;
import org.folio.innreach.dto.CheckOutResponseDTO;
import org.folio.innreach.external.dto.InnReachResponse;
import org.folio.innreach.external.service.InnReachExternalService;
import org.folio.innreach.repository.InnReachTransactionRepository;

@Sql(
  scripts = {
    "classpath:db/central-server/clear-central-server-tables.sql",
    "classpath:db/inn-reach-transaction/clear-inn-reach-transaction-tables.sql"
  },
  executionPhase = AFTER_TEST_METHOD
)
@SqlMergeMode(MERGE)
class InnReachCirculationControllerTest extends BaseControllerTest {

  private static final String ITEM_IN_TRANSIT_ENDPOINT = "/inn-reach/d2ir/circ/intransit/{trackingId}/{centralCode}";
  private static final String CIRCULATION_OPERATION_ENDPOINT = "/inn-reach/d2ir/circ/{circulationOperationName}/{trackingId}/{centralCode}";

  private static final String PRE_POPULATED_TRACKING_ID = "tracking1";
  private static final String NEW_TRANSACTION_TRACKING_ID = "tracking99";
  private static final String PRE_POPULATED_CENTRAL_CODE = "d2ir";

  private static final String PATRON_HOLD_OPERATION = "patronhold";
  private static final String ITEM_SHIPPED_OPERATION = "itemshipped";
  private static final String RECALL_REQUEST_PATH = "/inn-reach/d2ir/circ/recall/{trackingId}/{centralCode}";
  private static final String LOCAL_HOLD_OPERATION = "localhold";

  private static final String CANCEL_ITEM_HOLD_PATH = "/inn-reach/d2ir/circ/cancelitemhold/{trackingId}/{centralCode}";
  private static final String ITEM_RECEIVED_PATH = "/inn-reach/d2ir/circ/itemreceived/{trackingId}/{centralCode}";

  private static final String UNEXPECTED_TRANSACTION_STATE = "Unexpected transaction state: ";

  private static final UUID NEW_LOAN_ID = UUID.randomUUID();

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

  private HttpHeaders headers = circHeaders();

  @Test
  void processCreatePatronHoldCirculationRequest_and_createNewPatronHold() {
    var transactionHoldDTO = createTransactionHoldDTO();

    var responseEntity = testRestTemplate.postForEntity(
      "/inn-reach/d2ir/circ/{circulationOperationName}/{trackingId}/{centralCode}",
      new HttpEntity<>(transactionHoldDTO, headers), InnReachResponseDTO.class, PATRON_HOLD_OPERATION, "tracking99", PRE_POPULATED_CENTRAL_CODE);

    assertEquals(HttpStatus.OK, responseEntity.getStatusCode());

    var responseBody = responseEntity.getBody();

    assertNotNull(responseBody);
    assertNotNull(responseBody.getErrors());
    assertEquals(0, responseBody.getErrors().size());
    assertEquals(InnReachResponse.OK_STATUS, responseBody.getStatus());

    var innReachTransaction = repository.findByTrackingIdAndCentralServerCode("tracking99", PRE_POPULATED_CENTRAL_CODE);

    assertTrue(innReachTransaction.isPresent());
    assertNotNull(innReachTransaction.get().getHold());
  }

  @Test
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/inn-reach-transaction/pre-populate-inn-reach-transaction.sql"
  })
  void processCreatePatronHoldCirculationRequest_and_updateExitingPatronHold() {
    var transactionHoldDTO = createTransactionHoldDTO();

    var responseEntity = testRestTemplate.postForEntity(
      "/inn-reach/d2ir/circ/{circulationOperationName}/{trackingId}/{centralCode}",
      new HttpEntity<>(transactionHoldDTO, headers), InnReachResponseDTO.class, PATRON_HOLD_OPERATION, PRE_POPULATED_TRACKING_ID, PRE_POPULATED_CENTRAL_CODE);

    assertEquals(HttpStatus.OK, responseEntity.getStatusCode());

    var responseBody = responseEntity.getBody();

    assertNotNull(responseBody);
    assertNotNull(responseBody.getErrors());
    assertEquals(0, responseBody.getErrors().size());
    assertEquals(InnReachResponse.OK_STATUS, responseBody.getStatus());

    var updatedTransaction = repository.findByTrackingIdAndCentralServerCode(PRE_POPULATED_TRACKING_ID, PRE_POPULATED_CENTRAL_CODE);

    assertTrue(updatedTransaction.isPresent());

    var innReachTransaction = updatedTransaction.get();

    assertEquals(transactionHoldDTO.getTransactionTime(), innReachTransaction.getHold().getTransactionTime());
    assertEquals(transactionHoldDTO.getPatronId(), innReachTransaction.getHold().getPatronId());
    assertEquals(transactionHoldDTO.getPatronAgencyCode(), innReachTransaction.getHold().getPatronAgencyCode());
  }

  @Test
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/inn-reach-transaction/pre-populate-inn-reach-transaction.sql"
  })
  void processItemShippedCircRequest_updateFolioItem_whenAssociatedItemExists() {
    when(itemService.find(any())).thenReturn(Optional.of(InventoryItemDTO.builder().build()));
    when(itemService.findItemByBarcode(any())).thenReturn(Optional.of(InventoryItemDTO.builder().build()));
    when(itemService.changeAndUpdate(any(), any())).thenReturn(Optional.of(InventoryItemDTO.builder().build()));

    var itemShippedDTO = createItemShippedDTO();

    var responseEntity = testRestTemplate.exchange(
      "/inn-reach/d2ir/circ/{circulationOperationName}/{trackingId}/{centralCode}", HttpMethod.PUT,
      new HttpEntity<>(itemShippedDTO, headers), InnReachResponseDTO.class,
      ITEM_SHIPPED_OPERATION, PRE_POPULATED_TRACKING_ID, PRE_POPULATED_CENTRAL_CODE);

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
    when(itemService.findItemByBarcode(any())).thenReturn(Optional.of(InventoryItemDTO.builder().build()));
    when(itemService.changeAndUpdate(any(), any(), any())).thenThrow(new IllegalArgumentException("Not found"));

    var transactionHoldDTO = createTransactionHoldDTO();

    var responseEntity = testRestTemplate.exchange(
      "/inn-reach/d2ir/circ/{circulationOperationName}/{trackingId}/{centralCode}", HttpMethod.PUT,
      new HttpEntity<>(transactionHoldDTO, headers), InnReachResponseDTO.class,
      ITEM_SHIPPED_OPERATION, PRE_POPULATED_TRACKING_ID, PRE_POPULATED_CENTRAL_CODE);

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

    var transaction = fetchPrePopulatedTransaction();
    transaction.getHold().setFolioLoanId(null);
    repository.save(transaction);

    var transactionHoldDTO = createTransactionHoldDTO();

    var responseEntity = testRestTemplate.exchange(
      CANCEL_ITEM_HOLD_PATH, HttpMethod.PUT, new HttpEntity<>(transactionHoldDTO, headers), InnReachResponseDTO.class,
      PRE_POPULATED_TRACKING_ID, PRE_POPULATED_CENTRAL_CODE);

    assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
    var responseEntityBody = responseEntity.getBody();
    assertNotNull(responseEntityBody);
    assertEquals("ok", responseEntityBody.getStatus());

    verify(requestService).cancelRequest(any(), eq("Request cancelled at borrowing site"));
    var transactionUpdated = fetchPrePopulatedTransaction();
    assertEquals(BORROWING_SITE_CANCEL, transactionUpdated.getState());
  }

  @Test
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/inn-reach-transaction/pre-populate-inn-reach-transaction.sql"
  })
  void processCancelItemHoldRequest_whenItemIsCheckedOut() {
    var transactionHoldDTO = createTransactionHoldDTO();

    var responseEntity = testRestTemplate.exchange(
      CANCEL_ITEM_HOLD_PATH, HttpMethod.PUT, new HttpEntity<>(transactionHoldDTO, headers), InnReachResponseDTO.class,
      PRE_POPULATED_TRACKING_ID, PRE_POPULATED_CENTRAL_CODE);

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
    var transaction = fetchPrePopulatedTransaction();
    transaction.setState(ITEM_HOLD);
    repository.save(transaction);

    var transactionHoldDTO = createTransactionHoldDTO();

    when(servicePointsUsersClient.findServicePointsUsers(any())).thenReturn(ResultList.asSinglePage(createServicePointUserDTO()));
    when(circulationClient.checkOutByBarcode(any(CheckOutRequestDTO.class))).thenReturn(new CheckOutResponseDTO().id(NEW_LOAN_ID));

    var responseEntity = testRestTemplate.exchange(
      "/inn-reach/d2ir/circ/receiveunshipped/{trackingId}/{centralCode}", HttpMethod.PUT,
      new HttpEntity<>(transactionHoldDTO, headers), InnReachResponseDTO.class,
      PRE_POPULATED_TRACKING_ID, PRE_POPULATED_CENTRAL_CODE);

    assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
    var responseEntityBody = responseEntity.getBody();
    assertNotNull(responseEntityBody);
    assertEquals("ok", responseEntityBody.getStatus());

    var transactionUpdated = fetchPrePopulatedTransaction();
    assertEquals(RECEIVE_UNANNOUNCED, transactionUpdated.getState());
    assertEquals(NEW_LOAN_ID, transactionUpdated.getHold().getFolioLoanId());
  }

  @Test
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/inn-reach-transaction/pre-populate-inn-reach-transaction.sql"
  })
  void precessReportUnshippedItemReceived_whenTransactionItemShipped() {
    var transaction = fetchPrePopulatedTransaction();
    transaction.setState(ITEM_SHIPPED);
    repository.save(transaction);

    var transactionHoldDTO = createTransactionHoldDTO();

    var responseEntity = testRestTemplate.exchange(
      "/inn-reach/d2ir/circ/receiveunshipped/{trackingId}/{centralCode}", HttpMethod.PUT,
      new HttpEntity<>(transactionHoldDTO, headers), InnReachResponseDTO.class,
      PRE_POPULATED_TRACKING_ID, PRE_POPULATED_CENTRAL_CODE);

    assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
    var responseEntityBody = responseEntity.getBody();
    assertNotNull(responseEntityBody);
    assertEquals(UNEXPECTED_TRANSACTION_STATE + transaction.getState(),
      responseEntityBody.getReason());

    var transactionUpdated = fetchPrePopulatedTransaction();
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
      PRE_POPULATED_TRACKING_ID, PRE_POPULATED_CENTRAL_CODE);

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
      PRE_POPULATED_TRACKING_ID, PRE_POPULATED_CENTRAL_CODE);

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
    var transactionBefore = fetchPrePopulatedTransaction();

    transactionBefore.setState(testEnums);
    repository.save(transactionBefore);

    var responseEntity = testRestTemplate.exchange(
      "/inn-reach/d2ir/circ/returnuncirculated/{trackingId}/{centralCode}", HttpMethod.PUT,
      new HttpEntity<>(transactionHoldDTO, headers), InnReachResponseDTO.class,
      PRE_POPULATED_TRACKING_ID, PRE_POPULATED_CENTRAL_CODE);

    var transactionAfter = fetchPrePopulatedTransaction();

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
      PRE_POPULATED_TRACKING_ID, PRE_POPULATED_CENTRAL_CODE);

    var transactionAfter = fetchPrePopulatedTransaction();

    assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
    assertEquals(TRANSFER, transactionAfter.getState());
  }

  @Test
  void processLocalHoldCirculationRequest_createNew() {
    var transactionHoldDTO = createTransactionHoldDTO();
    transactionHoldDTO.setPatronAgencyCode(transactionHoldDTO.getItemAgencyCode());

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
    transactionHoldDTO.setPatronAgencyCode(transactionHoldDTO.getItemAgencyCode());

    var responseEntity = testRestTemplate.exchange(
      CIRCULATION_OPERATION_ENDPOINT, HttpMethod.PUT,
      new HttpEntity<>(transactionHoldDTO, headers), InnReachResponseDTO.class,
      LOCAL_HOLD_OPERATION, PRE_POPULATED_TRACKING_ID, PRE_POPULATED_CENTRAL_CODE
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
    "classpath:db/inn-reach-transaction/pre-populate-inn-reach-transaction.sql"
  })
  void processLocalHoldCirculationRequest_invalidAgencyCodes() {
    var transactionHoldDTO = createTransactionHoldDTO();
    transactionHoldDTO.setPatronAgencyCode("abcd1");
    transactionHoldDTO.setItemAgencyCode("abcd2");

    var responseEntity = testRestTemplate.exchange(
      CIRCULATION_OPERATION_ENDPOINT, HttpMethod.PUT,
      new HttpEntity<>(transactionHoldDTO, headers), InnReachResponseDTO.class,
      LOCAL_HOLD_OPERATION, PRE_POPULATED_TRACKING_ID, PRE_POPULATED_CENTRAL_CODE
    );
    var responseBody = responseEntity.getBody();

    assertEquals(BAD_REQUEST, responseEntity.getStatusCode());
    assertNotNull(responseBody);
    assertEquals("The patron and item agencies should be on the same local server", responseBody.getReason());
  }

  private InnReachTransaction fetchPrePopulatedTransaction() {
    return fetchTransactionByTrackingId(PRE_POPULATED_TRACKING_ID);
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
    var transaction = repository.findByTrackingIdAndCentralServerCode(PRE_POPULATED_TRACKING_ID,
      PRE_POPULATED_CENTRAL_CODE).get();
    transaction.setState(ITEM_SHIPPED);
    repository.save(transaction);

    var transactionHoldDTO = createTransactionHoldDTO();

    var responseEntity = testRestTemplate.exchange(
      ITEM_RECEIVED_PATH, HttpMethod.PUT, new HttpEntity<>(transactionHoldDTO, headers), InnReachResponseDTO.class,
      PRE_POPULATED_TRACKING_ID, PRE_POPULATED_CENTRAL_CODE);

    assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
    var responseEntityBody = responseEntity.getBody();
    assertNotNull(responseEntityBody);
    assertEquals("ok", responseEntityBody.getStatus());

    var transactionUpdated = repository.findByTrackingIdAndCentralServerCode(PRE_POPULATED_TRACKING_ID,
      PRE_POPULATED_CENTRAL_CODE).get();
    assertEquals(ITEM_RECEIVED, transactionUpdated.getState());
  }

  @Test
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/inn-reach-transaction/pre-populate-inn-reach-transaction.sql"
  })
  void processItemReceivedRequest_whenItemIsNotShipped() {
    var transactionHoldDTO = createTransactionHoldDTO();

    var responseEntity = testRestTemplate.exchange(
      ITEM_RECEIVED_PATH, HttpMethod.PUT, new HttpEntity<>(transactionHoldDTO, headers), InnReachResponseDTO.class,
      PRE_POPULATED_TRACKING_ID, PRE_POPULATED_CENTRAL_CODE);

    assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
    var responseEntityBody = responseEntity.getBody();
    assertNotNull(responseEntityBody);
    assertEquals(UNEXPECTED_TRANSACTION_STATE + "PATRON_HOLD", responseEntityBody.getReason());

    var transactionUpdated = repository.findByTrackingIdAndCentralServerCode(PRE_POPULATED_TRACKING_ID,
      PRE_POPULATED_CENTRAL_CODE).get();
    assertNotEquals(ITEM_RECEIVED, transactionUpdated.getState());
  }

  @Test
  @Sql(scripts = {
    "classpath:db/inn-reach-recall-user/pre-populate-inn-reach-recall-user.sql",
    "classpath:db/central-server/pre-populate-central-server-with-recall-user.sql",
    "classpath:db/inn-reach-transaction/pre-populate-inn-reach-transaction.sql"
  })
  @Sql(scripts = {
    "classpath:db/inn-reach-recall-user/clear-inn-reach-recall-user.sql"},
    executionPhase = AFTER_TEST_METHOD
  )
  void processRecallRequest_whenItemIsOnLoanToThePatron() {
    when(servicePointsUsersClient.findServicePointsUsers(any())).thenReturn(ResultList.of(1, List.of(createServicePointUserDTO())));
    when(circulationClient.findRequest(any())).thenReturn(Optional.of(new RequestDTO()));
    when(circulationClient.sendRequest(any())).thenReturn(new RequestDTO());
    var recallDTO = createRecallDTO();

    var responseEntity = testRestTemplate.exchange(
      RECALL_REQUEST_PATH, HttpMethod.PUT, new HttpEntity<>(recallDTO, headers), InnReachResponseDTO.class,
      PRE_POPULATED_TRACKING_ID, PRE_POPULATED_CENTRAL_CODE);

    assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
    var responseEntityBody = responseEntity.getBody();
    assertNotNull(responseEntityBody);
    assertEquals("ok", responseEntityBody.getStatus());

    verify(requestService).createRecallRequest(any(), any());
    var transactionUpdated = repository.findByTrackingIdAndCentralServerCode(PRE_POPULATED_TRACKING_ID,
      PRE_POPULATED_CENTRAL_CODE).get();
    assertEquals(RECALL, transactionUpdated.getState());
  }

  @Test
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/inn-reach-transaction/pre-populate-inn-reach-transaction.sql"
  })
  void processRecallRequest_whenItemIsOnTheHoldShelf() {
    var requestDTO = new RequestDTO();
    requestDTO.setStatus(RequestDTO.RequestStatus.OPEN_AWAITING_PICKUP);
    when(circulationClient.findRequest(any())).thenReturn(Optional.of(requestDTO));
    doNothing().when(circulationClient).updateRequest(any(), any());

    var transaction = repository.findByTrackingIdAndCentralServerCode(PRE_POPULATED_TRACKING_ID,
      PRE_POPULATED_CENTRAL_CODE).get();
    transaction.setState(ITEM_SHIPPED);
    transaction.getHold().setFolioLoanId(null);
    repository.save(transaction);

    var recallDTO = createRecallDTO();

    var responseEntity = testRestTemplate.exchange(
      RECALL_REQUEST_PATH, HttpMethod.PUT, new HttpEntity<>(recallDTO, headers), InnReachResponseDTO.class,
      PRE_POPULATED_TRACKING_ID, PRE_POPULATED_CENTRAL_CODE);

    assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
    var responseEntityBody = responseEntity.getBody();
    assertNotNull(responseEntityBody);
    assertEquals("ok", responseEntityBody.getStatus());

    verify(requestService).cancelRequest(any(), eq("Item has been recalled."));
    var transactionUpdated = repository.findByTrackingIdAndCentralServerCode(PRE_POPULATED_TRACKING_ID,
      PRE_POPULATED_CENTRAL_CODE).get();
    assertEquals(RECALL, transactionUpdated.getState());
  }

  @Test
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/inn-reach-transaction/pre-populate-inn-reach-transaction.sql"
  })
  void processRecallRequest_whenBadRequest() {
    var requestDTO = new RequestDTO();
    requestDTO.setStatus(RequestDTO.RequestStatus.OPEN_AWAITING_PICKUP);
    when(circulationClient.findRequest(any())).thenReturn(Optional.of(requestDTO));
    doThrow(new RuntimeException("Test exception.")).when(requestService).cancelRequest(any(), any());

    var recallDTO = createRecallDTO();

    var responseEntity = testRestTemplate.exchange(
      RECALL_REQUEST_PATH, HttpMethod.PUT, new HttpEntity<>(recallDTO, headers), InnReachResponseDTO.class,
      PRE_POPULATED_TRACKING_ID, PRE_POPULATED_CENTRAL_CODE);

    assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
    var responseEntityBody = responseEntity.getBody();
    assertNotNull(responseEntityBody);
    assertTrue(responseEntityBody.getReason().contains("Test exception."));

    var transactionUpdated = repository.findByTrackingIdAndCentralServerCode(PRE_POPULATED_TRACKING_ID,
      PRE_POPULATED_CENTRAL_CODE).get();
    assertNotEquals(RECALL, transactionUpdated.getState());
  }

  @Test
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/inn-reach-transaction/pre-populate-inn-reach-transaction.sql"
  })
  void processRecallRequest_whenRecallUserIsNotSet() {
    when(circulationClient.findRequest(any())).thenReturn(Optional.of(new RequestDTO()));
    var recallDTO = createRecallDTO();

    var responseEntity = testRestTemplate.exchange(
      RECALL_REQUEST_PATH, HttpMethod.PUT, new HttpEntity<>(recallDTO, headers), InnReachResponseDTO.class,
      PRE_POPULATED_TRACKING_ID, PRE_POPULATED_CENTRAL_CODE);

    assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
    var responseEntityBody = responseEntity.getBody();
    assertNotNull(responseEntityBody);
    assertTrue(responseEntityBody.getReason().contains("Recall user is not set for central server with code = " + PRE_POPULATED_CENTRAL_CODE));

    var transactionUpdated = repository.findByTrackingIdAndCentralServerCode(PRE_POPULATED_TRACKING_ID,
      PRE_POPULATED_CENTRAL_CODE).get();
    assertNotEquals(RECALL, transactionUpdated.getState());
  }

  @Test
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/inn-reach-transaction/pre-populate-inn-reach-transaction.sql"
  })
  void borrowerRenewRequestCalculatedDueDateAfterBorrowerDueDate() {
    var loan = new CheckOutResponseDTO();
    loan.setDueDate(new Date(Instant.now().toEpochMilli()));
    when(circulationClient.getLoanById(any())).thenReturn(loan);

    var renew = new CheckOutResponseDTO();
    renew.setDueDate(new Date(Instant.now().plus(1, ChronoUnit.DAYS).toEpochMilli()));
    when(circulationClient.renewLoan(any())).thenReturn(renew);

    var dueDateTime = (int) Instant.now().minus(1, ChronoUnit.DAYS).getEpochSecond();
    var borrowerItem = new BorrowerRenewDTO();
    borrowerItem.setDueDateTime(dueDateTime);

    var transactionHoldDTO = createTransactionHoldDTO();
    borrowerItem.setTransactionTime(transactionHoldDTO.getTransactionTime());
    borrowerItem.setPatronId(transactionHoldDTO.getPatronId());
    borrowerItem.setPatronAgencyCode(transactionHoldDTO.getPatronAgencyCode());
    borrowerItem.setItemAgencyCode(transactionHoldDTO.getItemAgencyCode());
    borrowerItem.setItemId(transactionHoldDTO.getItemId());

    var responseEntity = testRestTemplate.exchange(
      "/inn-reach/d2ir/circ/borrowerrenew/{trackingId}/{centralCode}", HttpMethod.PUT,
      new HttpEntity<>(borrowerItem, headers), BorrowerRenewDTO.class,
      PRE_POPULATED_TRACKING_ID, PRE_POPULATED_CENTRAL_CODE);

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

    var loan = new CheckOutResponseDTO();
    loan.setDueDate(new Date(Instant.now().toEpochMilli()));
    when(circulationClient.getLoanById(any())).thenReturn(loan);

    var renew = new CheckOutResponseDTO();
    renew.setDueDate(new Date(Instant.now().minus(1, ChronoUnit.DAYS).toEpochMilli()));
    when(circulationClient.renewLoan(any())).thenReturn(renew);

    var dueDateTime = (int) Instant.now().plus(1, ChronoUnit.DAYS).getEpochSecond();
    var borrowerItem = new BorrowerRenewDTO();
    borrowerItem.setDueDateTime(dueDateTime);

    var transactionHoldDTO = createTransactionHoldDTO();
    borrowerItem.setTransactionTime(transactionHoldDTO.getTransactionTime());
    borrowerItem.setPatronId(transactionHoldDTO.getPatronId());
    borrowerItem.setPatronAgencyCode(transactionHoldDTO.getPatronAgencyCode());
    borrowerItem.setItemAgencyCode(transactionHoldDTO.getItemAgencyCode());
    borrowerItem.setItemId(transactionHoldDTO.getItemId());

    var responseEntity = testRestTemplate.exchange(
      "/inn-reach/d2ir/circ/borrowerrenew/{trackingId}/{centralCode}", HttpMethod.PUT,
      new HttpEntity<>(borrowerItem, headers), BorrowerRenewDTO.class,
      PRE_POPULATED_TRACKING_ID, PRE_POPULATED_CENTRAL_CODE);

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

    var loan = new CheckOutResponseDTO();
    loan.setDueDate(new Date(Instant.now().toEpochMilli()));
    when(circulationClient.getLoanById(any())).thenReturn(loan);

    when(circulationClient.renewLoan(any())).thenThrow(IllegalArgumentException.class);

    var dueDateTime = (int) Instant.now().plus(1, ChronoUnit.DAYS).getEpochSecond();
    var borrowerItem = new BorrowerRenewDTO();
    borrowerItem.setDueDateTime(dueDateTime);

    var transactionHoldDTO = createTransactionHoldDTO();
    borrowerItem.setTransactionTime(transactionHoldDTO.getTransactionTime());
    borrowerItem.setPatronId(transactionHoldDTO.getPatronId());
    borrowerItem.setPatronAgencyCode(transactionHoldDTO.getPatronAgencyCode());
    borrowerItem.setItemAgencyCode(transactionHoldDTO.getItemAgencyCode());
    borrowerItem.setItemId(transactionHoldDTO.getItemId());

    var responseEntity = testRestTemplate.exchange(
      "/inn-reach/d2ir/circ/borrowerrenew/{trackingId}/{centralCode}", HttpMethod.PUT,
      new HttpEntity<>(borrowerItem, headers), BorrowerRenewDTO.class,
      PRE_POPULATED_TRACKING_ID, PRE_POPULATED_CENTRAL_CODE);

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
    var loan = new CheckOutResponseDTO();
    loan.setDueDate(new Date(Instant.now().toEpochMilli()));
    when(circulationClient.getLoanById(any())).thenReturn(loan);

    when(circulationClient.renewLoan(any())).thenThrow(IllegalArgumentException.class);

    var dueDateTime = (int) Instant.now().getEpochSecond();
    var borrowerItem = new BorrowerRenewDTO();
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
      new HttpEntity<>(borrowerItem, headers), BorrowerRenewDTO.class,
      PRE_POPULATED_TRACKING_ID, PRE_POPULATED_CENTRAL_CODE);

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

    var loan = new CheckOutResponseDTO();
    loan.setDueDate(new Date(Instant.now().toEpochMilli()));
    when(circulationClient.getLoanById(any())).thenReturn(loan);

    var renew = new CheckOutResponseDTO();
    renew.setDueDate(new Date(Instant.now().minus(1, ChronoUnit.DAYS).toEpochMilli()));
    when(circulationClient.renewLoan(any())).thenReturn(renew);

    var dueDateTime = (int) Instant.now().getEpochSecond();
    var borrowerItem = new BorrowerRenewDTO();
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
      new HttpEntity<>(borrowerItem, headers), BorrowerRenewDTO.class,
      PRE_POPULATED_TRACKING_ID, PRE_POPULATED_CENTRAL_CODE);

    var transactionStateAfter = fetchPrePopulatedTransaction().getState();

    assertEquals(BAD_REQUEST, responseEntity.getStatusCode());
    assertEquals(transactionStateBefore, transactionStateAfter);
  }

  @ParameterizedTest
  @EnumSource(names = {"ITEM_IN_TRANSIT","RETURN_UNCIRCULATED"})
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/inn-reach-transaction/pre-populate-inn-reach-transaction.sql"
  })
  void checkTransactionIsInStateItemInTransitOrReturnUncirculated(InnReachTransaction.TransactionState testEnums) {
    var transactionHoldDTO = createTransactionHoldDTO();
    var transactionBefore = fetchPrePopulatedTransaction();

    transactionBefore.setState(testEnums);
    repository.save(transactionBefore);

    var responseEntity = testRestTemplate.exchange(
      "/inn-reach/d2ir/circ/finalcheckin/{trackingId}/{centralCode}", HttpMethod.PUT,
      new HttpEntity<>(transactionHoldDTO), InnReachResponseDTO.class,
      PRE_POPULATED_TRACKING_ID, PRE_POPULATED_CENTRAL_CODE);

    var transactionAfter = fetchPrePopulatedTransaction();

    assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
    assertEquals(FINAL_CHECKIN, transactionAfter.getState());
  }

  @Test
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/inn-reach-transaction/pre-populate-inn-reach-transaction.sql"
  })
  void checkTransactionIsNotInStateItemInTransitOrReturnUncirculated() {
    var transactionHoldDTO = createTransactionHoldDTO();

    var responseEntity = testRestTemplate.exchange(
      "/inn-reach/d2ir/circ/finalcheckin/{trackingId}/{centralCode}", HttpMethod.PUT,
      new HttpEntity<>(transactionHoldDTO), InnReachResponseDTO.class,
      PRE_POPULATED_TRACKING_ID, PRE_POPULATED_CENTRAL_CODE);

    var transaction = fetchPrePopulatedTransaction();

    assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
    assertEquals(PATRON_HOLD, transaction.getState());
  }
}
