package org.folio.innreach.controller;

import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.AFTER_TEST_METHOD;
import static org.springframework.test.context.jdbc.SqlMergeMode.MergeMode.MERGE;

import static org.folio.innreach.domain.dto.folio.inventory.InventoryItemStatus.AVAILABLE;
import static org.folio.innreach.domain.dto.folio.inventory.InventoryItemStatus.IN_TRANSIT;
import static org.folio.innreach.domain.dto.folio.inventory.InventoryItemStatus.MISSING;
import static org.folio.innreach.domain.dto.folio.inventory.InventoryItemStatus.UNAVAILABLE;
import static org.folio.innreach.domain.entity.InnReachTransaction.TransactionType.ITEM;
import static org.folio.innreach.fixture.InventoryItemFixture.createInventoryItemDTO;
import static org.folio.innreach.fixture.RequestFixture.createRequestDTO;
import static org.folio.innreach.fixture.TestUtil.deserializeFromJsonFile;
import static org.folio.innreach.fixture.UserFixture.createUser;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlMergeMode;

import org.folio.innreach.client.InventoryClient;
import org.folio.innreach.client.RequestStorageClient;
import org.folio.innreach.client.ServicePointsUsersClient;
import org.folio.innreach.client.UsersClient;
import org.folio.innreach.controller.base.BaseControllerTest;
import org.folio.innreach.domain.dto.OwningSiteCancelsRequestDTO;
import org.folio.innreach.domain.dto.folio.ResultList;
import org.folio.innreach.domain.dto.folio.User;
import org.folio.innreach.domain.dto.folio.inventory.InventoryItemDTO;
import org.folio.innreach.domain.dto.folio.inventorystorage.ServicePointUserDTO;
import org.folio.innreach.domain.dto.folio.requeststorage.RequestDTO;
import org.folio.innreach.domain.entity.TransactionItemHold;
import org.folio.innreach.domain.service.RequestService;
import org.folio.innreach.dto.InnReachResponseDTO;
import org.folio.innreach.dto.InnReachTransactionDTO;
import org.folio.innreach.dto.TransactionHoldDTO;
import org.folio.innreach.external.client.feign.InnReachClient;
import org.folio.innreach.mapper.InnReachTransactionPickupLocationMapper;
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
class InnReachTransactionControllerTest extends BaseControllerTest {

  private static final String TRACKING_ID = "trackingid1";
  private static final String PRE_POPULATED_TRACKING_ID = "tracking1";
  private static final String PRE_POPULATED_CENTRAL_SERVER_CODE = "d2ir";
  private static final String PRE_POPULATED_CENTRAL_SERVER_ID = "edab6baf-c696-42b1-89bb-1bbb8759b0d2";
  private static final String PRE_POPULATED_USER_BARCODE = "0000098765";
  private static final String PRE_POPULATED_USER_BARCODE_QUERY = "(barcode==\"" + PRE_POPULATED_USER_BARCODE + "\")";
  private static final Integer PRE_POPULATED_CENTRAL_PATRON_TYPE = 200;
  private static final String PRE_POPULATED_MATERIAL_TYPE_ID = "1a54b431-2e4f-452d-9cae-9cee66c9a892";

  public static final String TRANSACTION_WITH_ITEM_HOLD_ID = "ab2393a1-acc4-4849-82ac-8cc0c37339e1";
  public static final String TRANSACTION_WITH_LOCAL_HOLD_ID = "79b0a1fb-55be-4e55-9d84-01303aaec1ce";
  public static final String TRANSACTION_WITH_PATRON_HOLD_ID = "0aab1720-14b4-4210-9a19-0d0bf1cd64d3";

  @Autowired
  private TestRestTemplate testRestTemplate;
  @Autowired
  private InnReachTransactionRepository repository;
  @Autowired
  private InnReachTransactionPickupLocationMapper transactionPickupLocationMapper;

  @MockBean
  private InventoryClient inventoryClient;
  @MockBean
  private RequestStorageClient requestsClient;
  @MockBean
  private ServicePointsUsersClient servicePointsUsersClient;
  @MockBean
  private UsersClient usersClient;
  @MockBean
  private InnReachClient innReachClient;

  @SpyBean
  private RequestService requestService;

  InventoryItemDTO mockInventoryClient(){
    var inventoryItemDTO = createInventoryItemDTO();
    inventoryItemDTO.setStatus(AVAILABLE);
    inventoryItemDTO.setMaterialType(new InventoryItemDTO.MaterialType(UUID.fromString(PRE_POPULATED_MATERIAL_TYPE_ID), "materialType"));
    when(inventoryClient.getItemsByHrId(inventoryItemDTO.getHrId())).thenReturn(ResultList.of(1, List.of(inventoryItemDTO)));
    return inventoryItemDTO;
  }

  User mockUserClient(){
    var user = createUser();
    user.setBarcode(PRE_POPULATED_USER_BARCODE);
    when(usersClient.query(PRE_POPULATED_USER_BARCODE_QUERY)).thenReturn(ResultList.of(1, List.of(user)));
    return user;
  }

  void mockInventoryStorageClient(User user){
    var servicePointUserDTO = new ServicePointUserDTO();
    servicePointUserDTO.setUserId(fromString(user.getId()));
    servicePointUserDTO.setDefaultServicePointId(randomUUID());
    when(servicePointsUsersClient.findServicePointsUsers(UUID.fromString(user.getId()))).thenReturn(ResultList.of(1, List.of(servicePointUserDTO)));
  }

  void mockFindRequestsReturnsEmptyList(InventoryItemDTO inventoryItemDTO) {
    when(requestsClient.findRequests(inventoryItemDTO.getId())).thenReturn(ResultList.of(0, Collections.emptyList()));
  }

  @Test
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/mtype-mapping/pre-populate-material-type-mapping.sql",
    "classpath:db/central-patron-type-mapping/pre-populate-central-patron_type-mapping-table.sql"
  })
  @Sql(scripts = {"classpath:db/central-patron-type-mapping/clear-central-patron-type-mapping-table.sql"},
    executionPhase = AFTER_TEST_METHOD)
  @SqlMergeMode(MERGE)
  void return200HttpCode_and_sendRequest_whenItemHoldTransactionCreated() {
    var inventoryItemDTO = mockInventoryClient();
    inventoryItemDTO.setStatus(IN_TRANSIT);
    var requestDTO = createRequestDTO();
    requestDTO.setItemId(inventoryItemDTO.getId());
    when(requestsClient.findRequests(inventoryItemDTO.getId())).thenReturn(ResultList.of(1,
      List.of(requestDTO)));
    var user = mockUserClient();
    mockInventoryStorageClient(user);
    when(requestsClient.sendRequest(any(RequestDTO.class))).then((Answer<RequestDTO>) invocationOnMock -> {
      var sentRequest = (RequestDTO) invocationOnMock.getArgument(0);
      sentRequest.setId(randomUUID());
      return sentRequest;
    });

    var itemHoldDTO = deserializeFromJsonFile(
      "/inn-reach-transaction/create-item-hold-request.json", TransactionHoldDTO.class);
    itemHoldDTO.setCentralPatronType(PRE_POPULATED_CENTRAL_PATRON_TYPE);
    itemHoldDTO.setItemId(inventoryItemDTO.getHrId());

    var responseEntity = testRestTemplate.postForEntity(
      "/inn-reach/d2ir/circ/itemhold/{trackingId}/{centralCode}", itemHoldDTO, InnReachResponseDTO.class, TRACKING_ID,
      PRE_POPULATED_CENTRAL_SERVER_CODE);

    verify(requestService).createItemRequest(TRACKING_ID);
    assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
    assertTrue(responseEntity.hasBody());
    assertEquals("ok", responseEntity.getBody().getStatus());

    await().until(() -> repository.fetchOneByTrackingId(TRACKING_ID).get().getHold().getFolioItemId() != null);

    verify(requestService).createItemRequest(TRACKING_ID);
    verify(inventoryClient, times(2)).getItemsByHrId(itemHoldDTO.getItemId());
    verify(requestsClient).findRequests(inventoryItemDTO.getId());
    verify(usersClient).query(PRE_POPULATED_USER_BARCODE_QUERY);
    verify(servicePointsUsersClient).findServicePointsUsers(fromString(user.getId()));
    verify(requestsClient).sendRequest(any());

    var transaction = repository.fetchOneByTrackingId(TRACKING_ID);

    assertTrue(transaction.isPresent());
    assertEquals(PRE_POPULATED_CENTRAL_SERVER_CODE, transaction.get().getCentralServerCode());
    assertEquals(ITEM, transaction.get().getType());
    assertEquals(itemHoldDTO.getItemId(), transaction.get().getHold().getItemId());
    assertEquals(itemHoldDTO.getItemAgencyCode(), transaction.get().getHold().getItemAgencyCode());
    assertEquals(transactionPickupLocationMapper.fromString(itemHoldDTO.getPickupLocation()).getDisplayName(),
      transaction.get().getHold().getPickupLocation().getDisplayName());
    assertEquals(itemHoldDTO.getTransactionTime(), transaction.get().getHold().getTransactionTime());
    assertEquals(itemHoldDTO.getPatronName(), ((TransactionItemHold) transaction.get().getHold()).getPatronName());

    assertEquals(inventoryItemDTO.getId(), transaction.get().getHold().getFolioItemId());
    assertNotNull(transaction.get().getHold().getFolioRequestId());
    assertEquals(fromString(user.getId()), transaction.get().getHold().getFolioPatronId());
  }

  @Test
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/mtype-mapping/pre-populate-material-type-mapping.sql",
    "classpath:db/central-patron-type-mapping/pre-populate-central-patron_type-mapping-table.sql"
  })
  void return200HttpCode_and_doNotSendRequest_whenItemHoldTransactionCreatedForNotAvailableItem() {
    var inventoryItemDTO = mockInventoryClient();
    inventoryItemDTO.setStatus(MISSING);
    mockFindRequestsReturnsEmptyList(inventoryItemDTO);
    var user = mockUserClient();
    mockInventoryStorageClient(user);
    when(requestsClient.sendRequest(any(RequestDTO.class))).then((Answer<RequestDTO>) invocationOnMock -> {
      var sentRequest = (RequestDTO) invocationOnMock.getArgument(0);
      sentRequest.setId(randomUUID());
      return sentRequest;
    });

    var itemHoldDTO = deserializeFromJsonFile(
      "/inn-reach-transaction/create-item-hold-request.json", TransactionHoldDTO.class);
    itemHoldDTO.setCentralPatronType(PRE_POPULATED_CENTRAL_PATRON_TYPE);
    itemHoldDTO.setItemId(inventoryItemDTO.getHrId());

    var responseEntity = testRestTemplate.postForEntity(
      "/inn-reach/d2ir/circ/itemhold/{trackingId}/{centralCode}", itemHoldDTO, InnReachResponseDTO.class, TRACKING_ID,
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
    "classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/mtype-mapping/pre-populate-material-type-mapping.sql"
  })
  void return400HttpCode_when_createInnReachTransactionWithItemHoldWithInvalidPatronId() {
    var inventoryItemDTO = mockInventoryClient();

    var itemHoldDTO = deserializeFromJsonFile(
      "/inn-reach-transaction/create-item-hold-invalid-patron-id-request.json", TransactionHoldDTO.class);
    itemHoldDTO.setItemId(inventoryItemDTO.getHrId());

    var responseEntity = testRestTemplate.postForEntity(
      "/inn-reach/d2ir/circ/itemhold/{trackingId}/{centralCode}", itemHoldDTO, InnReachResponseDTO.class, TRACKING_ID,
      PRE_POPULATED_CENTRAL_SERVER_CODE);

    assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
    assertEquals("failed", responseEntity.getBody().getStatus());
    assertEquals("Argument validation failed.", responseEntity.getBody().getReason());
    assertThat(responseEntity.getBody().getErrors().get(0).getReason(), containsString("must match \"[a-z,0-9]{1,32}\""));
  }

  @Test
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/mtype-mapping/pre-populate-material-type-mapping.sql",
    "classpath:db/inn-reach-transaction/pre-populate-inn-reach-transaction.sql",
  })
  void return400HttpCode_when_createInnReachTransaction_and_trackingIdAlreadyExists() {
    var inventoryItemDTO = mockInventoryClient();

    var itemHoldDTO = deserializeFromJsonFile(
      "/inn-reach-transaction/create-item-hold-request.json", TransactionHoldDTO.class);
    itemHoldDTO.setItemId(inventoryItemDTO.getHrId());

    var responseEntity = testRestTemplate.postForEntity(
      "/inn-reach/d2ir/circ/itemhold/{trackingId}/{centralCode}", itemHoldDTO, InnReachResponseDTO.class, PRE_POPULATED_TRACKING_ID,
      PRE_POPULATED_CENTRAL_SERVER_CODE);

    assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
    assertEquals("failed", responseEntity.getBody().getStatus());

    assertEquals("An error occurred during creation of INN-Reach Transaction.", responseEntity.getBody().getReason());
    assertEquals("INN-Reach Transaction with tracking ID = tracking1 already exists.", responseEntity.getBody().getErrors().get(0).getReason());
  }

  @Test
  void return400HttpCode_when_createInnReachTransaction_and_centralServerDoesNotExist() {
    var inventoryItemDTO = mockInventoryClient();

    var itemHoldDTO = deserializeFromJsonFile(
      "/inn-reach-transaction/create-item-hold-request.json", TransactionHoldDTO.class);
    itemHoldDTO.setItemId(inventoryItemDTO.getHrId());

    var responseEntity = testRestTemplate.postForEntity(
      "/inn-reach/d2ir/circ/itemhold/{trackingId}/{centralCode}", itemHoldDTO, InnReachResponseDTO.class, PRE_POPULATED_TRACKING_ID,
      PRE_POPULATED_CENTRAL_SERVER_CODE);

    assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
    assertEquals("failed", responseEntity.getBody().getStatus());
    assertEquals("An error occurred during creation of INN-Reach Transaction.", responseEntity.getBody().getReason());
    assertEquals("Central server with code: d2ir not found", responseEntity.getBody().getErrors().get(0).getReason());
  }

  @Test
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/mtype-mapping/pre-populate-material-type-mapping.sql"
  })
  void return400HttpCode_when_createInnReachTransaction_and_pickupLocationIsNotValid() {
    var inventoryItemDTO = mockInventoryClient();

    var itemHoldDTO = deserializeFromJsonFile(
      "/inn-reach-transaction/create-item-hold-invalid-pickup-location-request.json", TransactionHoldDTO.class);
    itemHoldDTO.setItemId(inventoryItemDTO.getHrId());

    var responseEntity = testRestTemplate.postForEntity(
      "/inn-reach/d2ir/circ/itemhold/{trackingId}/{centralCode}", itemHoldDTO, InnReachResponseDTO.class, PRE_POPULATED_TRACKING_ID,
      PRE_POPULATED_CENTRAL_SERVER_CODE);

    assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
    assertEquals("failed", responseEntity.getBody().getStatus());
    assertEquals("An error occurred during creation of INN-Reach Transaction.", responseEntity.getBody().getReason());
    assertEquals("Pickup location must consist of 3 or 4 strings delimited by a colon.", responseEntity.getBody().getErrors().get(0).getReason());
  }

  @Test
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql"
  })
  void return400HttpCode_when_createInnReachTransactionWithInvalidMaterialType() {
    var inventoryItemDTO = mockInventoryClient();

    var itemHoldDTO = deserializeFromJsonFile(
      "/inn-reach-transaction/create-item-hold-request.json", TransactionHoldDTO.class);
    itemHoldDTO.setItemId(inventoryItemDTO.getHrId());

    var responseEntity = testRestTemplate.postForEntity(
      "/inn-reach/d2ir/circ/itemhold/{trackingId}/{centralCode}", itemHoldDTO, InnReachResponseDTO.class, TRACKING_ID,
      PRE_POPULATED_CENTRAL_SERVER_CODE);

    assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
    assertEquals("failed", responseEntity.getBody().getStatus());
    assertEquals("An error occurred during creation of INN-Reach Transaction.", responseEntity.getBody().getReason());
    assertEquals("Material type mapping for central server id = "
      + PRE_POPULATED_CENTRAL_SERVER_ID + " and material type id = " + PRE_POPULATED_MATERIAL_TYPE_ID + " not found",
      responseEntity.getBody().getErrors().get(0).getReason());
  }

  @Test
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/mtype-mapping/pre-populate-material-type-mapping.sql",
    "classpath:db/central-patron-type-mapping/pre-populate-central-patron_type-mapping-table.sql"
  })
  void issueOwningSideCancelsRequest_when_createInnReachTransaction_and_creatingRequestFails() {
    var inventoryItemDTO = mockInventoryClient();
    mockFindRequestsReturnsEmptyList(inventoryItemDTO);
    var user = mockUserClient();
    when(servicePointsUsersClient.findServicePointsUsers(fromString(user.getId()))).thenThrow(IllegalStateException.class);
    when(innReachClient.postInnReachApi(any(), anyString(), anyString(), anyString(), any())).thenReturn("response");

    var itemHoldDTO = deserializeFromJsonFile(
      "/inn-reach-transaction/create-item-hold-request.json", TransactionHoldDTO.class);
    itemHoldDTO.setItemId(inventoryItemDTO.getHrId());
    itemHoldDTO.setCentralPatronType(PRE_POPULATED_CENTRAL_PATRON_TYPE);

    var responseEntity = testRestTemplate.postForEntity(
      "/inn-reach/d2ir/circ/itemhold/{trackingId}/{centralCode}", itemHoldDTO, InnReachResponseDTO.class, TRACKING_ID,
      PRE_POPULATED_CENTRAL_SERVER_CODE);

    assertEquals(HttpStatus.OK, responseEntity.getStatusCode());

    await().untilAsserted(() -> verify(innReachClient).postInnReachApi(any(), anyString(), anyString(), anyString(), any()));

    verify(inventoryClient).getItemsByHrId(inventoryItemDTO.getHrId());
    verify(requestsClient, never()).findRequests(inventoryItemDTO.getId());
    verify(usersClient).query(PRE_POPULATED_USER_BARCODE_QUERY);
    verify(servicePointsUsersClient).findServicePointsUsers(fromString(user.getId()));
    verify(requestsClient, never()).sendRequest(any());

    var cancelRequest = ArgumentCaptor.forClass(OwningSiteCancelsRequestDTO.class);
    verify(innReachClient).postInnReachApi(any(), anyString(), anyString(), anyString(), cancelRequest.capture());
    assertEquals("Request not permitted", cancelRequest.getValue().getReason());
  }

  @Test
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/mtype-mapping/pre-populate-material-type-mapping.sql",
    "classpath:db/central-patron-type-mapping/pre-populate-central-patron_type-mapping-table.sql"
  })
  void issueOwningSideCancelsRequest_when_createInnReachTransaction_and_itemIsNotRequestable() {
    var inventoryItemDTO = mockInventoryClient();
    inventoryItemDTO.setStatus(UNAVAILABLE);
    mockFindRequestsReturnsEmptyList(inventoryItemDTO);
    var user = mockUserClient();
    mockInventoryStorageClient(user);
    when(innReachClient.postInnReachApi(any(), anyString(), anyString(), anyString(), any())).thenReturn("response");

    var itemHoldDTO = deserializeFromJsonFile(
      "/inn-reach-transaction/create-item-hold-request.json", TransactionHoldDTO.class);
    itemHoldDTO.setItemId(inventoryItemDTO.getHrId());
    itemHoldDTO.setCentralPatronType(PRE_POPULATED_CENTRAL_PATRON_TYPE);

    var responseEntity = testRestTemplate.postForEntity(
      "/inn-reach/d2ir/circ/itemhold/{trackingId}/{centralCode}", itemHoldDTO, InnReachResponseDTO.class, PRE_POPULATED_TRACKING_ID,
      PRE_POPULATED_CENTRAL_SERVER_CODE);

    assertEquals(HttpStatus.OK, responseEntity.getStatusCode());

    await().untilAsserted(() -> verify(innReachClient).postInnReachApi(any(), anyString(), anyString(), anyString(), any()));

    verify(inventoryClient, times(2)).getItemsByHrId(inventoryItemDTO.getHrId());
    verify(requestsClient).findRequests(inventoryItemDTO.getId());
    verify(usersClient).query(PRE_POPULATED_USER_BARCODE_QUERY);
    verify(servicePointsUsersClient).findServicePointsUsers(fromString(user.getId()));
    verify(requestsClient, never()).sendRequest(any());

    var request = ArgumentCaptor.forClass(OwningSiteCancelsRequestDTO.class);
    verify(innReachClient).postInnReachApi(any(), anyString(), anyString(), anyString(), request.capture());
    assertEquals("Item not available", request.getValue().getReason());
  }

  @Test
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/inn-reach-transaction/pre-populate-inn-reach-transaction.sql",
  })
  void returnInnReachTransactionWithPatronHold_when_transactionExists() {
    var responseEntity = testRestTemplate.getForEntity("/inn-reach/transactions/{transactionId}",
      InnReachTransactionDTO.class, TRANSACTION_WITH_PATRON_HOLD_ID);

    assertEquals(HttpStatus.OK, responseEntity.getStatusCode());

    var responseBody = responseEntity.getBody();

    assertNotNull(responseBody);
    assertNotNull(responseBody.getTransactionHold().getTitle());
    assertNotNull(responseBody.getTransactionHold().getAuthor());
    assertNotNull(responseBody.getTransactionHold().getCallNumber());
    assertNotNull(responseBody.getTransactionHold().getShippedItemBarcode());
  }

  @Test
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/inn-reach-transaction/pre-populate-inn-reach-transaction.sql",
  })
  void returnInnReachTransactionWithItemHold_when_transactionExists() {
    var responseEntity = testRestTemplate.getForEntity("/inn-reach/transactions/{transactionId}",
      InnReachTransactionDTO.class, TRANSACTION_WITH_ITEM_HOLD_ID);

    assertEquals(HttpStatus.OK, responseEntity.getStatusCode());

    var responseBody = responseEntity.getBody();

    assertNotNull(responseBody);
    assertNotNull(responseBody.getTransactionHold().getCentralPatronType());
    assertNotNull(responseBody.getTransactionHold().getPatronName());
  }

  @Test
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/inn-reach-transaction/pre-populate-inn-reach-transaction.sql",
  })
  void returnInnReachTransactionWithLocalHold_when_transactionExists() {
    var responseEntity = testRestTemplate.getForEntity("/inn-reach/transactions/{transactionId}",
      InnReachTransactionDTO.class, TRANSACTION_WITH_LOCAL_HOLD_ID);

    assertEquals(HttpStatus.OK, responseEntity.getStatusCode());

    var responseBody = responseEntity.getBody();

    assertNotNull(responseBody);
    assertNotNull(responseBody.getTransactionHold().getPatronHomeLibrary());
    assertNotNull(responseBody.getTransactionHold().getTitle());
    assertNotNull(responseBody.getTransactionHold().getAuthor());
    assertNotNull(responseBody.getTransactionHold().getCallNumber());
    assertNotNull(responseBody.getTransactionHold().getCentralPatronType());
    assertNotNull(responseBody.getTransactionHold().getPatronName());
  }

}
