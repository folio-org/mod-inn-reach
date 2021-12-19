package org.folio.innreach.controller;

import static java.util.UUID.randomUUID;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
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
import static org.folio.innreach.domain.entity.InnReachTransaction.TransactionState.CANCEL_REQUEST;
import static org.folio.innreach.dto.TransactionStateEnum.PATRON_HOLD;
import static org.folio.innreach.dto.TransactionTypeEnum.ITEM;
import static org.folio.innreach.dto.TransactionTypeEnum.LOCAL;
import static org.folio.innreach.dto.TransactionTypeEnum.PATRON;
import static org.folio.innreach.fixture.InventoryItemFixture.createInventoryItemDTO;
import static org.folio.innreach.fixture.RequestFixture.createRequestDTO;
import static org.folio.innreach.fixture.TestUtil.deserializeFromJsonFile;
import static org.folio.innreach.fixture.UserFixture.createUser;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

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

import org.folio.innreach.client.CirculationClient;
import org.folio.innreach.client.HoldingsStorageClient;
import org.folio.innreach.client.InventoryClient;
import org.folio.innreach.client.ServicePointsUsersClient;
import org.folio.innreach.client.UsersClient;
import org.folio.innreach.controller.base.BaseControllerTest;
import org.folio.innreach.domain.dto.OwningSiteCancelsRequestDTO;
import org.folio.innreach.domain.dto.folio.ResultList;
import org.folio.innreach.domain.dto.folio.User;
import org.folio.innreach.domain.dto.folio.circulation.RequestDTO;
import org.folio.innreach.domain.dto.folio.inventory.InventoryItemDTO;
import org.folio.innreach.domain.dto.folio.inventorystorage.ServicePointUserDTO;
import org.folio.innreach.domain.entity.InnReachTransaction;
import org.folio.innreach.domain.entity.TransactionItemHold;
import org.folio.innreach.domain.entity.base.AuditableUser;
import org.folio.innreach.domain.service.RequestService;
import org.folio.innreach.dto.CheckInRequestDTO;
import org.folio.innreach.dto.CheckInResponseDTO;
import org.folio.innreach.dto.CheckInResponseDTOItem;
import org.folio.innreach.dto.CheckOutRequestDTO;
import org.folio.innreach.dto.CheckOutResponseDTO;
import org.folio.innreach.dto.InnReachResponseDTO;
import org.folio.innreach.dto.InnReachTransactionDTO;
import org.folio.innreach.dto.InnReachTransactionsDTO;
import org.folio.innreach.dto.ItemHoldCheckOutResponseDTO;
import org.folio.innreach.dto.LoanItem;
import org.folio.innreach.dto.PatronHoldCheckInResponseDTO;
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

  private static final String PATRON_HOLD_CHECK_IN_ENDPOINT = "/inn-reach/transactions/{id}/receive-item/{servicePointId}";
  private static final String ITEM_HOLD_CHECK_OUT_ENDPOINT = "/inn-reach/transactions/{itemBarcode}/check-out-item/{servicePointId}";

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

  private static final UUID PRE_POPULATED_TRANSACTION_ID1 = UUID.fromString("0aab1720-14b4-4210-9a19-0d0bf1cd64d3");
  private static final UUID PRE_POPULATED_ITEM_HOLD_TRANSACTION_ID = UUID.fromString("ab2393a1-acc4-4849-82ac-8cc0c37339e1");
  private static final UUID PRE_POPULATED_TRANSACTION_ID3 = UUID.fromString("79b0a1fb-55be-4e55-9d84-01303aaec1ce");
  private static final UUID PRE_POPULATED_ITEM_SHIPPED_TRANSACTION_ID = UUID.fromString("7106c3ac-890a-4126-bf9b-a10b67555b6e");
  private static final String PRE_POPULATED_PATRON_HOLD_ITEM_BARCODE = "1111111";
  private static final String PRE_POPULATED_ITEM_HOLD_ITEM_BARCODE = "DEF-def-5678";
  private static final String PRE_POPULATED_CENTRAL_PATRON_ID2 = "a7853dda520b4f7aa1fb9383665ea770";
  private static final UUID FOLIO_CHECKOUT_ID = UUID.randomUUID();

  private static final AuditableUser PRE_POPULATED_USER = AuditableUser.SYSTEM;

  private static final Duration ASYNC_AWAIT_TIMEOUT = Duration.ofSeconds(15);

  @Autowired
  private TestRestTemplate testRestTemplate;
  @SpyBean
  private InnReachTransactionRepository repository;
  @Autowired
  private InnReachTransactionPickupLocationMapper transactionPickupLocationMapper;

  @MockBean
  private InventoryClient inventoryClient;
  @MockBean
  private HoldingsStorageClient holdingsStorageClient;
  @MockBean
  private CirculationClient circulationClient;
  @MockBean
  private ServicePointsUsersClient servicePointsUsersClient;
  @MockBean
  private UsersClient usersClient;
  @MockBean
  private InnReachClient innReachClient;

  @SpyBean
  private RequestService requestService;

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

  void mockInventoryStorageClient(User user) {
    var servicePointUserDTO = new ServicePointUserDTO();
    servicePointUserDTO.setUserId(user.getId());
    servicePointUserDTO.setDefaultServicePointId(randomUUID());
    when(servicePointsUsersClient.findServicePointsUsers(user.getId())).thenReturn(ResultList.of(1, List.of(servicePointUserDTO)));
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
      List.of(PRE_POPULATED_TRANSACTION_ID1, PRE_POPULATED_ITEM_HOLD_TRANSACTION_ID, PRE_POPULATED_TRANSACTION_ID3)));

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
      "/inn-reach/transactions?offset=1&limit=1", InnReachTransactionsDTO.class
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
    assertTrue(transactionIds.containsAll(List.of(PRE_POPULATED_TRANSACTION_ID1, PRE_POPULATED_ITEM_HOLD_TRANSACTION_ID)));

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
    assertEquals(2, responseEntity.getBody().getTotalRecords());

    var titles = responseEntity.getBody().getTransactions().stream()
      .map(InnReachTransactionDTO::getHold).map(TransactionHoldDTO::getTitle).collect(Collectors.toList());
    assertEquals(2, titles.size());
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
    assertTrue(transactions.get(0).getHold().getCentralPatronType() <
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
    mockInventoryStorageClient(user);
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
      "/inn-reach/d2ir/circ/itemhold/{trackingId}/{centralCode}", itemHoldDTO, InnReachResponseDTO.class, TRACKING_ID,
      PRE_POPULATED_CENTRAL_SERVER_CODE);

    verify(requestService).createItemHoldRequest(TRACKING_ID);
    assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
    assertTrue(responseEntity.hasBody());
    assertEquals("ok", responseEntity.getBody().getStatus());

    await().atMost(ASYNC_AWAIT_TIMEOUT).untilAsserted(() ->
      verify(repository).save(
        argThat((InnReachTransaction t) -> t.getHold().getFolioRequestId() != null)));

    verify(requestService).createItemHoldRequest(TRACKING_ID);
    verify(inventoryClient, times(2)).getItemsByHrId(itemHoldDTO.getItemId());
    verify(circulationClient).queryRequestsByItemId(inventoryItemDTO.getId());
    verify(usersClient).query(PRE_POPULATED_USER_BARCODE_QUERY);
    verify(servicePointsUsersClient).findServicePointsUsers(user.getId());
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
    var user = mockUserClient();
    mockInventoryStorageClient(user);
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
      "/inn-reach/d2ir/circ/itemhold/{trackingId}/{centralCode}", itemHoldDTO, InnReachResponseDTO.class, TRACKING_ID,
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
    itemHoldDTO.setItemId(inventoryItemDTO.getHrid());

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
    itemHoldDTO.setItemId(inventoryItemDTO.getHrid());

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
    itemHoldDTO.setItemId(inventoryItemDTO.getHrid());

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
    itemHoldDTO.setItemId(inventoryItemDTO.getHrid());

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
    when(servicePointsUsersClient.findServicePointsUsers(user.getId())).thenThrow(IllegalStateException.class);
    when(innReachClient.postInnReachApi(any(), anyString(), anyString(), anyString(), any())).thenReturn("response");

    var itemHoldDTO = deserializeFromJsonFile(
      "/inn-reach-transaction/create-item-hold-request.json", TransactionHoldDTO.class);
    itemHoldDTO.setItemId(inventoryItemDTO.getHrid());
    itemHoldDTO.setCentralPatronType(PRE_POPULATED_CENTRAL_PATRON_TYPE);

    var responseEntity = testRestTemplate.postForEntity(
      "/inn-reach/d2ir/circ/itemhold/{trackingId}/{centralCode}", itemHoldDTO, InnReachResponseDTO.class, TRACKING_ID,
      PRE_POPULATED_CENTRAL_SERVER_CODE);

    assertEquals(HttpStatus.OK, responseEntity.getStatusCode());

    await().untilAsserted(() -> verify(innReachClient).postInnReachApi(any(), anyString(), anyString(), anyString(), any()));

    verify(inventoryClient).getItemsByHrId(inventoryItemDTO.getHrid());
    verify(circulationClient, never()).queryRequestsByItemId(inventoryItemDTO.getId());
    verify(usersClient).query(PRE_POPULATED_USER_BARCODE_QUERY);
    verify(servicePointsUsersClient).findServicePointsUsers(user.getId());
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
    mockInventoryStorageClient(user);
    when(innReachClient.postInnReachApi(any(), anyString(), anyString(), anyString(), any())).thenReturn("response");

    var itemHoldDTO = deserializeFromJsonFile(
      "/inn-reach-transaction/create-item-hold-request.json", TransactionHoldDTO.class);
    itemHoldDTO.setItemId(inventoryItemDTO.getHrid());
    itemHoldDTO.setCentralPatronType(PRE_POPULATED_CENTRAL_PATRON_TYPE);

    var responseEntity = testRestTemplate.postForEntity(
      "/inn-reach/d2ir/circ/itemhold/{trackingId}/{centralCode}", itemHoldDTO, InnReachResponseDTO.class, PRE_POPULATED_TRACKING_ID,
      PRE_POPULATED_CENTRAL_SERVER_CODE);

    assertEquals(HttpStatus.OK, responseEntity.getStatusCode());

    await().untilAsserted(() -> verify(innReachClient).postInnReachApi(any(), anyString(), anyString(), anyString(), any()));

    verify(inventoryClient, times(2)).getItemsByHrId(inventoryItemDTO.getHrid());
    verify(circulationClient).queryRequestsByItemId(inventoryItemDTO.getId());
    verify(usersClient).query(PRE_POPULATED_USER_BARCODE_QUERY);
    verify(servicePointsUsersClient).findServicePointsUsers(user.getId());
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
  }

  @Test
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/inn-reach-transaction/pre-populate-transaction-item-shipped.sql"
  })
  void testCheckInPatronHoldItem_withBarcodeAugmented() {
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
      PRE_POPULATED_TRANSACTION_ID1, UUID.randomUUID()
    );

    assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
  }

  @Test
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/inn-reach-transaction/pre-populate-inn-reach-transaction.sql"
  })
  void testCheckOutItemHoldItem() {
    var checkOutResponse = new CheckOutResponseDTO()
      .id(FOLIO_CHECKOUT_ID)
      .item(new LoanItem().barcode(PRE_POPULATED_ITEM_HOLD_ITEM_BARCODE));

    when(circulationClient.checkOutByBarcode(any(CheckOutRequestDTO.class))).thenReturn(checkOutResponse);

    var responseEntity = testRestTemplate.postForEntity(
      ITEM_HOLD_CHECK_OUT_ENDPOINT, null, ItemHoldCheckOutResponseDTO.class,
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
      ITEM_HOLD_CHECK_OUT_ENDPOINT, null, ItemHoldCheckOutResponseDTO.class,
      PRE_POPULATED_ITEM_HOLD_ITEM_BARCODE, UUID.randomUUID()
    );

    assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
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

  private void modifyFolioItemBarcode(UUID transactionId, String newBarcode) {
    var transaction = repository.fetchOneById(transactionId).get();
    transaction.getHold().setFolioItemBarcode(newBarcode);
    repository.save(transaction);
  }

  private void modifyTransactionState(UUID transactionId, InnReachTransaction.TransactionState newState) {
    var transaction = repository.fetchOneById(transactionId).get();
    transaction.setState(newState);
    repository.save(transaction);
  }

}
