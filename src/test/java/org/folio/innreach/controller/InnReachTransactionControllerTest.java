package org.folio.innreach.controller;

import static java.util.UUID.randomUUID;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.AFTER_TEST_METHOD;
import static org.springframework.test.context.jdbc.SqlMergeMode.MergeMode.MERGE;

import static org.folio.innreach.domain.dto.folio.circulation.RequestDTO.RequestStatus.CLOSED_CANCELLED;
import static org.folio.innreach.domain.dto.folio.inventory.InventoryItemStatus.AVAILABLE;
import static org.folio.innreach.domain.dto.folio.inventory.InventoryItemStatus.IN_PROCESS;
import static org.folio.innreach.domain.dto.folio.inventory.InventoryItemStatus.IN_TRANSIT;
import static org.folio.innreach.domain.dto.folio.inventory.InventoryItemStatus.MISSING;
import static org.folio.innreach.domain.dto.folio.inventory.InventoryItemStatus.UNAVAILABLE;
import static org.folio.innreach.domain.entity.InnReachTransaction.TransactionState.CANCEL_REQUEST;
import static org.folio.innreach.domain.entity.InnReachTransaction.TransactionState.FINAL_CHECKIN;
import static org.folio.innreach.domain.entity.InnReachTransaction.TransactionState.ITEM_RECEIVED;
import static org.folio.innreach.domain.entity.InnReachTransaction.TransactionState.ITEM_SHIPPED;
import static org.folio.innreach.domain.entity.InnReachTransaction.TransactionState.RECEIVE_UNANNOUNCED;
import static org.folio.innreach.domain.entity.InnReachTransaction.TransactionState.RETURN_UNCIRCULATED;
import static org.folio.innreach.domain.entity.InnReachTransaction.TransactionState.TRANSFER;
import static org.folio.innreach.dto.TransactionStateEnum.PATRON_HOLD;
import static org.folio.innreach.dto.TransactionTypeEnum.ITEM;
import static org.folio.innreach.dto.TransactionTypeEnum.LOCAL;
import static org.folio.innreach.dto.TransactionTypeEnum.PATRON;
import static org.folio.innreach.fixture.InnReachTransactionFixture.createInnReachTransaction;
import static org.folio.innreach.fixture.InventoryFixture.createInventoryItemDTO;
import static org.folio.innreach.fixture.RequestFixture.createRequestDTO;
import static org.folio.innreach.fixture.TestUtil.circHeaders;
import static org.folio.innreach.fixture.TestUtil.deserializeFromJsonFile;
import static org.folio.innreach.fixture.UserFixture.createUser;
import static org.folio.innreach.util.DateHelper.toEpochSec;

import java.time.Duration;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.stubbing.Answer;
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
import org.folio.innreach.client.HoldingsStorageClient;
import org.folio.innreach.client.InventoryClient;
import org.folio.innreach.client.RequestPreferenceStorageClient;
import org.folio.innreach.client.ServicePointsUsersClient;
import org.folio.innreach.client.UsersClient;
import org.folio.innreach.controller.base.BaseControllerTest;
import org.folio.innreach.domain.dto.OwningSiteCancelsRequestDTO;
import org.folio.innreach.domain.dto.folio.ResultList;
import org.folio.innreach.domain.dto.folio.User;
import org.folio.innreach.domain.dto.folio.circulation.RequestDTO;
import org.folio.innreach.domain.dto.folio.inventory.InventoryItemDTO;
import org.folio.innreach.domain.dto.folio.inventory.InventoryItemStatus;
import org.folio.innreach.domain.dto.folio.requestpreference.RequestPreferenceDTO;
import org.folio.innreach.domain.entity.InnReachTransaction;
import org.folio.innreach.domain.entity.TransactionItemHold;
import org.folio.innreach.domain.entity.base.AuditableUser;
import org.folio.innreach.domain.service.RequestService;
import org.folio.innreach.domain.service.impl.InnReachTransactionActionNotifier;
import org.folio.innreach.dto.CancelPatronHoldDTO;
import org.folio.innreach.dto.CheckInRequestDTO;
import org.folio.innreach.dto.CheckInResponseDTO;
import org.folio.innreach.dto.CheckInResponseDTOItem;
import org.folio.innreach.dto.CheckOutRequestDTO;
import org.folio.innreach.dto.InnReachResponseDTO;
import org.folio.innreach.dto.InnReachTransactionDTO;
import org.folio.innreach.dto.InnReachTransactionsDTO;
import org.folio.innreach.dto.LoanDTO;
import org.folio.innreach.dto.LoanItem;
import org.folio.innreach.dto.LoanStatus;
import org.folio.innreach.dto.PatronHoldCheckInResponseDTO;
import org.folio.innreach.dto.TransactionCheckOutResponseDTO;
import org.folio.innreach.dto.TransactionHoldDTO;
import org.folio.innreach.dto.TransactionStateEnum;
import org.folio.innreach.external.client.feign.InnReachClient;
import org.folio.innreach.mapper.InnReachTransactionMapper;
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
@TestInstance(TestInstance.Lifecycle.PER_METHOD)
class InnReachTransactionControllerTest extends BaseControllerTest {

  private static final String PATRON_HOLD_CHECK_IN_ENDPOINT = "/inn-reach/transactions/{id}/receive-item/{servicePointId}";
  private static final String PATRON_HOLD_CHECK_IN_UNSHIPPED_ENDPOINT = "/inn-reach/transactions/{id}/receive-unshipped-item/{servicePointId}/{itemBarcode}";
  private static final String ITEM_HOLD_CHECK_OUT_ENDPOINT = "/inn-reach/transactions/{itemBarcode}/check-out-item/{servicePointId}";
  private static final String PATRON_HOLD_CHECK_OUT_ENDPOINT = "/inn-reach/transactions/{id}/patronhold/check-out-item/{servicePointId}";
  private static final String UPDATE_TRANSACTION_ENDPOINT = "/inn-reach/transactions/{transactionId}";
  private static final String PATRON_HOLD_CANCEL_ENDPOINT = "/inn-reach/transactions/{id}/patronhold/cancel";
  private static final String PATRON_HOLD_RETURN_ITEM_ENDPOINT = "/inn-reach/transactions/{id}/patronhold/return-item/{servicePointId}";

  private static final String TRACKING_ID = "trackingid1";
  private static final String PRE_POPULATED_TRACKING_ID = "tracking1";
  private static final String PRE_POPULATED_CENTRAL_SERVER_CODE = "d2ir";
  private static final String PRE_POPULATED_CENTRAL_SERVER_ID = "edab6baf-c696-42b1-89bb-1bbb8759b0d2";
  private static final String PRE_POPULATED_USER_BARCODE = "0000098765";
  private static final String PRE_POPULATED_USER_BARCODE_QUERY = "(barcode==\"" + PRE_POPULATED_USER_BARCODE + "\")";
  private static final Integer PRE_POPULATED_CENTRAL_PATRON_TYPE = 200;
  private static final String PRE_POPULATED_MATERIAL_TYPE_ID = "1a54b431-2e4f-452d-9cae-9cee66c9a892";
  private static final String NEW_CENTRAL_SERVER_CODE = "a0aa";
  private static final String NEW_ITEM_AND_AGENCY_CODE = "a0aa0";
  private static final String NEW_TEST_PARAMETER_VALUE = "abc";

  public static final String TRANSACTION_WITH_ITEM_HOLD_ID = "ab2393a1-acc4-4849-82ac-8cc0c37339e1";
  public static final String TRANSACTION_WITH_LOCAL_HOLD_ID = "79b0a1fb-55be-4e55-9d84-01303aaec1ce";
  public static final String TRANSACTION_WITH_PATRON_HOLD_ID = "0aab1720-14b4-4210-9a19-0d0bf1cd64d3";

  private static final UUID PRE_POPULATED_PATRON_HOLD_TRANSACTION_ID = UUID.fromString("0aab1720-14b4-4210-9a19-0d0bf1cd64d3");
  private static final UUID PRE_POPULATED_ITEM_HOLD_TRANSACTION_ID = UUID.fromString("ab2393a1-acc4-4849-82ac-8cc0c37339e1");
  private static final UUID PRE_POPULATED_TRANSACTION_ID3 = UUID.fromString("79b0a1fb-55be-4e55-9d84-01303aaec1ce");
  private static final UUID PRE_POPULATED_ITEM_SHIPPED_TRANSACTION_ID = UUID.fromString("7106c3ac-890a-4126-bf9b-a10b67555b6e");
  private static final String PRE_POPULATED_PATRON_HOLD_ITEM_BARCODE = "1111111";
  private static final String PRE_POPULATED_ITEM_HOLD_ITEM_BARCODE = "DEF-def-5678";
  private static final String PRE_POPULATED_CENTRAL_PATRON_ID2 = "u6ct3wssbnhxvip3sobwmxvhoa";
  private static final UUID PRE_POPULATED_PATRON_HOLD_REQUEST_ID = UUID.fromString("ea11eba7-3c0f-4d15-9cca-c8608cd6bc8a");
  private static final UUID PRE_POPULATED_PATRON_HOLD_ITEM_ID = UUID.fromString("9a326225-6530-41cc-9399-a61987bfab3c");
  private static final UUID FOLIO_CHECKOUT_ID = UUID.randomUUID();

  private static final AuditableUser PRE_POPULATED_USER = AuditableUser.SYSTEM;

  private static final Duration ASYNC_AWAIT_TIMEOUT = Duration.ofSeconds(15);

  @Autowired
  private TestRestTemplate testRestTemplate;
  @SpyBean
  private InnReachTransactionRepository repository;
  @Autowired
  private InnReachTransactionPickupLocationMapper transactionPickupLocationMapper;
  @Autowired
  private InnReachTransactionMapper innReachTransactionMapper;

  @MockBean
  private InventoryClient inventoryClient;
  @MockBean
  private CirculationClient circulationClient;
  @MockBean
  private ServicePointsUsersClient servicePointsUsersClient;
  @MockBean
  private UsersClient usersClient;
  @MockBean
  private InnReachClient innReachClient;
  @MockBean
  private HoldingsStorageClient holdingsStorageClient;
  @MockBean
  private RequestPreferenceStorageClient requestPreferenceClient;
  @SpyBean
  private RequestService requestService;
  @SpyBean
  private InnReachTransactionActionNotifier actionNotifier;

  private static HttpHeaders headers = circHeaders();

  InventoryItemDTO mockInventoryClient() {
    var inventoryItemDTO = createInventoryItemDTO();
    inventoryItemDTO.setStatus(AVAILABLE);
    inventoryItemDTO.setMaterialType(new InventoryItemDTO.MaterialType(UUID.fromString(PRE_POPULATED_MATERIAL_TYPE_ID), "materialType"));
    when(inventoryClient.getItemsByHrId(inventoryItemDTO.getHrid())).thenReturn(ResultList.of(1, List.of(inventoryItemDTO)));
    return inventoryItemDTO;
  }

  User mockUserClient() {
    var user = createUser();
    user.setBarcode(PRE_POPULATED_USER_BARCODE);
    when(usersClient.query(PRE_POPULATED_USER_BARCODE_QUERY)).thenReturn(ResultList.of(1, List.of(user)));
    return user;
  }

  void mockFindRequestsReturnsEmptyList(InventoryItemDTO inventoryItemDTO) {
    when(circulationClient.queryRequestsByItemId(inventoryItemDTO.getId())).thenReturn(ResultList.of(0, Collections.emptyList()));
  }

  void modifyTransactionsDateCreated() {
    var transactions = repository.findAll();
    int[] hours = {0};
    transactions.forEach(t -> t.setCreatedDate(t.getCreatedDate().minusHours(hours[0]++)));
    repository.saveAll(transactions);
  }

  @Test
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/inn-reach-transaction/pre-populate-inn-reach-transaction.sql"
  })
  void return200HttpCode_and_allExistingTransactions_when_getAllTransactionsWithNoFilters() {
    var responseEntity = testRestTemplate.getForEntity(
      "/inn-reach/transactions", InnReachTransactionsDTO.class
    );

    assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
    assertNotNull(responseEntity.getBody());
    assertEquals(3, responseEntity.getBody().getTotalRecords());

    var transactionIds = responseEntity.getBody().getTransactions().stream()
      .map(InnReachTransactionDTO::getId).collect(Collectors.toList());
    assertTrue(transactionIds.containsAll(
      List.of(PRE_POPULATED_PATRON_HOLD_TRANSACTION_ID, PRE_POPULATED_ITEM_HOLD_TRANSACTION_ID, PRE_POPULATED_TRANSACTION_ID3)));

    var transactionMetadatas = responseEntity.getBody().getTransactions().stream()
      .map(InnReachTransactionDTO::getMetadata).collect(Collectors.toList());
    assertTrue(transactionMetadatas.stream().allMatch(Objects::nonNull));
    assertTrue(transactionMetadatas.stream().allMatch(m -> m.getCreatedDate() != null));
    assertTrue(transactionMetadatas.stream().allMatch(m -> m.getCreatedByUsername().equals(PRE_POPULATED_USER.getName())));
  }

  @Test
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/inn-reach-transaction/pre-populate-inn-reach-transaction.sql"
  })
  void return200HttpCode_and_pageOfTransactions_when_getAllTransactionsWithOffsetAndLimit() {
    var responseEntity = testRestTemplate.getForEntity(
      "/inn-reach/transactions?offset=2&limit=2", InnReachTransactionsDTO.class
    );

    assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
    assertNotNull(responseEntity.getBody());
    assertEquals(3, responseEntity.getBody().getTotalRecords());

    var transactions = responseEntity.getBody().getTransactions();

    assertEquals(1, transactions.size());
  }

  @Test
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/inn-reach-transaction/pre-populate-inn-reach-transaction.sql"
  })
  void return200HttpCode_and_sortedTransactions_when_getAllTransactionsWithTypeAndState() {
    var responseEntity = testRestTemplate.getForEntity(
      "/inn-reach/transactions?type=PATRON&type=ITEM&state=PATRON_HOLD", InnReachTransactionsDTO.class
    );

    assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
    assertNotNull(responseEntity.getBody());
    assertEquals(1, responseEntity.getBody().getTotalRecords());

    var transactionIds = responseEntity.getBody().getTransactions().stream()
      .map(InnReachTransactionDTO::getId).collect(Collectors.toList());
    assertTrue(transactionIds.contains(UUID.fromString(TRANSACTION_WITH_PATRON_HOLD_ID)));

    assertEquals(1, responseEntity.getBody().getTransactions().size());
    var transaction = responseEntity.getBody().getTransactions().stream()
      .findFirst().get();
    assertEquals(PATRON, transaction.getType());
    assertEquals(PATRON_HOLD, transaction.getState());
  }

  @Test
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/inn-reach-transaction/pre-populate-inn-reach-transaction.sql"
  })
  void return200HttpCode_and_sortedTransactions_when_getAllTransactionsWithCentralServerCodeAndPatronAgencyCode() {
    var responseEntity = testRestTemplate.getForEntity(
      "/inn-reach/transactions?centralServerCode=d2ir&patronAgencyCode=qwe56", InnReachTransactionsDTO.class
    );

    assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
    assertNotNull(responseEntity.getBody());
    assertEquals(1, responseEntity.getBody().getTotalRecords());

    var transactionIds = responseEntity.getBody().getTransactions().stream()
      .map(InnReachTransactionDTO::getId).collect(Collectors.toList());
    assertTrue(transactionIds.contains(PRE_POPULATED_ITEM_HOLD_TRANSACTION_ID));

    assertEquals(1, responseEntity.getBody().getTransactions().size());
    assertTrue(responseEntity.getBody().getTransactions().stream().map(InnReachTransactionDTO::getCentralServerCode)
      .allMatch(c -> c.equals("d2ir")));

    var holdDTOs = responseEntity.getBody().getTransactions().stream()
      .map(InnReachTransactionDTO::getHold).collect(Collectors.toList());
    assertTrue(holdDTOs.stream().allMatch(h -> h.getPatronAgencyCode().equals("qwe56")));
  }

  @Test
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/inn-reach-transaction/pre-populate-inn-reach-transaction.sql"
  })
  void return200HttpCode_and_sortedTransactions_when_getAllTransactionsWithItemAgency() {
    var responseEntity = testRestTemplate.getForEntity(
      "/inn-reach/transactions?itemAgencyCode=asd78", InnReachTransactionsDTO.class
    );

    assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
    assertNotNull(responseEntity.getBody());
    assertEquals(2, responseEntity.getBody().getTotalRecords());

    var transactionIds = responseEntity.getBody().getTransactions().stream()
      .map(InnReachTransactionDTO::getId).collect(Collectors.toList());
    assertTrue(transactionIds.containsAll(List.of(UUID.fromString(TRANSACTION_WITH_ITEM_HOLD_ID),
      UUID.fromString(TRANSACTION_WITH_LOCAL_HOLD_ID))));

    assertEquals(2, responseEntity.getBody().getTransactions().size());
    var holdDTOs = responseEntity.getBody().getTransactions().stream()
      .map(InnReachTransactionDTO::getHold).collect(Collectors.toList());
    assertTrue(holdDTOs.stream().allMatch(h -> h.getItemAgencyCode().equals("asd78")));
  }

  @Test
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/inn-reach-transaction/pre-populate-inn-reach-transaction.sql"
  })
  void return200HttpCode_and_sortedTransactions_when_getAllTransactionsWithPatronType() {
    var responseEntity = testRestTemplate.getForEntity(
      "/inn-reach/transactions?centralPatronType=1&centralPatronType=0", InnReachTransactionsDTO.class
    );

    assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
    assertNotNull(responseEntity.getBody());
    assertEquals(2, responseEntity.getBody().getTotalRecords());

    var transactionIds = responseEntity.getBody().getTransactions().stream()
      .map(InnReachTransactionDTO::getId).collect(Collectors.toList());
    assertTrue(transactionIds.containsAll(List.of(UUID.fromString(TRANSACTION_WITH_ITEM_HOLD_ID),
      UUID.fromString(TRANSACTION_WITH_LOCAL_HOLD_ID))));

    assertEquals(2, responseEntity.getBody().getTransactions().size());
    var centralPatronTypes = responseEntity.getBody().getTransactions().stream()
      .map(InnReachTransactionDTO::getHold).map(TransactionHoldDTO::getCentralPatronType).collect(Collectors.toList());
    assertTrue(centralPatronTypes.containsAll(List.of(0, 1)));
  }

  @Test
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/inn-reach-transaction/pre-populate-inn-reach-transaction.sql"
  })
  void return200HttpCode_and_sortedTransactions_when_getAllTransactionsWithCentralItemType() {
    var responseEntity = testRestTemplate.getForEntity(
      "/inn-reach/transactions?centralItemType=1&centralItemType=2", InnReachTransactionsDTO.class
    );

    assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
    assertNotNull(responseEntity.getBody());
    assertEquals(2, responseEntity.getBody().getTotalRecords());

    var transactionIds = responseEntity.getBody().getTransactions().stream()
      .map(InnReachTransactionDTO::getId).collect(Collectors.toList());
    assertTrue(transactionIds.containsAll(List.of(PRE_POPULATED_PATRON_HOLD_TRANSACTION_ID, PRE_POPULATED_ITEM_HOLD_TRANSACTION_ID)));

    assertEquals(2, responseEntity.getBody().getTransactions().size());
    var centralItemTypes = responseEntity.getBody().getTransactions().stream()
      .map(InnReachTransactionDTO::getHold).map(TransactionHoldDTO::getCentralItemType).collect(Collectors.toList());
    assertTrue(centralItemTypes.containsAll(List.of(1, 2)));
  }

  @Test
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/inn-reach-transaction/pre-populate-inn-reach-transaction.sql"
  })
  void return200HttpCode_and_emptyTransactionList_when_noTransactionsMatchFilters() {
    var responseEntity = testRestTemplate.getForEntity(
      "/inn-reach/transactions?type=ITEM&state=PATRON_HOLD&centralServerCode=qwe12", InnReachTransactionsDTO.class
    );

    assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
    assertNotNull(responseEntity.getBody());
    assertEquals(0, responseEntity.getBody().getTotalRecords());
    assertTrue(responseEntity.getBody().getTransactions().isEmpty());
  }

  @Test
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/inn-reach-transaction/pre-populate-inn-reach-transaction.sql"
  })
  void return200HttpCode_and_sortedTransactionList_when_SortByCreatedDateDescending() {
    modifyTransactionsDateCreated();
    var responseEntity = testRestTemplate.getForEntity(
      "/inn-reach/transactions?sortBy=createdDate&sortOrder=desc", InnReachTransactionsDTO.class
    );

    assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
    assertNotNull(responseEntity.getBody());
    assertEquals(3, responseEntity.getBody().getTotalRecords());

    var transactions = responseEntity.getBody().getTransactions();
    assertTrue(transactions.get(0).getMetadata().getCreatedDate().after(
      transactions.get(2).getMetadata().getCreatedDate()));
  }

  @Test
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/inn-reach-transaction/pre-populate-inn-reach-transaction.sql"
  })
  void return200HttpCode_and_sortedTransactionList_when_SortByCentralItemTypeAscending() {
    var responseEntity = testRestTemplate.getForEntity(
      "/inn-reach/transactions?sortBy=centralItemType", InnReachTransactionsDTO.class
    );

    assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
    assertNotNull(responseEntity.getBody());
    assertEquals(3, responseEntity.getBody().getTotalRecords());

    var transactions = responseEntity.getBody().getTransactions();
    assertTrue(transactions.get(0).getHold().getCentralItemType() <
      transactions.get(2).getHold().getCentralItemType());
  }

  @Test
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/inn-reach-transaction/pre-populate-inn-reach-transaction.sql"
  })
  void return200HttpCode_and_sortedTransactionList_when_SortByCentralPatronType() {
    var responseEntity = testRestTemplate.getForEntity(
      "/inn-reach/transactions?sortBy=centralPatronType", InnReachTransactionsDTO.class
    );

    assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
    assertNotNull(responseEntity.getBody());
    assertEquals(3, responseEntity.getBody().getTotalRecords());

    var transactions = responseEntity.getBody().getTransactions();
    assertTrue(transactions.get(0).getHold().getCentralPatronType() <
      transactions.get(1).getHold().getCentralPatronType());
  }

  @Test
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/inn-reach-transaction/pre-populate-inn-reach-transaction.sql"
  })
  void return200HttpCode_and_sortedTransactionList_when_getTransactionsWithItemBarcode() {
    var responseEntity = testRestTemplate.getForEntity(
      "/inn-reach/transactions?query=ABC-abc-1234", InnReachTransactionsDTO.class
    );

    assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
    assertNotNull(responseEntity.getBody());
    assertEquals(1, responseEntity.getBody().getTotalRecords());

    var transactions = responseEntity.getBody().getTransactions();
    assertEquals(1, transactions.size());
    assertEquals("ABC-abc-1234", transactions.get(0).getHold().getFolioItemBarcode());
  }

  @Test
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/inn-reach-transaction/pre-populate-inn-reach-transaction.sql"
  })
  void return200HttpCode_and_sortedTransactionList_when_getTransactionsWithItemTitle() {
    var responseEntity = testRestTemplate.getForEntity(
      "/inn-reach/transactions?query=TITLE", InnReachTransactionsDTO.class
    );

    assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
    assertNotNull(responseEntity.getBody());
    assertEquals(3, responseEntity.getBody().getTotalRecords());

    var titles = responseEntity.getBody().getTransactions().stream()
      .map(InnReachTransactionDTO::getHold).map(TransactionHoldDTO::getTitle).collect(Collectors.toList());
    assertEquals(3, titles.size());
    assertTrue(titles.stream().allMatch(t -> t.toLowerCase().contains("title")));
  }

  @Test
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/inn-reach-transaction/pre-populate-inn-reach-transaction.sql"
  })
  void return200HttpCode_and_sortedTransactionList_when_getTransactionsWithTrackingId() {
    var responseEntity = testRestTemplate.getForEntity(
      "/inn-reach/transactions?query=tracking1", InnReachTransactionsDTO.class
    );

    assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
    assertNotNull(responseEntity.getBody());
    assertEquals(1, responseEntity.getBody().getTotalRecords());

    var transactions = responseEntity.getBody().getTransactions();
    assertEquals(1, transactions.size());
    assertEquals("tracking1", transactions.get(0).getTrackingId());
  }

  @Test
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/inn-reach-transaction/pre-populate-inn-reach-transaction.sql"
  })
  void return200HttpCode_and_sortedTransactionList_when_getTransactionsWithPatronId() {
    var responseEntity = testRestTemplate.getForEntity(
      "/inn-reach/transactions?query=" + PRE_POPULATED_CENTRAL_PATRON_ID2, InnReachTransactionsDTO.class
    );

    assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
    assertNotNull(responseEntity.getBody());
    assertEquals(1, responseEntity.getBody().getTotalRecords());

    var transactions = responseEntity.getBody().getTransactions();
    assertEquals(1, transactions.size());
    assertEquals(PRE_POPULATED_CENTRAL_PATRON_ID2, transactions.get(0).getHold().getPatronId());
  }

  @Test
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/inn-reach-transaction/pre-populate-inn-reach-transaction.sql",
    "classpath:db/inn-reach-transaction/pre-populate-another-inn-reach-transaction.sql"
  })
  void return200HttpCode_and_sortedTransactionList_when_getTransactionsWithMultipleFiltersAndSorted() {
    var responseEntity = testRestTemplate.getForEntity(
      "/inn-reach/transactions?type=ITEM&type=LOCAL&centralServerCode=d2ir1&itemAgencyCode=asd78&sortBy=centralPatronType", InnReachTransactionsDTO.class
    );

    assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
    assertNotNull(responseEntity.getBody());
    assertEquals(2, responseEntity.getBody().getTotalRecords());

    var transactions = responseEntity.getBody().getTransactions();
    assertEquals(2, transactions.size());
    assertTrue(transactions.stream().allMatch(t -> t.getType().equals(ITEM) ||
      t.getType().equals(LOCAL)));
    assertTrue(transactions.stream().allMatch(t -> t.getCentralServerCode().equals("d2ir1")));
    assertTrue(transactions.stream().allMatch(t -> t.getHold().getItemAgencyCode().equals("asd78")));
    assertSame(transactions.get(0).getHold().getCentralPatronType(),
      transactions.get(1).getHold().getCentralPatronType());
  }

  @Test
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/inn-reach-transaction/pre-populate-inn-reach-transaction.sql",
    "classpath:db/inn-reach-transaction/pre-populate-another-inn-reach-transaction.sql"
  })
  void return200HttpCode_and_sortedTransactionList_when_getTransactionsWithMultipleFiltersAndPaged() {
    var responseEntity = testRestTemplate.getForEntity(
      "/inn-reach/transactions?state=PATRON_HOLD&state=LOCAL_HOLD&patronAgencyCode=qwe12&query=2&limit=1&offset=1", InnReachTransactionsDTO.class
    );

    assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
    assertNotNull(responseEntity.getBody());
    assertEquals(2, responseEntity.getBody().getTotalRecords());

    var transactions = responseEntity.getBody().getTransactions();
    assertEquals(1, transactions.size());
    var transaction = transactions.get(0);
    assertTrue(transaction.getType().equals(PATRON) || transaction.getType().equals(LOCAL));
    assertEquals("qwe12", transaction.getHold().getPatronAgencyCode());
    assertTrue(transaction.getHold().getAuthor().contains("2"));
  }

  @Test
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/mtype-mapping/pre-populate-material-type-mapping.sql",
    "classpath:db/central-patron-type-mapping/pre-populate-central-patron_type-mapping-table.sql"
  })
  void return200HttpCode_and_sendRequest_whenItemHoldTransactionCreated() {
    var inventoryItemDTO = mockInventoryClient();
    inventoryItemDTO.setStatus(IN_TRANSIT);
    var requestDTO = createRequestDTO();
    requestDTO.setItemId(inventoryItemDTO.getId());
    when(circulationClient.queryRequestsByItemId(inventoryItemDTO.getId())).thenReturn(ResultList.of(1,
      List.of(requestDTO)));
    var user = mockUserClient();
    var requestPreference = new RequestPreferenceDTO(user.getId(), randomUUID());
    when(requestPreferenceClient.getUserRequestPreference(user.getId())).thenReturn(ResultList.of(1, List.of(requestPreference)));
    when(circulationClient.sendRequest(any(RequestDTO.class))).then((Answer<RequestDTO>) invocationOnMock -> {
      var sentRequest = (RequestDTO) invocationOnMock.getArgument(0);
      sentRequest.setId(randomUUID());
      return sentRequest;
    });

    var itemHoldDTO = deserializeFromJsonFile(
      "/inn-reach-transaction/create-item-hold-request.json", TransactionHoldDTO.class);
    itemHoldDTO.setCentralPatronType(PRE_POPULATED_CENTRAL_PATRON_TYPE);
    itemHoldDTO.setItemId(inventoryItemDTO.getHrid());

    var responseEntity = testRestTemplate.postForEntity(
      "/inn-reach/d2ir/circ/itemhold/{trackingId}/{centralCode}", new HttpEntity<>(itemHoldDTO, headers), InnReachResponseDTO.class, TRACKING_ID,
      PRE_POPULATED_CENTRAL_SERVER_CODE);

    assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
    assertTrue(responseEntity.hasBody());
    assertEquals("ok", responseEntity.getBody().getStatus());

    await().atMost(ASYNC_AWAIT_TIMEOUT).untilAsserted(() ->
      verify(repository, atLeastOnce()).save(
        argThat((InnReachTransaction t) -> t.getHold().getFolioRequestId() != null)));

    verify(requestService).createItemHoldRequest(TRACKING_ID, PRE_POPULATED_CENTRAL_SERVER_CODE);
    verify(inventoryClient, times(2)).getItemsByHrId(itemHoldDTO.getItemId());
    verify(circulationClient).queryRequestsByItemId(inventoryItemDTO.getId());
    verify(usersClient).query(PRE_POPULATED_USER_BARCODE_QUERY);
    verify(requestPreferenceClient).getUserRequestPreference(user.getId());
    verify(circulationClient).sendRequest(any());

    var transaction = repository.fetchOneByTrackingId(TRACKING_ID);

    assertTrue(transaction.isPresent());
    assertEquals(PRE_POPULATED_CENTRAL_SERVER_CODE, transaction.get().getCentralServerCode());
    assertEquals(InnReachTransaction.TransactionType.ITEM, transaction.get().getType());
    assertEquals(itemHoldDTO.getItemId(), transaction.get().getHold().getItemId());
    assertEquals(itemHoldDTO.getItemAgencyCode(), transaction.get().getHold().getItemAgencyCode());
    assertEquals(transactionPickupLocationMapper.fromString(itemHoldDTO.getPickupLocation()).getDisplayName(),
      transaction.get().getHold().getPickupLocation().getDisplayName());
    assertEquals(itemHoldDTO.getTransactionTime(), transaction.get().getHold().getTransactionTime());
    assertEquals(itemHoldDTO.getPatronName(), ((TransactionItemHold) transaction.get().getHold()).getPatronName());

    assertEquals(inventoryItemDTO.getId(), transaction.get().getHold().getFolioItemId());
    assertEquals(inventoryItemDTO.getTitle(), transaction.get().getHold().getTitle());
    assertNotNull(transaction.get().getHold().getFolioRequestId());
    assertEquals(user.getId(), transaction.get().getHold().getFolioPatronId());
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
    when(circulationClient.sendRequest(any(RequestDTO.class))).then((Answer<RequestDTO>) invocationOnMock -> {
      var sentRequest = (RequestDTO) invocationOnMock.getArgument(0);
      sentRequest.setId(randomUUID());
      return sentRequest;
    });

    var itemHoldDTO = deserializeFromJsonFile(
      "/inn-reach-transaction/create-item-hold-request.json", TransactionHoldDTO.class);
    itemHoldDTO.setCentralPatronType(PRE_POPULATED_CENTRAL_PATRON_TYPE);
    itemHoldDTO.setItemId(inventoryItemDTO.getHrid());

    var responseEntity = testRestTemplate.postForEntity(
      "/inn-reach/d2ir/circ/itemhold/{trackingId}/{centralCode}", new HttpEntity<>(itemHoldDTO, headers), InnReachResponseDTO.class, TRACKING_ID,
      PRE_POPULATED_CENTRAL_SERVER_CODE);

    assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
    verify(circulationClient, never()).sendRequest(any());

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
    itemHoldDTO.setItemId(inventoryItemDTO.getHrid());

    var responseEntity = testRestTemplate.postForEntity(
      "/inn-reach/d2ir/circ/itemhold/{trackingId}/{centralCode}", new HttpEntity<>(itemHoldDTO, headers), InnReachResponseDTO.class, TRACKING_ID,
      PRE_POPULATED_CENTRAL_SERVER_CODE);

    assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
    assertEquals("failed", responseEntity.getBody().getStatus());
    assertEquals("Argument validation failed", responseEntity.getBody().getReason());
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
    itemHoldDTO.setItemId(inventoryItemDTO.getHrid());

    var responseEntity = testRestTemplate.postForEntity(
      "/inn-reach/d2ir/circ/itemhold/{trackingId}/{centralCode}", new HttpEntity<>(itemHoldDTO, headers), InnReachResponseDTO.class, PRE_POPULATED_TRACKING_ID,
      PRE_POPULATED_CENTRAL_SERVER_CODE);

    assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
    assertEquals("failed", responseEntity.getBody().getStatus());

    assertTrue(responseEntity.getBody().getReason().contains("INN-Reach Transaction with tracking ID = tracking1 already exists."));
  }

  @Test
  void return400HttpCode_when_createInnReachTransaction_and_centralServerDoesNotExist() {
    var inventoryItemDTO = mockInventoryClient();

    var itemHoldDTO = deserializeFromJsonFile(
      "/inn-reach-transaction/create-item-hold-request.json", TransactionHoldDTO.class);
    itemHoldDTO.setItemId(inventoryItemDTO.getHrid());

    var responseEntity = testRestTemplate.postForEntity(
      "/inn-reach/d2ir/circ/itemhold/{trackingId}/{centralCode}", new HttpEntity<>(itemHoldDTO, headers), InnReachResponseDTO.class, PRE_POPULATED_TRACKING_ID,
      PRE_POPULATED_CENTRAL_SERVER_CODE);

    assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
    assertEquals("failed", responseEntity.getBody().getStatus());
    assertTrue(responseEntity.getBody().getReason().contains("Central server with code: d2ir not found"));
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
    itemHoldDTO.setItemId(inventoryItemDTO.getHrid());

    var responseEntity = testRestTemplate.postForEntity(
      "/inn-reach/d2ir/circ/itemhold/{trackingId}/{centralCode}", new HttpEntity<>(itemHoldDTO, headers), InnReachResponseDTO.class, PRE_POPULATED_TRACKING_ID,
      PRE_POPULATED_CENTRAL_SERVER_CODE);

    assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
    assertEquals("failed", responseEntity.getBody().getStatus());
    assertTrue(responseEntity.getBody().getReason().contains("Pickup location must consist of 3 or 4 strings delimited by a colon."));
  }

  @Test
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql"
  })
  void return400HttpCode_when_createInnReachTransactionWithInvalidMaterialType() {
    var inventoryItemDTO = mockInventoryClient();

    var itemHoldDTO = deserializeFromJsonFile(
      "/inn-reach-transaction/create-item-hold-request.json", TransactionHoldDTO.class);
    itemHoldDTO.setItemId(inventoryItemDTO.getHrid());

    var responseEntity = testRestTemplate.postForEntity(
      "/inn-reach/d2ir/circ/itemhold/{trackingId}/{centralCode}", new HttpEntity<>(itemHoldDTO, headers), InnReachResponseDTO.class, TRACKING_ID,
      PRE_POPULATED_CENTRAL_SERVER_CODE);

    assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
    assertEquals("failed", responseEntity.getBody().getStatus());
    assertTrue(responseEntity.getBody().getReason()
      .contains("Material type mapping for central server id = " + PRE_POPULATED_CENTRAL_SERVER_ID +
        " and material type id = " + PRE_POPULATED_MATERIAL_TYPE_ID + " not found"));
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
    when(requestPreferenceClient.getUserRequestPreference(user.getId())).thenThrow(IllegalStateException.class);
    when(innReachClient.postInnReachApi(any(), anyString(), anyString(), anyString())).thenReturn("response");

    var itemHoldDTO = deserializeFromJsonFile(
      "/inn-reach-transaction/create-item-hold-request.json", TransactionHoldDTO.class);
    itemHoldDTO.setItemId(inventoryItemDTO.getHrid());
    itemHoldDTO.setCentralPatronType(PRE_POPULATED_CENTRAL_PATRON_TYPE);

    var responseEntity = testRestTemplate.postForEntity(
      "/inn-reach/d2ir/circ/itemhold/{trackingId}/{centralCode}", new HttpEntity<>(itemHoldDTO, headers), InnReachResponseDTO.class, TRACKING_ID,
      PRE_POPULATED_CENTRAL_SERVER_CODE);

    assertEquals(HttpStatus.OK, responseEntity.getStatusCode());

    await().untilAsserted(() -> verify(innReachClient).postInnReachApi(any(), anyString(), anyString(), anyString(), any()));

    verify(inventoryClient).getItemsByHrId(inventoryItemDTO.getHrid());
    verify(circulationClient, never()).queryRequestsByItemId(inventoryItemDTO.getId());
    verify(usersClient).query(PRE_POPULATED_USER_BARCODE_QUERY);
    verify(requestPreferenceClient).getUserRequestPreference(user.getId());
    verify(circulationClient, never()).sendRequest(any());

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
    var requestPreference = new RequestPreferenceDTO(user.getId(), randomUUID());
    when(requestPreferenceClient.getUserRequestPreference(user.getId())).thenReturn(ResultList.of(1, List.of(requestPreference)));
    when(innReachClient.postInnReachApi(any(), anyString(), anyString(), anyString())).thenReturn("response");
    when(holdingsStorageClient.findHolding(any())).thenReturn(Optional.empty());

    var itemHoldDTO = deserializeFromJsonFile(
      "/inn-reach-transaction/create-item-hold-request.json", TransactionHoldDTO.class);
    itemHoldDTO.setItemId(inventoryItemDTO.getHrid());
    itemHoldDTO.setCentralPatronType(PRE_POPULATED_CENTRAL_PATRON_TYPE);

    var responseEntity = testRestTemplate.postForEntity(
      "/inn-reach/d2ir/circ/itemhold/{trackingId}/{centralCode}", new HttpEntity<>(itemHoldDTO, headers), InnReachResponseDTO.class, PRE_POPULATED_TRACKING_ID,
      PRE_POPULATED_CENTRAL_SERVER_CODE);

    assertEquals(HttpStatus.OK, responseEntity.getStatusCode());

    await().untilAsserted(() -> verify(innReachClient).postInnReachApi(any(), anyString(), anyString(), anyString(), any()));

    verify(inventoryClient, times(2)).getItemsByHrId(inventoryItemDTO.getHrid());
    verify(circulationClient).queryRequestsByItemId(inventoryItemDTO.getId());
    verify(usersClient).query(PRE_POPULATED_USER_BARCODE_QUERY);
    verify(requestPreferenceClient).getUserRequestPreference(user.getId());
    verify(circulationClient, never()).sendRequest(any());

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
    assertNotNull(responseBody.getHold().getTitle());
    assertNotNull(responseBody.getHold().getAuthor());
    assertNotNull(responseBody.getHold().getCallNumber());
    assertNotNull(responseBody.getHold().getShippedItemBarcode());
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
    assertNotNull(responseBody.getHold().getCentralPatronType());
    assertNotNull(responseBody.getHold().getPatronName());
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
    assertNotNull(responseBody.getHold().getPatronHomeLibrary());
    assertNotNull(responseBody.getHold().getTitle());
    assertNotNull(responseBody.getHold().getAuthor());
    assertNotNull(responseBody.getHold().getCallNumber());
    assertNotNull(responseBody.getHold().getCentralPatronType());
    assertNotNull(responseBody.getHold().getPatronName());
  }

  @Test
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/inn-reach-transaction/pre-populate-transaction-item-shipped.sql"
  })
  void testCheckInPatronHoldItem() {
    var requestDTO = new RequestDTO();
    when(circulationClient.findRequest(any())).thenReturn(Optional.of(requestDTO));
    when(circulationClient.checkInByBarcode(any(CheckInRequestDTO.class)))
      .thenReturn(new CheckInResponseDTO().item(new CheckInResponseDTOItem().barcode(PRE_POPULATED_PATRON_HOLD_ITEM_BARCODE)));

    var responseEntity = testRestTemplate.postForEntity(
      PATRON_HOLD_CHECK_IN_ENDPOINT, null, PatronHoldCheckInResponseDTO.class,
      PRE_POPULATED_ITEM_SHIPPED_TRANSACTION_ID, UUID.randomUUID()
    );

    var response = responseEntity.getBody();
    assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
    assertNotNull(response);

    var transaction = response.getTransaction();
    assertEquals(PRE_POPULATED_ITEM_SHIPPED_TRANSACTION_ID, transaction.getId());

    var checkInResponse = response.getFolioCheckIn();
    assertEquals(PRE_POPULATED_PATRON_HOLD_ITEM_BARCODE, checkInResponse.getItem().getBarcode());

    assertFalse(response.getBarcodeAugmented());

    var updatedTransaction = repository.findById(PRE_POPULATED_ITEM_SHIPPED_TRANSACTION_ID).get();
    assertEquals(ITEM_RECEIVED, updatedTransaction.getState());
  }

  @Test
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/inn-reach-transaction/pre-populate-transaction-item-shipped.sql"
  })
  void testCheckInPatronHoldItem_whenItemIsShippedAndRequestIsCancelled() {
    when(innReachClient.postInnReachApi(any(), anyString(), anyString(), anyString())).thenReturn("test");

    when(circulationClient.findRequest(any())).thenReturn(Optional.of(createCancelledRequest()));

    when(circulationClient.checkInByBarcode(any(CheckInRequestDTO.class)))
      .thenReturn(new CheckInResponseDTO().item(new CheckInResponseDTOItem().barcode(PRE_POPULATED_PATRON_HOLD_ITEM_BARCODE)));

    var responseEntity = testRestTemplate.postForEntity(
      PATRON_HOLD_CHECK_IN_ENDPOINT, null, PatronHoldCheckInResponseDTO.class,
      PRE_POPULATED_ITEM_SHIPPED_TRANSACTION_ID, UUID.randomUUID()
    );

    var response = responseEntity.getBody();
    assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
    assertNotNull(response);

    var transaction = response.getTransaction();
    assertEquals(PRE_POPULATED_ITEM_SHIPPED_TRANSACTION_ID, transaction.getId());

    var checkInResponse = response.getFolioCheckIn();
    assertEquals(PRE_POPULATED_PATRON_HOLD_ITEM_BARCODE, checkInResponse.getItem().getBarcode());

    assertFalse(response.getBarcodeAugmented());

    var updatedTransaction = repository.findById(PRE_POPULATED_ITEM_SHIPPED_TRANSACTION_ID).get();
    assertEquals(RETURN_UNCIRCULATED, updatedTransaction.getState());
  }

  @Test
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/inn-reach-transaction/pre-populate-inn-reach-transaction.sql"
  })
  void testCheckInPatronHoldUnshippedItem() {
    modifyFolioItemBarcode(PRE_POPULATED_PATRON_HOLD_TRANSACTION_ID, null);

    when(circulationClient.findRequest(any())).thenReturn(Optional.of(new RequestDTO()));
    when(circulationClient.checkInByBarcode(any(CheckInRequestDTO.class)))
      .thenReturn(new CheckInResponseDTO().item(new CheckInResponseDTOItem().barcode("newbarcode")));
    when(inventoryClient.getItemByBarcode(any())).thenReturn(ResultList.empty());
    when(inventoryClient.findItem(any())).thenReturn(Optional.of(createInventoryItemDTO()));

    var responseEntity = testRestTemplate.postForEntity(
      PATRON_HOLD_CHECK_IN_UNSHIPPED_ENDPOINT, null, PatronHoldCheckInResponseDTO.class,
      PRE_POPULATED_PATRON_HOLD_TRANSACTION_ID, UUID.randomUUID(), "newbarcode"
    );

    var response = responseEntity.getBody();
    assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
    assertNotNull(response);

    var transaction = response.getTransaction();
    assertEquals(PRE_POPULATED_PATRON_HOLD_TRANSACTION_ID, transaction.getId());

    var checkInResponse = response.getFolioCheckIn();
    assertEquals("newbarcode", checkInResponse.getItem().getBarcode());

    assertFalse(response.getBarcodeAugmented());

    var updatedTransaction = repository.findById(PRE_POPULATED_PATRON_HOLD_TRANSACTION_ID).get();
    assertEquals(RECEIVE_UNANNOUNCED, updatedTransaction.getState());
  }

  @Test
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/inn-reach-transaction/pre-populate-inn-reach-transaction.sql"
  })
  void checkInPatronHoldUnshippedItem_barcodeAugmented() {
    modifyFolioItemBarcode(PRE_POPULATED_PATRON_HOLD_TRANSACTION_ID, null);

    when(circulationClient.findRequest(any())).thenReturn(Optional.of(new RequestDTO()));
    when(circulationClient.checkInByBarcode(any(CheckInRequestDTO.class)))
      .thenReturn(new CheckInResponseDTO().item(new CheckInResponseDTOItem().barcode("newbarcode")));
    when(inventoryClient.getItemByBarcode(any())).thenReturn(ResultList.asSinglePage(new InventoryItemDTO()));
    when(inventoryClient.findItem(any())).thenReturn(Optional.of(createInventoryItemDTO()));

    var responseEntity = testRestTemplate.postForEntity(
      PATRON_HOLD_CHECK_IN_UNSHIPPED_ENDPOINT, null, PatronHoldCheckInResponseDTO.class,
      PRE_POPULATED_PATRON_HOLD_TRANSACTION_ID, UUID.randomUUID(), "newbarcode"
    );

    var response = responseEntity.getBody();
    assertTrue(response.getBarcodeAugmented());
  }

  @Test
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/inn-reach-transaction/pre-populate-inn-reach-transaction.sql"
  })
  void testCheckInPatronHoldUnshippedItem_whenRequestIsCancelled() {
    modifyFolioItemBarcode(PRE_POPULATED_PATRON_HOLD_TRANSACTION_ID, null);

    when(circulationClient.findRequest(any())).thenReturn(Optional.of(createCancelledRequest()));
    when(circulationClient.checkInByBarcode(any(CheckInRequestDTO.class)))
      .thenReturn(new CheckInResponseDTO().item(new CheckInResponseDTOItem().barcode("newbarcode")));
    when(inventoryClient.getItemByBarcode(any())).thenReturn(ResultList.empty());
    when(inventoryClient.findItem(any())).thenReturn(Optional.of(createInventoryItemDTO()));

    var responseEntity = testRestTemplate.postForEntity(
      PATRON_HOLD_CHECK_IN_UNSHIPPED_ENDPOINT, null, PatronHoldCheckInResponseDTO.class,
      PRE_POPULATED_PATRON_HOLD_TRANSACTION_ID, UUID.randomUUID(), "newbarcode"
    );

    var response = responseEntity.getBody();
    assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
    assertNotNull(response);

    var transaction = response.getTransaction();
    assertEquals(PRE_POPULATED_PATRON_HOLD_TRANSACTION_ID, transaction.getId());

    var checkInResponse = response.getFolioCheckIn();
    assertEquals("newbarcode", checkInResponse.getItem().getBarcode());

    assertFalse(response.getBarcodeAugmented());

    var updatedTransaction = repository.findById(PRE_POPULATED_PATRON_HOLD_TRANSACTION_ID).get();
    assertEquals(RETURN_UNCIRCULATED, updatedTransaction.getState());
  }

  @Test
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/inn-reach-transaction/pre-populate-transaction-item-shipped.sql"
  })
  void testCheckInPatronHoldItem_withBarcodeAugmented() {
    var requestDTO = new RequestDTO();
    when(circulationClient.findRequest(any())).thenReturn(Optional.of(requestDTO));
    modifyFolioItemBarcode(PRE_POPULATED_ITEM_SHIPPED_TRANSACTION_ID, PRE_POPULATED_PATRON_HOLD_ITEM_BARCODE + "1234");

    when(circulationClient.checkInByBarcode(any(CheckInRequestDTO.class)))
      .thenReturn(new CheckInResponseDTO().item(new CheckInResponseDTOItem().barcode(PRE_POPULATED_PATRON_HOLD_ITEM_BARCODE)));

    var responseEntity = testRestTemplate.postForEntity(
      PATRON_HOLD_CHECK_IN_ENDPOINT, null, PatronHoldCheckInResponseDTO.class,
      PRE_POPULATED_ITEM_SHIPPED_TRANSACTION_ID, UUID.randomUUID()
    );

    var response = responseEntity.getBody();
    assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
    assertNotNull(response);

    var transaction = response.getTransaction();
    assertEquals(PRE_POPULATED_ITEM_SHIPPED_TRANSACTION_ID, transaction.getId());

    var checkInResponse = response.getFolioCheckIn();
    assertEquals(PRE_POPULATED_PATRON_HOLD_ITEM_BARCODE, checkInResponse.getItem().getBarcode());

    assertTrue(response.getBarcodeAugmented());
  }

  @Test
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/inn-reach-transaction/pre-populate-inn-reach-transaction.sql"
  })
  void testCheckInPatronHoldItem_invalidTransactionState() {
    var responseEntity = testRestTemplate.postForEntity(
      PATRON_HOLD_CHECK_IN_ENDPOINT, null, PatronHoldCheckInResponseDTO.class,
      PRE_POPULATED_PATRON_HOLD_TRANSACTION_ID, UUID.randomUUID()
    );

    assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
  }

  @Test
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/inn-reach-transaction/pre-populate-inn-reach-transaction.sql"
  })
  void testCheckOutItemHoldItem() {
    var checkOutResponse = new LoanDTO()
      .id(FOLIO_CHECKOUT_ID)
      .item(new LoanItem().barcode(PRE_POPULATED_ITEM_HOLD_ITEM_BARCODE));

    when(circulationClient.checkOutByBarcode(any(CheckOutRequestDTO.class))).thenReturn(checkOutResponse);

    var responseEntity = testRestTemplate.postForEntity(
      ITEM_HOLD_CHECK_OUT_ENDPOINT, null, TransactionCheckOutResponseDTO.class,
      PRE_POPULATED_ITEM_HOLD_ITEM_BARCODE, UUID.randomUUID()
    );

    var response = responseEntity.getBody();
    assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
    assertNotNull(response);

    var transaction = response.getTransaction();
    assertEquals(PRE_POPULATED_ITEM_HOLD_TRANSACTION_ID, transaction.getId());

    var folioCheckOut = response.getFolioCheckOut();
    assertNotNull(folioCheckOut);
    assertEquals(FOLIO_CHECKOUT_ID, folioCheckOut.getId());
  }

  @Test
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/inn-reach-transaction/pre-populate-inn-reach-transaction.sql"
  })
  void testCheckOutItemHoldItem_invalidTransactionState() {
    modifyTransactionState(PRE_POPULATED_ITEM_HOLD_TRANSACTION_ID, CANCEL_REQUEST);

    var responseEntity = testRestTemplate.postForEntity(
      ITEM_HOLD_CHECK_OUT_ENDPOINT, null, TransactionCheckOutResponseDTO.class,
      PRE_POPULATED_ITEM_HOLD_ITEM_BARCODE, UUID.randomUUID()
    );

    assertEquals(HttpStatus.NOT_FOUND, responseEntity.getStatusCode());
  }

  @Test
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/inn-reach-transaction/pre-populate-inn-reach-transaction.sql",
  })
  void testCheckOutItemHoldItem_multipleTransactions() {
    var transactionWithTheSameItem = createInnReachTransaction(InnReachTransaction.TransactionType.ITEM);
    transactionWithTheSameItem.setCentralServerCode(PRE_POPULATED_CENTRAL_SERVER_CODE);
    transactionWithTheSameItem.getHold().setFolioItemBarcode(PRE_POPULATED_ITEM_HOLD_ITEM_BARCODE);
    transactionWithTheSameItem.setState(TRANSFER);
    var savedTransaction = repository.save(transactionWithTheSameItem);

    modifyTransactionState(PRE_POPULATED_ITEM_HOLD_TRANSACTION_ID, CANCEL_REQUEST);

    var checkOutResponse = new LoanDTO()
      .id(FOLIO_CHECKOUT_ID)
      .item(new LoanItem().barcode(PRE_POPULATED_ITEM_HOLD_ITEM_BARCODE));

    when(circulationClient.checkOutByBarcode(any(CheckOutRequestDTO.class))).thenReturn(checkOutResponse);

    var responseEntity = testRestTemplate.postForEntity(
      ITEM_HOLD_CHECK_OUT_ENDPOINT, null, TransactionCheckOutResponseDTO.class,
      PRE_POPULATED_ITEM_HOLD_ITEM_BARCODE, UUID.randomUUID()
    );

    var response = responseEntity.getBody();
    assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
    assertNotNull(response);

    var transaction = response.getTransaction();
    assertEquals(savedTransaction.getId(), transaction.getId());

    var folioCheckOut = response.getFolioCheckOut();
    assertNotNull(folioCheckOut);
    assertEquals(FOLIO_CHECKOUT_ID, folioCheckOut.getId());
  }

  @ParameterizedTest
  @EnumSource(names = {"ITEM_RECEIVED", "RECEIVE_UNANNOUNCED"})
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/inn-reach-transaction/pre-populate-inn-reach-transaction.sql",
  })
  void testCheckOutPatronHoldItem(InnReachTransaction.TransactionState state) {
    var loanDueDate = new Date();
    var checkOutResponse = new LoanDTO()
      .id(FOLIO_CHECKOUT_ID)
      .dueDate(loanDueDate);

    modifyTransactionState(PRE_POPULATED_PATRON_HOLD_TRANSACTION_ID, state);

    when(circulationClient.queryLoansByItemId(any())).thenReturn(ResultList.asSinglePage(checkOutResponse));

    var responseEntity = testRestTemplate.postForEntity(
      PATRON_HOLD_CHECK_OUT_ENDPOINT, null, TransactionCheckOutResponseDTO.class,
      PRE_POPULATED_PATRON_HOLD_TRANSACTION_ID, UUID.randomUUID()
    );

    var response = responseEntity.getBody();
    assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
    assertNotNull(response);

    var updatedTransaction = response.getTransaction();
    var updatedHold = updatedTransaction.getHold();
    assertEquals(PRE_POPULATED_PATRON_HOLD_TRANSACTION_ID, updatedTransaction.getId());

    var loan = response.getFolioCheckOut();
    assertNotNull(loan);
    assertEquals(FOLIO_CHECKOUT_ID, loan.getId());
    assertEquals(toEpochSec(loanDueDate), updatedHold.getDueDateTime());
    assertEquals(FOLIO_CHECKOUT_ID, updatedHold.getFolioLoanId());
  }

  @ParameterizedTest
  @EnumSource(names = {"ITEM_RECEIVED", "RECEIVE_UNANNOUNCED"})
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/inn-reach-transaction/pre-populate-inn-reach-transaction.sql",
  })
  void testCheckOutPatronHoldItem_createNewLoan(InnReachTransaction.TransactionState state) {
    var loanDueDate = new Date();
    var checkOutResponse = new LoanDTO()
      .id(FOLIO_CHECKOUT_ID)
      .dueDate(loanDueDate);

    modifyTransactionState(PRE_POPULATED_PATRON_HOLD_TRANSACTION_ID, state);

    when(circulationClient.queryLoansByItemId(any())).thenReturn(ResultList.empty());
    when(circulationClient.checkOutByBarcode(any(CheckOutRequestDTO.class))).thenReturn(checkOutResponse);

    var responseEntity = testRestTemplate.postForEntity(
      PATRON_HOLD_CHECK_OUT_ENDPOINT, null, TransactionCheckOutResponseDTO.class,
      PRE_POPULATED_PATRON_HOLD_TRANSACTION_ID, UUID.randomUUID()
    );

    var response = responseEntity.getBody();
    assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
    assertNotNull(response);

    var updatedTransaction = response.getTransaction();
    var updatedHold = updatedTransaction.getHold();
    assertEquals(PRE_POPULATED_PATRON_HOLD_TRANSACTION_ID, updatedTransaction.getId());

    var loan = response.getFolioCheckOut();
    assertNotNull(loan);
    assertEquals(FOLIO_CHECKOUT_ID, loan.getId());
    assertEquals(toEpochSec(loanDueDate), updatedHold.getDueDateTime());
    assertEquals(FOLIO_CHECKOUT_ID, updatedHold.getFolioLoanId());
  }

  @ParameterizedTest
  @EnumSource(names = {"ITEM_RECEIVED", "RECEIVE_UNANNOUNCED"})
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/inn-reach-transaction/pre-populate-inn-reach-transaction.sql",
  })
  void testReturnPatronHoldItem_whenLoanIsOpen(InnReachTransaction.TransactionState state) {
    modifyTransactionState(PRE_POPULATED_PATRON_HOLD_TRANSACTION_ID, state);

    var loan = new LoanDTO().id(FOLIO_CHECKOUT_ID).status(new LoanStatus().name("Open"));

    when(circulationClient.findLoan(any())).thenReturn(Optional.ofNullable(loan));

    var responseEntity = testRestTemplate.postForEntity(
      PATRON_HOLD_RETURN_ITEM_ENDPOINT, null, Void.class,
      PRE_POPULATED_PATRON_HOLD_TRANSACTION_ID, UUID.randomUUID()
    );

    assertEquals(HttpStatus.NO_CONTENT, responseEntity.getStatusCode());

    verify(circulationClient).checkInByBarcode(any());
    verifyNoInteractions(requestService);
  }

  @ParameterizedTest
  @EnumSource(names = {"ITEM_RECEIVED", "RECEIVE_UNANNOUNCED"})
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/inn-reach-transaction/pre-populate-inn-reach-transaction.sql",
  })
  void testReturnPatronHoldItem_whenLoanIsClosed(InnReachTransaction.TransactionState state) {
    modifyTransactionState(PRE_POPULATED_PATRON_HOLD_TRANSACTION_ID, state);

    var loan = new LoanDTO().id(FOLIO_CHECKOUT_ID).status(new LoanStatus().name("Closed"));

    when(circulationClient.findLoan(any())).thenReturn(Optional.ofNullable(loan));

    var responseEntity = testRestTemplate.postForEntity(
      PATRON_HOLD_RETURN_ITEM_ENDPOINT, null, Void.class,
      PRE_POPULATED_PATRON_HOLD_TRANSACTION_ID, UUID.randomUUID()
    );

    assertEquals(HttpStatus.NO_CONTENT, responseEntity.getStatusCode());

    verify(actionNotifier).reportItemInTransit(any());
    verify(circulationClient, never()).checkInByBarcode(any());
    verifyNoInteractions(requestService);
  }

  @ParameterizedTest
  @EnumSource(names = {"ITEM_RECEIVED", "RECEIVE_UNANNOUNCED"})
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/inn-reach-transaction/pre-populate-inn-reach-transaction.sql",
  })
  void testReturnPatronHoldItem_whenNoLoan_and_requestIsClosed(InnReachTransaction.TransactionState state) {
    var request = RequestDTO.builder()
      .id(PRE_POPULATED_PATRON_HOLD_REQUEST_ID)
      .status(CLOSED_CANCELLED)
      .build();

    modifyTransaction(PRE_POPULATED_PATRON_HOLD_TRANSACTION_ID, t -> {
      t.getHold().setFolioLoanId(null);
      t.setState(state);
    });

    when(circulationClient.findRequest(any())).thenReturn(Optional.ofNullable(request));

    var responseEntity = testRestTemplate.postForEntity(
      PATRON_HOLD_RETURN_ITEM_ENDPOINT, null, Void.class,
      PRE_POPULATED_PATRON_HOLD_TRANSACTION_ID, UUID.randomUUID()
    );

    assertEquals(HttpStatus.NO_CONTENT, responseEntity.getStatusCode());

    verify(circulationClient).checkInByBarcode(any());
    verify(requestService).findRequest(PRE_POPULATED_PATRON_HOLD_REQUEST_ID);
  }

  @Test
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/inn-reach-transaction/pre-populate-inn-reach-transaction-for-search.sql"
  })
  void returnTransactionByBarcodeAndState_when_transactionsFound() {
    var responseEntity = testRestTemplate.getForEntity(
      "/inn-reach/transactions?itemBarcode={itemBarcode}&state={state1}&state={state2}", InnReachTransactionsDTO.class,
      "ABC-abc-1234", "PATRON_HOLD", "ITEM_HOLD"
    );

    assertEquals(HttpStatus.OK, responseEntity.getStatusCode());

    var body = responseEntity.getBody();

    assertNotNull(body);
    assertEquals(2, body.getTotalRecords());

    var transactions = body.getTransactions();

    assertNotNull(transactions);
    assertFalse(transactions.isEmpty());
    assertEquals(2, transactions.size());

    var allTransactionTypesEqualToSearched = transactions
      .stream()
      .allMatch(it -> it.getType().equals(ITEM) || it.getType().equals(PATRON));

    assertTrue(allTransactionTypesEqualToSearched);

    var allBarcodesEqualToSearched = transactions
      .stream()
      .allMatch(it -> it.getHold().getFolioItemBarcode().equals("ABC-abc-1234")
        || it.getHold().getShippedItemBarcode().equals("ABC-abc-1234"));

    assertTrue(allBarcodesEqualToSearched);
  }

  @Test
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/inn-reach-transaction/pre-populate-inn-reach-transaction-for-search.sql"
  })
  void returnEmptyListByBarcodeAndState_when_transactionsNotFound() {
    var responseEntity = testRestTemplate.getForEntity(
      "/inn-reach/transactions?itemBarcode={itemBarcode}&state={state1}&state={state2}", InnReachTransactionsDTO.class,
      "ABC-abc-4321", "PATRON_HOLD", "ITEM_HOLD"
    );

    assertEquals(HttpStatus.OK, responseEntity.getStatusCode());

    var body = responseEntity.getBody();

    assertNotNull(body);

    var transactions = body.getTransactions();

    assertNotNull(transactions);
    assertTrue(transactions.isEmpty());
  }

  @ParameterizedTest
  @ValueSource(strings = {"0aab1720-14b4-4210-9a19-0d0bf1cd64d3",
    "ab2393a1-acc4-4849-82ac-8cc0c37339e1",
    "79b0a1fb-55be-4e55-9d84-01303aaec1ce"})
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/inn-reach-transaction/pre-populate-inn-reach-transaction.sql"
  })
  void updateTransactionWhenImmutableFieldsNotChanged(String transactionId) {
    var transaction = repository.fetchOneById(UUID.fromString(transactionId)).get();
    var hold = transaction.getHold();

    hold.setFolioRequestId(null);
    transaction.setHold(hold);
    transaction.setState(FINAL_CHECKIN);

    var transactionDTO = innReachTransactionMapper.toDTO(transaction);

    var responseEntity = testRestTemplate.exchange(
      UPDATE_TRANSACTION_ENDPOINT, HttpMethod.PUT,
      new HttpEntity<>(transactionDTO, headers), InnReachTransactionDTO.class,
      UUID.fromString(transactionId)
    );

    var updatedTransaction = repository.fetchOneById(UUID.fromString(transactionId)).get();

    assertEquals(HttpStatus.NO_CONTENT, responseEntity.getStatusCode());
    assertEquals(FINAL_CHECKIN, updatedTransaction.getState());
    assertEquals(transaction.getTrackingId(), updatedTransaction.getTrackingId());
    assertEquals(transaction.getCentralServerCode(), updatedTransaction.getCentralServerCode());
    assertEquals(transaction.getType(), updatedTransaction.getType());
  }

  @Test
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/inn-reach-transaction/pre-populate-inn-reach-transaction.sql"
  })
  void updateTransactionWhenImmutableFieldsChanged() {
    var oldTransaction = repository.fetchOneById(PRE_POPULATED_ITEM_HOLD_TRANSACTION_ID).get();
    var transaction = repository.fetchOneById(PRE_POPULATED_ITEM_HOLD_TRANSACTION_ID).get();
    var hold = transaction.getHold();
    var pickupLocation = hold.getPickupLocation();

    pickupLocation.setDeliveryStop(NEW_TEST_PARAMETER_VALUE);
    pickupLocation.setDisplayName(NEW_TEST_PARAMETER_VALUE);
    pickupLocation.setPickupLocCode(NEW_TEST_PARAMETER_VALUE);
    pickupLocation.setPrintName(NEW_TEST_PARAMETER_VALUE);
    hold.setItemAgencyCode(NEW_ITEM_AND_AGENCY_CODE);
    hold.setPatronAgencyCode(NEW_ITEM_AND_AGENCY_CODE);
    hold.setFolioRequestId(null);
    hold.setPickupLocation(pickupLocation);
    transaction.setHold(hold);
    transaction.setTrackingId(TRACKING_ID);
    transaction.setCentralServerCode(NEW_CENTRAL_SERVER_CODE);

    var transactionDTO = innReachTransactionMapper.toDTO(transaction);

    var responseEntity = testRestTemplate.exchange(
      UPDATE_TRANSACTION_ENDPOINT, HttpMethod.PUT,
      new HttpEntity<>(transactionDTO, headers), InnReachTransactionDTO.class,
      PRE_POPULATED_ITEM_HOLD_TRANSACTION_ID
    );

    var updatedTransaction = repository.fetchOneById(PRE_POPULATED_ITEM_HOLD_TRANSACTION_ID).get();

    assertEquals(HttpStatus.NO_CONTENT, responseEntity.getStatusCode());
    assertEquals(oldTransaction.getTrackingId(), updatedTransaction.getTrackingId());
    assertEquals(oldTransaction.getCentralServerCode(), updatedTransaction.getCentralServerCode());
    assertEquals(NEW_ITEM_AND_AGENCY_CODE, updatedTransaction.getHold().getItemAgencyCode());
    assertEquals(NEW_ITEM_AND_AGENCY_CODE, updatedTransaction.getHold().getPatronAgencyCode());
    assertEquals(NEW_TEST_PARAMETER_VALUE, updatedTransaction.getHold().getPickupLocation().getDeliveryStop());
    assertEquals(NEW_TEST_PARAMETER_VALUE, updatedTransaction.getHold().getPickupLocation().getDisplayName());
    assertEquals(NEW_TEST_PARAMETER_VALUE, updatedTransaction.getHold().getPickupLocation().getPickupLocCode());
    assertEquals(NEW_TEST_PARAMETER_VALUE, updatedTransaction.getHold().getPickupLocation().getPrintName());
  }

  @ParameterizedTest
  @EnumSource(names = {"OPEN_AWAITING_PICKUP", "OPEN_AWAITING_DELIVERY", "OPEN_IN_TRANSIT", "OPEN_NOT_YET_FILLED"})
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/inn-reach-transaction/pre-populate-inn-reach-transaction.sql",
  })
  void cancelPatronHold_when_ItemShipped_and_RequestIsOpen(RequestDTO.RequestStatus status) {
    mockFindRequest(status);

    modifyTransactionState(PRE_POPULATED_PATRON_HOLD_TRANSACTION_ID, ITEM_SHIPPED);
    var cancelPatronHold = createCancelPatronHold();

    var responseEntity = testRestTemplate.postForEntity(
      PATRON_HOLD_CANCEL_ENDPOINT, cancelPatronHold, InnReachTransactionDTO.class,
      PRE_POPULATED_PATRON_HOLD_TRANSACTION_ID);

    assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
    var updatedTransaction = responseEntity.getBody();

    assertNotNull(updatedTransaction);
    assertEquals(TransactionStateEnum.ITEM_SHIPPED, updatedTransaction.getState());

    var cancelRequestCaptor = ArgumentCaptor.forClass(RequestDTO.class);
    verify(circulationClient).updateRequest(eq(PRE_POPULATED_PATRON_HOLD_REQUEST_ID), cancelRequestCaptor.capture());

    var cancelRequest = cancelRequestCaptor.getValue();
    assertEquals(CLOSED_CANCELLED, cancelRequest.getStatus());
    assertEquals(cancelPatronHold.getCancellationReasonId(), cancelRequest.getCancellationReasonId());
    assertEquals(cancelPatronHold.getCancellationAdditionalInformation(),
      cancelRequest.getCancellationAdditionalInformation());

    verify(innReachClient, never()).postInnReachApi(any(), anyString(), anyString(), anyString());
  }

  @ParameterizedTest
  @EnumSource(names = {"OPEN_AWAITING_PICKUP", "OPEN_AWAITING_DELIVERY", "OPEN_IN_TRANSIT", "OPEN_NOT_YET_FILLED"})
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/inn-reach-transaction/pre-populate-inn-reach-transaction.sql",
  })
  void cancelPatronHold_when_ItemIsNotShipped_and_RequestIsOpen(RequestDTO.RequestStatus status) {
    mockFindRequest(status);

    modifyTransactionState(PRE_POPULATED_PATRON_HOLD_TRANSACTION_ID, InnReachTransaction.TransactionState.PATRON_HOLD);
    var cancelPatronHold = createCancelPatronHold();

    var responseEntity = testRestTemplate.postForEntity(
      PATRON_HOLD_CANCEL_ENDPOINT, cancelPatronHold, InnReachTransactionDTO.class,
      PRE_POPULATED_PATRON_HOLD_TRANSACTION_ID);

    assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
    var updatedTransaction = responseEntity.getBody();

    assertNotNull(updatedTransaction);
    assertEquals(TransactionStateEnum.BORROWING_SITE_CANCEL, updatedTransaction.getState());

    var cancelRequestCaptor = ArgumentCaptor.forClass(RequestDTO.class);

    verify(circulationClient).updateRequest(eq(PRE_POPULATED_PATRON_HOLD_REQUEST_ID), cancelRequestCaptor.capture());

    var cancelRequest = cancelRequestCaptor.getValue();
    assertEquals(CLOSED_CANCELLED, cancelRequest.getStatus());
    assertEquals(cancelPatronHold.getCancellationReasonId(), cancelRequest.getCancellationReasonId());
    assertEquals(cancelPatronHold.getCancellationAdditionalInformation(),
      cancelRequest.getCancellationAdditionalInformation());

    verify(actionNotifier).reportCancelItemHold(any());
    verify(innReachClient).postInnReachApi(any(), anyString(), anyString(), anyString());
  }

  @ParameterizedTest
  @EnumSource(names = {"PATRON_HOLD", "TRANSFER"})
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/inn-reach-transaction/pre-populate-inn-reach-transaction.sql",
  })
  void cancelPatronHold_when_TransactionIsOnHoldOrTransfer_and_RequestIsClosed(InnReachTransaction.TransactionState state) {
    mockFindRequest(CLOSED_CANCELLED);

    modifyTransactionState(PRE_POPULATED_PATRON_HOLD_TRANSACTION_ID, state);
    var cancelPatronHold = createCancelPatronHold();

    var responseEntity = testRestTemplate.postForEntity(
      PATRON_HOLD_CANCEL_ENDPOINT, cancelPatronHold, InnReachTransactionDTO.class,
      PRE_POPULATED_PATRON_HOLD_TRANSACTION_ID);

    assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
    var updatedTransaction = responseEntity.getBody();

    assertNotNull(updatedTransaction);
    assertEquals(TransactionStateEnum.BORROWING_SITE_CANCEL, updatedTransaction.getState());

    verify(circulationClient, never()).updateRequest(eq(PRE_POPULATED_PATRON_HOLD_REQUEST_ID), any());
    verify(actionNotifier).reportCancelItemHold(any());
    verify(innReachClient).postInnReachApi(any(), anyString(), anyString(), anyString());
  }

  @ParameterizedTest
  @EnumSource(names = {"ITEM_SHIPPED", "RECEIVE_UNANNOUNCED", "ITEM_RECEIVED"})
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/inn-reach-transaction/pre-populate-inn-reach-transaction.sql",
  })
  void cancelPatronHold_when_ItemIsNotAwaitingPickup_and_RequestIsClosed(InnReachTransaction.TransactionState state) {
    mockFindRequest(CLOSED_CANCELLED);
    mockFindItem(IN_PROCESS);

    modifyTransactionState(PRE_POPULATED_PATRON_HOLD_TRANSACTION_ID, state);

    var cancelPatronHold = createCancelPatronHold();
    var responseEntity = testRestTemplate.postForEntity(
      PATRON_HOLD_CANCEL_ENDPOINT, cancelPatronHold, InnReachTransactionDTO.class,
      PRE_POPULATED_PATRON_HOLD_TRANSACTION_ID);

    assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
    var updatedTransaction = responseEntity.getBody();

    assertNotNull(updatedTransaction);
    assertEquals(TransactionStateEnum.RETURN_UNCIRCULATED, updatedTransaction.getState());

    verify(circulationClient, never()).updateRequest(eq(PRE_POPULATED_PATRON_HOLD_REQUEST_ID), any());
    verify(actionNotifier).reportReturnUncirculated(any());
    verify(innReachClient).postInnReachApi(any(), anyString(), anyString(), anyString());
  }

  private void mockFindRequest(RequestDTO.RequestStatus status) {
    var requestDTO = createRequestDTO();
    requestDTO.setId(PRE_POPULATED_PATRON_HOLD_REQUEST_ID);
    requestDTO.setStatus(status);

    when(circulationClient.findRequest(PRE_POPULATED_PATRON_HOLD_REQUEST_ID))
      .thenReturn(Optional.of(requestDTO));
  }

  private void mockFindItem(InventoryItemStatus status) {
    var itemDTO = createInventoryItemDTO();
    itemDTO.setId(PRE_POPULATED_PATRON_HOLD_ITEM_ID);
    itemDTO.setStatus(status);

    when(inventoryClient.findItem(PRE_POPULATED_PATRON_HOLD_ITEM_ID))
      .thenReturn(Optional.of(itemDTO));
  }

  private CancelPatronHoldDTO createCancelPatronHold() {
    return new CancelPatronHoldDTO()
      .cancellationReasonId(randomUUID())
      .cancellationAdditionalInformation(RandomStringUtils.randomAlphabetic(255));
  }

  private void modifyFolioItemBarcode(UUID transactionId, String newBarcode) {
    modifyTransaction(transactionId, t -> t.getHold().setFolioItemBarcode(newBarcode));
  }

  private void modifyTransactionState(UUID transactionId, InnReachTransaction.TransactionState newState) {
    modifyTransaction(transactionId, t -> t.setState(newState));
  }

  private InnReachTransaction modifyTransaction(UUID transactionId, Consumer<InnReachTransaction> transactionModifier) {
    var transaction = repository.fetchOneById(transactionId).get();
    transactionModifier.accept(transaction);
    return repository.save(transaction);
  }

  private RequestDTO createCancelledRequest() {
    var request = new RequestDTO();
    request.setStatus(CLOSED_CANCELLED);
    return request;
  }

}
