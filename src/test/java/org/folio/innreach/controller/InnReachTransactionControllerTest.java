package org.folio.innreach.controller;

import org.folio.innreach.client.InventoryClient;
import org.folio.innreach.client.InventoryStorageClient;
import org.folio.innreach.client.RequestStorageClient;
import org.folio.innreach.client.UsersClient;
import org.folio.innreach.controller.base.BaseControllerTest;
import org.folio.innreach.domain.dto.folio.ResultList;
import org.folio.innreach.domain.dto.folio.inventory.InventoryItemStatus;
import org.folio.innreach.domain.dto.folio.inventorystorage.ServicePointUserDTO;
import org.folio.innreach.domain.dto.folio.inventorystorage.ServicePointsUsersDTO;
import org.folio.innreach.domain.dto.folio.requeststorage.RequestDTO;
import org.folio.innreach.domain.dto.folio.requeststorage.RequestsDTO;
import org.folio.innreach.domain.entity.TransactionItemHold;
import org.folio.innreach.domain.service.InnReachTransactionService;
import org.folio.innreach.dto.InnReachResponseDTO;
import org.folio.innreach.dto.TransactionItemHoldDTO;
import org.folio.innreach.mapper.InnReachTransactionMapper;
import org.folio.innreach.repository.InnReachTransactionRepository;
import org.junit.jupiter.api.Test;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlMergeMode;

import java.util.List;
import java.util.UUID;

import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static org.folio.innreach.domain.entity.InnReachTransaction.TransactionType.ITEM;
import static org.folio.innreach.fixture.InventoryItemFixture.createInventoryItemDTO;
import static org.folio.innreach.fixture.RequestFixture.createRequestDTO;
import static org.folio.innreach.fixture.TestUtil.deserializeFromJsonFile;
import static org.folio.innreach.fixture.UserFixture.createUser;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.AFTER_TEST_METHOD;
import static org.springframework.test.context.jdbc.SqlMergeMode.MergeMode.MERGE;

@Sql(
  scripts = {"classpath:db/inn-reach-transaction/clear-inn-reach-transaction-tables.sql",
    "classpath:db/central-server/clear-central-server-tables.sql"
  },
  executionPhase = AFTER_TEST_METHOD
)
@SqlMergeMode(MERGE)
class InnReachTransactionControllerTest extends BaseControllerTest {

  private static final String TRACKING_ID = "trackingid1";
  private static final String PRE_POPULATED_TRACKING_ID = "tracking1";
  private static final String PRE_POPULATED_CENTRAL_SERVER_CODE = "d2ir";
  private static final String PRE_POPULATED_USER_BARCODE = "0000098765";
  private static final Integer PRE_POPULATED_CENTRAL_PATRON_TYPE = 200;

  @Autowired
  private TestRestTemplate testRestTemplate;
  @Autowired
  private InnReachTransactionRepository repository;
  @Autowired
  private InnReachTransactionMapper mapper;

  @MockBean
  private InventoryClient inventoryClient;
  @MockBean
  private RequestStorageClient requestsClient;
  @MockBean
  private InventoryStorageClient inventoryStorageClient;
  @MockBean
  private UsersClient usersClient;

  @SpyBean
  private InnReachTransactionService service;

  @Test
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql"
  })
  void return200HttpCode_and_createdInnReachTransactionEntity_when_createInnReachTransactionWithItemHold() {
    doNothing().when(service).createItemRequest(anyString());

    var itemHoldDTO = deserializeFromJsonFile(
      "/inn-reach-transaction/create-item-hold-request.json", TransactionItemHoldDTO.class);

    var responseEntity = testRestTemplate.postForEntity(
      "/innreach/v2/circ/itemHold/{trackingId}/{centralCode}", itemHoldDTO, InnReachResponseDTO.class, TRACKING_ID,
      PRE_POPULATED_CENTRAL_SERVER_CODE);

    verify(service).createItemRequest(TRACKING_ID);
    assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
    assertTrue(responseEntity.hasBody());
    assertEquals("ok", responseEntity.getBody().getStatus());

    var createdTransaction = repository.fetchOneByTrackingId(TRACKING_ID);

    assertTrue(createdTransaction.isPresent());
    assertEquals(PRE_POPULATED_CENTRAL_SERVER_CODE, createdTransaction.get().getCentralServerCode());
    assertEquals(ITEM, createdTransaction.get().getType());
    assertEquals(itemHoldDTO.getItemId(), createdTransaction.get().getHold().getItemId());
    assertEquals(itemHoldDTO.getItemAgencyCode(), createdTransaction.get().getHold().getItemAgencyCode());
    assertEquals(mapper.map(itemHoldDTO.getPickupLocation()).getDisplayName(),
      createdTransaction.get().getHold().getPickupLocation().getDisplayName());
    assertEquals(itemHoldDTO.getTransactionTime(), createdTransaction.get().getHold().getTransactionTime());
    assertEquals(itemHoldDTO.getPatronName(), ((TransactionItemHold) createdTransaction.get().getHold()).getPatronName());
  }

  @Test
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/central-patron-type-mapping/pre-populate-central-patron_type-mapping-table.sql.sql"
  })
  @Sql(scripts = {"classpath:db/central-patron-type-mapping/clear-central-patron-type-mapping-table.sql.sql"},
    executionPhase = AFTER_TEST_METHOD)
  @SqlMergeMode(MERGE)
  void return200HttpCode_and_sendRequest_whenItemHoldTransactionCreated() {
    var inventoryItemDTO = createInventoryItemDTO(InventoryItemStatus.IN_TRANSIT, randomUUID(), randomUUID(), randomUUID(), randomUUID());
    when(inventoryClient.getItemById(any(UUID.class))).thenReturn(inventoryItemDTO);
    var requestDTO = createRequestDTO();
    requestDTO.setItemId(inventoryItemDTO.getId());
    var requestsDTO = new RequestsDTO(List.of(requestDTO), 1);
    when(requestsClient.findRequests(any(UUID.class))).thenReturn(requestsDTO);
    var user = createUser();
    user.setBarcode(PRE_POPULATED_USER_BARCODE);
    when(usersClient.query(anyString())).thenReturn(ResultList.of(1, List.of(user)));
    var servicePointUserDTO = new ServicePointUserDTO();
    servicePointUserDTO.setUserId(fromString(user.getId()));
    servicePointUserDTO.setDefaultServicePointId(randomUUID());
    when(inventoryStorageClient.findServicePointsUsers(any(UUID.class))).thenReturn(new ServicePointsUsersDTO(List.of(servicePointUserDTO)));
    when(requestsClient.sendRequest(any(RequestDTO.class))).then((Answer<RequestDTO>) invocationOnMock -> {
      var sentRequest = (RequestDTO) invocationOnMock.getArgument(0);
      sentRequest.setId(randomUUID());
      return sentRequest;
    });

    var itemHoldDTO = deserializeFromJsonFile(
      "/inn-reach-transaction/create-item-hold-request.json", TransactionItemHoldDTO.class);
    itemHoldDTO.setCentralPatronType(PRE_POPULATED_CENTRAL_PATRON_TYPE);
    itemHoldDTO.setItemId(inventoryItemDTO.getId());

    var responseEntity = testRestTemplate.postForEntity(
      "/innreach/v2/circ/itemHold/{trackingId}/{centralCode}", itemHoldDTO, InnReachResponseDTO.class, TRACKING_ID,
      PRE_POPULATED_CENTRAL_SERVER_CODE);

    assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
    verify(inventoryClient).getItemById(itemHoldDTO.getItemId());
    verify(requestsClient).findRequests(itemHoldDTO.getItemId());
    verify(usersClient).query(anyString());
    verify(inventoryStorageClient).findServicePointsUsers(UUID.fromString(user.getId()));
    verify(requestsClient).sendRequest(any());

    var transaction = repository.fetchOneByTrackingId(TRACKING_ID);

    assertNotNull(transaction.get());
    assertEquals(itemHoldDTO.getItemId(), transaction.get().getHold().getFolioItemId());
    assertNotNull(transaction.get().getHold().getFolioRequestId());
    assertEquals(fromString(user.getId()), transaction.get().getHold().getFolioPatronId());
  }

  @Test
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql"
  })
  void return200HttpCode_and_doNotSendRequest_whenItemHoldTransactionCreatedForNotAvailableItem() {
    var inventoryItemDTO = createInventoryItemDTO(InventoryItemStatus.MISSING, randomUUID(), randomUUID(), randomUUID(), randomUUID());
    when(inventoryClient.getItemById(any(UUID.class))).thenReturn(inventoryItemDTO);
    when(requestsClient.findRequests(any(UUID.class))).thenReturn(new RequestsDTO(null, 0));
    var user = createUser();
    user.setBarcode(PRE_POPULATED_USER_BARCODE);
    when(usersClient.query(anyString())).thenReturn(ResultList.of(1, List.of(user)));
    var servicePointUserDTO = new ServicePointUserDTO();
    servicePointUserDTO.setUserId(fromString(user.getId()));
    servicePointUserDTO.setDefaultServicePointId(randomUUID());
    when(inventoryStorageClient.findServicePointsUsers(any(UUID.class))).thenReturn(new ServicePointsUsersDTO(List.of(servicePointUserDTO)));
    when(requestsClient.sendRequest(any(RequestDTO.class))).then((Answer<RequestDTO>) invocationOnMock -> {
      var sentRequest = (RequestDTO) invocationOnMock.getArgument(0);
      sentRequest.setId(randomUUID());
      return sentRequest;
    });

    var itemHoldDTO = deserializeFromJsonFile(
      "/inn-reach-transaction/create-item-hold-request.json", TransactionItemHoldDTO.class);
    itemHoldDTO.setCentralPatronType(PRE_POPULATED_CENTRAL_PATRON_TYPE);
    itemHoldDTO.setItemId(inventoryItemDTO.getId());

    var responseEntity = testRestTemplate.postForEntity(
      "/innreach/v2/circ/itemHold/{trackingId}/{centralCode}", itemHoldDTO, InnReachResponseDTO.class, TRACKING_ID,
      PRE_POPULATED_CENTRAL_SERVER_CODE);

    assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
    verify(requestsClient, never()).sendRequest(any());

    var transaction = repository.fetchOneByTrackingId(TRACKING_ID);
    assertNotNull(transaction.get());
    assertNull(transaction.get().getHold().getFolioPatronId());
    assertNull(transaction.get().getHold().getFolioItemId());
    assertNull(transaction.get().getHold().getFolioRequestId());
  }

  @Test
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql"
  })
  void return400HttpCode_when_createInnReachTransactionWithItemHoldWithInvalidPatronId() {
    doNothing().when(service).createItemRequest(anyString());

    var itemHoldDTO = deserializeFromJsonFile(
      "/inn-reach-transaction/create-item-hold-invalid-patron-id-request.json", TransactionItemHoldDTO.class);

    var responseEntity = testRestTemplate.postForEntity(
      "/innreach/v2/circ/itemHold/{trackingId}/{centralCode}", itemHoldDTO, InnReachResponseDTO.class, TRACKING_ID,
      PRE_POPULATED_CENTRAL_SERVER_CODE);

    assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
    assertEquals("failed", responseEntity.getBody().getStatus());
    assertThat(responseEntity.getBody().getReason(), containsString("must match \"[a-z,0-9]{1,32}\""));
  }

  @Test
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql"
  })
  void return400HttpCode_when_createInnReachTransactionWithItemHoldWithInvalidCentralItemType() {
    doNothing().when(service).createItemRequest(anyString());

    var itemHoldDTO = deserializeFromJsonFile(
      "/inn-reach-transaction/create-item-hold-invalid-central-item-type-request.json", TransactionItemHoldDTO.class);

    var responseEntity = testRestTemplate.postForEntity(
      "/innreach/v2/circ/itemHold/{trackingId}/{centralCode}", itemHoldDTO, InnReachResponseDTO.class, TRACKING_ID,
      PRE_POPULATED_CENTRAL_SERVER_CODE);

    assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
    assertEquals("failed", responseEntity.getBody().getStatus());
    assertThat(responseEntity.getBody().getReason(), containsString("must be less than or equal to 255"));
  }

  @Test
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/inn-reach-transaction/pre-populate-inn-reach-transaction.sql",
  })
  void return400HttpCode_when_createInnReachTransaction_and_trackingIdAlreadyExists() {
    doNothing().when(service).createItemRequest(anyString());

    var itemHoldDTO = deserializeFromJsonFile(
      "/inn-reach-transaction/create-item-hold-request.json", TransactionItemHoldDTO.class);

    var responseEntity = testRestTemplate.postForEntity(
      "/innreach/v2/circ/itemHold/{trackingId}/{centralCode}", itemHoldDTO, InnReachResponseDTO.class, PRE_POPULATED_TRACKING_ID,
      PRE_POPULATED_CENTRAL_SERVER_CODE);

    assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
    assertEquals("failed", responseEntity.getBody().getStatus());
    assertThat(responseEntity.getBody().getReason(), containsString("constraint [unq_tracking_id]"));
  }

  @Test
  void return400HttpCode_when_createInnReachTransaction_and_centralServerDoesNotExist() {
    doNothing().when(service).createItemRequest(anyString());

    var itemHoldDTO = deserializeFromJsonFile(
      "/inn-reach-transaction/create-item-hold-request.json", TransactionItemHoldDTO.class);

    var responseEntity = testRestTemplate.postForEntity(
      "/innreach/v2/circ/itemHold/{trackingId}/{centralCode}", itemHoldDTO, InnReachResponseDTO.class, PRE_POPULATED_TRACKING_ID,
      PRE_POPULATED_CENTRAL_SERVER_CODE);

    assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
    assertEquals("failed", responseEntity.getBody().getStatus());
    assertEquals("Central server with code: d2ir not found", responseEntity.getBody().getReason());
  }

  @Test
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql"
  })
  void return400HttpCode_when_createInnReachTransaction_and_pickupLocationIsNotValid() {
    doNothing().when(service).createItemRequest(anyString());

    var itemHoldDTO = deserializeFromJsonFile(
      "/inn-reach-transaction/create-item-hold-invalid-pickup-location-request.json", TransactionItemHoldDTO.class);

    var responseEntity = testRestTemplate.postForEntity(
      "/innreach/v2/circ/itemHold/{trackingId}/{centralCode}", itemHoldDTO, InnReachResponseDTO.class, PRE_POPULATED_TRACKING_ID,
      PRE_POPULATED_CENTRAL_SERVER_CODE);

    assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
    assertEquals("failed", responseEntity.getBody().getStatus());
    assertEquals("Pickup location must consist of 3 or 4 strings delimited by a colon.", responseEntity.getBody().getReason());
  }
}
