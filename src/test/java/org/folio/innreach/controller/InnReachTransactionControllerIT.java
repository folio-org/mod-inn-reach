package org.folio.innreach.controller;

import static org.folio.innreach.domain.entity.InnReachTransaction.TransactionState.ITEM_RECEIVED;
import static org.folio.innreach.domain.entity.InnReachTransaction.TransactionState.RECEIVE_UNANNOUNCED;
import static org.folio.innreach.domain.entity.InnReachTransaction.TransactionState.RETURN_UNCIRCULATED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.AFTER_TEST_METHOD;
import static org.springframework.test.context.jdbc.SqlMergeMode.MergeMode.MERGE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import org.folio.innreach.domain.entity.InnReachTransaction;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import tools.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlMergeMode;

import org.folio.innreach.controller.base.BaseApiControllerTest;
import org.folio.innreach.dto.TransactionCheckOutResponseDTO;
import org.folio.innreach.external.service.InnReachExternalService;

@Sql(
  scripts = {
    "classpath:db/central-server/clear-central-server-tables.sql",
    "classpath:db/inn-reach-transaction/clear-inn-reach-transaction-tables.sql"
  },
  executionPhase = AFTER_TEST_METHOD
)
@SqlMergeMode(MERGE)
@ExtendWith(MockitoExtension.class)
class InnReachTransactionControllerIT extends BaseApiControllerTest {

  private static final String CHECK_OUT_ITEM_HOLD_ENDPOINT =
    "/inn-reach/transactions/{itemBarcode}/check-out-item/{servicePointId}";
  private static final String GET_TRANSACTION_ENDPOINT = "/inn-reach/transactions/{id}";
  private static final String GET_ALL_TRANSACTIONS_ENDPOINT = "/inn-reach/transactions";
  private static final String CHECK_IN_PATRON_HOLD_ENDPOINT =
    "/inn-reach/transactions/{id}/receive-item/{servicePointId}";
  private static final String CHECK_IN_UNSHIPPED_ENDPOINT =
    "/inn-reach/transactions/{id}/receive-unshipped-item/{servicePointId}/{itemBarcode}";
  private static final String CANCEL_ITEM_HOLD_ENDPOINT =
    "/inn-reach/transactions/{id}/itemhold/cancel";
  private static final String REMOVE_PATRON_HOLD_ENDPOINT =
    "/inn-reach/transactions/{id}/patronhold/remove";
  private static final String RETURN_PATRON_HOLD_ENDPOINT =
    "/inn-reach/transactions/{id}/patronhold/return-item/{servicePointId}";
  private static final String TRANSFER_ITEM_HOLD_ENDPOINT =
    "/inn-reach/transactions/{id}/itemhold/transfer-item/{itemId}";
  private static final String CHECK_OUT_PATRON_HOLD_ENDPOINT =
    "/inn-reach/transactions/{id}/patronhold/check-out-item/{servicePointId}";
  private static final String CHECK_OUT_LOCAL_HOLD_ENDPOINT =
    "/inn-reach/transactions/{id}/localhold/check-out-item/{servicePointId}";
  private static final String FINAL_CHECKIN_ENDPOINT =
    "/inn-reach/transactions/{id}/itemhold/finalcheckin/{servicePointId}";
  private static final String CANCEL_LOCAL_HOLD_ENDPOINT =
    "/inn-reach/transactions/{id}/localhold/cancel";

  private static final String PRE_POPULATED_ITEM_HOLD_ITEM_BARCODE = "DEF-def-5678";
  private static final String CHECK_OUT_BY_BARCODE_URL = "/circulation/check-out-by-barcode";
  private static final String CHECK_IN_BY_BARCODE_URL = "/circulation/check-in-by-barcode";
  private static final UUID SERVICE_POINT_ID = UUID.randomUUID();

  // Transaction IDs from pre-populate-inn-reach-transaction.sql
  private static final String PATRON_HOLD_TRANSACTION_ID = "0aab1720-14b4-4210-9a19-0d0bf1cd64d3";
  private static final String ITEM_HOLD_TRANSACTION_ID = "ab2393a1-acc4-4849-82ac-8cc0c37339e1";

  // Transaction ID from pre-populate-transaction-item-shipped.sql (state=ITEM_SHIPPED, type=PATRON)
  private static final String ITEM_SHIPPED_TRANSACTION_ID = "7106c3ac-890a-4126-bf9b-a10b67555b6e";

  // Item/request IDs from pre-populated data
  private static final String PATRON_HOLD_REQUEST_ID = "ea11eba7-3c0f-4d15-9cca-c8608cd6bc8a";
  private static final String PATRON_HOLD_ITEM_ID = "9a326225-6530-41cc-9399-a61987bfab3c";
  private static final String UNSHIPPED_ITEM_BARCODE = "newbarcode";

  // Local hold data for testing
  private static final String LOCAL_HOLD_TRANSACTION_ID = "79b0a1fb-55be-4e55-9d84-01303aaec1ce";
  private static final String LOCAL_HOLD_REQUEST_ID = "4106d147-9085-4dfa-a59f-b8d50d551a48";
  private static final String LOCAL_HOLD_INSTANCE_ID = "709c1075-0378-48af-a682-b4e7ac170424";
  private static final String ITEM_HOLD_ITEM_ID = "4def31b0-2b60-4531-ad44-7eab60fa5428";
  private static final String ITEM_HOLD_REQUEST_ID = "26278b3a-de32-4deb-b81b-896637b3dbeb";
  private static final String ITEM_HOLD_LOAN_ID = "06e820e3-71a0-455e-8c73-3963aea677d4";
  private static final String PATRON_HOLD_LOAN_ID = "fd5109c7-8934-4294-9504-c1a4a4f07c96";

  @MockitoBean
  private InnReachExternalService innReachExternalService;

  @Autowired
  private ObjectMapper objectMapper;

  @BeforeEach
  void setup() {
    wm.resetAll();
  }

  @SneakyThrows
  @Test
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/inn-reach-transaction/pre-populate-inn-reach-transaction.sql"
  })
  void checkOutItemHoldItem_success() {
    stubPost(CHECK_OUT_BY_BARCODE_URL, "circulation/checkout-response.json");

    when(innReachExternalService.postInnReachApi(any(), any(), any()))
      .thenReturn("ok");

    var result = mockMvc.perform(post(CHECK_OUT_ITEM_HOLD_ENDPOINT,
        PRE_POPULATED_ITEM_HOLD_ITEM_BARCODE, SERVICE_POINT_ID)
        .contentType(MediaType.APPLICATION_JSON)
        .headers(getOkapiHeaders()))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.folioCheckOut").exists())
      .andExpect(jsonPath("$.folioCheckOut.id").value("a1b2c3d4-e5f6-7890-abcd-ef1234567890"))
      .andExpect(jsonPath("$.folioCheckOut.item.barcode").value(PRE_POPULATED_ITEM_HOLD_ITEM_BARCODE))
      .andExpect(jsonPath("$.transaction").exists())
      .andExpect(jsonPath("$.transaction.state").value("ITEM_SHIPPED"))
      .andReturn();

    var response = objectMapper.readValue(
      result.getResponse().getContentAsString(), TransactionCheckOutResponseDTO.class);

    assertNotNull(response.getTransaction());
    assertNotNull(response.getFolioCheckOut());
    assertEquals(PRE_POPULATED_ITEM_HOLD_ITEM_BARCODE, response.getFolioCheckOut().getItem().getBarcode());
  }

  @SneakyThrows
  @Test
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/inn-reach-transaction/pre-populate-inn-reach-transaction.sql"
  })
  void getInnReachTransaction_success() {
    mockMvc.perform(get(GET_TRANSACTION_ENDPOINT, PATRON_HOLD_TRANSACTION_ID)
        .contentType(MediaType.APPLICATION_JSON)
        .headers(getOkapiHeaders()))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.id").value(PATRON_HOLD_TRANSACTION_ID))
      .andExpect(jsonPath("$.state").value("PATRON_HOLD"))
      .andExpect(jsonPath("$.type").value("PATRON"));
  }

  @SneakyThrows
  @Test
  void getInnReachTransaction_notFound() {
    mockMvc.perform(get(GET_TRANSACTION_ENDPOINT, UUID.randomUUID())
        .contentType(MediaType.APPLICATION_JSON)
        .headers(getOkapiHeaders()))
      .andExpect(status().isNotFound());
  }

  @SneakyThrows
  @Test
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/inn-reach-transaction/pre-populate-inn-reach-transaction.sql"
  })
  void getAllTransactions_success() {
    mockMvc.perform(get(GET_ALL_TRANSACTIONS_ENDPOINT)
        .contentType(MediaType.APPLICATION_JSON)
        .headers(getOkapiHeaders()))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.totalRecords").value(3))
      .andExpect(jsonPath("$.transactions").isArray())
      .andExpect(jsonPath("$.transactions.length()").value(3));
  }

  @SneakyThrows
  @Test
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/inn-reach-transaction/pre-populate-inn-reach-transaction.sql"
  })
  void getAllTransactions_withPagination() {
    mockMvc.perform(get(GET_ALL_TRANSACTIONS_ENDPOINT)
        .param("offset", "0")
        .param("limit", "1")
        .contentType(MediaType.APPLICATION_JSON)
        .headers(getOkapiHeaders()))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.totalRecords").value(3))
      .andExpect(jsonPath("$.transactions.length()").value(1));
  }

  @SneakyThrows
  @ParameterizedTest
  @MethodSource("checkInItemPatronHoldTestArguments")
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/inn-reach-transaction/pre-populate-transaction-item-shipped.sql"
  })
  void checkInPatronHoldItem_success(InnReachTransaction.TransactionState expectedState, String requestStubResponse) {
    stubPost(CHECK_IN_BY_BARCODE_URL, "circulation/checkin-response.json");

    // Stub request lookup for handleItemWithCanceledRequest
    stubGet("/circulation/requests/" + PATRON_HOLD_REQUEST_ID, requestStubResponse);

    when(innReachExternalService.postInnReachApi(any(), any(), any()))
      .thenReturn("ok");

    mockMvc.perform(post(CHECK_IN_PATRON_HOLD_ENDPOINT,
        ITEM_SHIPPED_TRANSACTION_ID, SERVICE_POINT_ID)
        .contentType(MediaType.APPLICATION_JSON)
        .headers(getOkapiHeaders()))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.transaction").exists())
      .andExpect(jsonPath("$.transaction.state").value(expectedState.name()))
      .andExpect(jsonPath("$.folioCheckIn").exists());
  }

  @SneakyThrows
  @Test
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/inn-reach-transaction/pre-populate-inn-reach-transaction.sql"
  })
  void checkInPatronHoldItem_wrongState_returnsBadRequest() {
    // Patron hold transaction is in PATRON_HOLD state, not ITEM_SHIPPED
    mockMvc.perform(post(CHECK_IN_PATRON_HOLD_ENDPOINT,
        PATRON_HOLD_TRANSACTION_ID, SERVICE_POINT_ID)
        .contentType(MediaType.APPLICATION_JSON)
        .headers(getOkapiHeaders()))
      .andExpect(status().isBadRequest());
  }

  @SneakyThrows
  @Test
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/inn-reach-transaction/pre-populate-inn-reach-transaction.sql"
  })
  void cancelItemHold_success() {
    mockMvc.perform(post(CANCEL_ITEM_HOLD_ENDPOINT, ITEM_HOLD_TRANSACTION_ID)
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"cancellationReasonId\":\"75187e8d-e25a-47a7-89ad-23ba612bb5aa\"}")
        .headers(getOkapiHeaders()))
      .andExpect(status().isNoContent());
  }

  @SneakyThrows
  @Test
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/inn-reach-transaction/pre-populate-inn-reach-broken_patron_hold_transaction.sql"
  })
  void removePatronHold_success() {
    mockMvc.perform(post(REMOVE_PATRON_HOLD_ENDPOINT, PATRON_HOLD_TRANSACTION_ID)
        .contentType(MediaType.APPLICATION_JSON)
        .headers(getOkapiHeaders()))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.state").value("CANCEL_REQUEST"));
  }

  @SneakyThrows
  @Test
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/inn-reach-transaction/pre-populate-inn-reach-transaction.sql"
  })
  void checkOutItemHoldItem_wrongBarcode_returnsNotFound() {
    mockMvc.perform(post(CHECK_OUT_ITEM_HOLD_ENDPOINT,
        "NONEXISTENT-BARCODE", SERVICE_POINT_ID)
        .contentType(MediaType.APPLICATION_JSON)
        .headers(getOkapiHeaders()))
      .andExpect(status().isNotFound());
  }

  @SneakyThrows
  @ParameterizedTest
  @MethodSource("checkInUnshippedTestArguments")
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/inn-reach-transaction/pre-populate-patron-hold-unshipped.sql"
  })
  void checkInPatronHoldUnshippedItem_success(InnReachTransaction.TransactionState expectedState, String requestStubResponse) {
    // Stub barcode lookup - no existing item with this barcode
    stubGet("/inventory/items", "inventory/empty-items-response.json",
      Map.of("query", "barcode==" + UNSHIPPED_ITEM_BARCODE));

    // Stub item find and update for addItemBarcode
    stubGet("/inventory/items/" + PATRON_HOLD_ITEM_ID, "inventory/item-response.json");
    stubPut("/inventory/items/" + PATRON_HOLD_ITEM_ID);

    // Stub check-in
    stubPost(CHECK_IN_BY_BARCODE_URL, "circulation/checkin-response.json");

    // Stub request lookup for handleItemWithCanceledRequest
    stubGet("/circulation/requests/" + PATRON_HOLD_REQUEST_ID, requestStubResponse);

    when(innReachExternalService.postInnReachApi(any(), any(), any()))
      .thenReturn("ok");

    mockMvc.perform(post(CHECK_IN_UNSHIPPED_ENDPOINT,
        PATRON_HOLD_TRANSACTION_ID, SERVICE_POINT_ID, UNSHIPPED_ITEM_BARCODE)
        .contentType(MediaType.APPLICATION_JSON)
        .headers(getOkapiHeaders()))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.transaction").exists())
      .andExpect(jsonPath("$.transaction.state").value(expectedState.name()))
      .andExpect(jsonPath("$.folioCheckIn").exists());
  }

  @SneakyThrows
  @Test
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/inn-reach-transaction/pre-populate-patron-hold-item-received.sql"
  })
  void returnPatronHoldItem_withOpenLoan_success() {
    stubGet("/circulation/loans/" + PATRON_HOLD_LOAN_ID, "circulation/open-loan-response.json");
    stubPost(CHECK_IN_BY_BARCODE_URL, "circulation/checkin-response.json");

    mockMvc.perform(post(RETURN_PATRON_HOLD_ENDPOINT,
        PATRON_HOLD_TRANSACTION_ID, SERVICE_POINT_ID)
        .contentType(MediaType.APPLICATION_JSON)
        .headers(getOkapiHeaders()))
      .andExpect(status().isNoContent());
  }

  @SneakyThrows
  @Test
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/inn-reach-transaction/pre-populate-inn-reach-transaction.sql"
  })
  void transferItemHold_sameItemAsRequest_success() {
    stubGet("/circulation/requests/" + ITEM_HOLD_REQUEST_ID, "circulation/item-hold-request-response.json");
    stubGet("/inventory/items/" + ITEM_HOLD_ITEM_ID, "inventory/item-hold-item-response.json");

    when(innReachExternalService.postInnReachApi(any(), any(), any()))
      .thenReturn("ok");

    mockMvc.perform(post(TRANSFER_ITEM_HOLD_ENDPOINT,
        ITEM_HOLD_TRANSACTION_ID, ITEM_HOLD_ITEM_ID)
        .contentType(MediaType.APPLICATION_JSON)
        .headers(getOkapiHeaders()))
      .andExpect(status().isNoContent());
  }

  @SneakyThrows
  @Test
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/inn-reach-transaction/pre-populate-patron-hold-item-received.sql"
  })
  void checkOutPatronHoldItem_success() {
    stubGet("/circulation/loans", "circulation/empty-loans-response.json", Map.of());
    stubPost(CHECK_OUT_BY_BARCODE_URL, "circulation/checkout-response.json");

    mockMvc.perform(post(CHECK_OUT_PATRON_HOLD_ENDPOINT,
        PATRON_HOLD_TRANSACTION_ID, SERVICE_POINT_ID)
        .contentType(MediaType.APPLICATION_JSON)
        .headers(getOkapiHeaders()))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.folioCheckOut").exists())
      .andExpect(jsonPath("$.transaction").exists());
  }

  @SneakyThrows
  @Test
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/inn-reach-transaction/pre-populate-inn-reach-transaction.sql"
  })
  void checkOutLocalHoldItem_success() {
    stubGet("/circulation/loans", "circulation/empty-loans-response.json", Map.of());
    stubPost(CHECK_OUT_BY_BARCODE_URL, "circulation/checkout-response.json");

    mockMvc.perform(post(CHECK_OUT_LOCAL_HOLD_ENDPOINT,
        LOCAL_HOLD_TRANSACTION_ID, SERVICE_POINT_ID)
        .contentType(MediaType.APPLICATION_JSON)
        .headers(getOkapiHeaders()))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.folioCheckOut").exists())
      .andExpect(jsonPath("$.transaction").exists());
  }

  @SneakyThrows
  @Test
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/inn-reach-transaction/pre-populate-item-hold-shipped.sql"
  })
  void finalCheckInItemHold_withOpenLoan_success() {
    stubGet("/circulation/loans/" + ITEM_HOLD_LOAN_ID, "circulation/item-hold-open-loan-response.json");
    stubPost(CHECK_IN_BY_BARCODE_URL, "circulation/checkin-response.json");

    when(innReachExternalService.postInnReachApi(any(), any(), any()))
      .thenReturn("ok");

    mockMvc.perform(post(FINAL_CHECKIN_ENDPOINT,
        ITEM_HOLD_TRANSACTION_ID, SERVICE_POINT_ID)
        .contentType(MediaType.APPLICATION_JSON)
        .headers(getOkapiHeaders()))
      .andExpect(status().isNoContent());
  }

  @SneakyThrows
  @Test
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/inn-reach-transaction/pre-populate-inn-reach-transaction.sql"
  })
  void cancelLocalHold_success() {
    stubGet("/circulation/requests/" + LOCAL_HOLD_REQUEST_ID, "circulation/local-hold-open-request-response.json");
    stubPut("/circulation/requests/" + LOCAL_HOLD_REQUEST_ID);
    stubGet("/inventory/instances/" + LOCAL_HOLD_INSTANCE_ID, "inventory/instance-response.json");

    when(innReachExternalService.postInnReachApi(any(), any(), any()))
      .thenReturn("ok");

    mockMvc.perform(post(CANCEL_LOCAL_HOLD_ENDPOINT, LOCAL_HOLD_TRANSACTION_ID)
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"cancellationReasonId\":\"75187e8d-e25a-47a7-89ad-23ba612bb5aa\"}")
        .headers(getOkapiHeaders()))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.state").value("CANCEL_REQUEST"));
  }

  private static Stream<Arguments> checkInItemPatronHoldTestArguments() {
    return Stream.of(
      Arguments.of(ITEM_RECEIVED, "circulation/open-request-response.json"),
      Arguments.of(RETURN_UNCIRCULATED, "circulation/canceled-request-response.json")
    );
  }

  private static Stream<Arguments> checkInUnshippedTestArguments() {
    return Stream.of(
      Arguments.of(RECEIVE_UNANNOUNCED, "circulation/open-request-response.json"),
      Arguments.of(RETURN_UNCIRCULATED, "circulation/canceled-request-response.json")
    );
  }
}
