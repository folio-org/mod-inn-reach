package org.folio.innreach.controller.d2ir;

import static java.lang.String.format;
import static org.folio.innreach.domain.entity.InnReachTransaction.TransactionState.CANCEL_REQUEST;
import static org.folio.util.StringUtil.cqlEncode;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.AFTER_TEST_METHOD;
import static org.springframework.test.context.jdbc.SqlMergeMode.MergeMode.MERGE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import static org.folio.innreach.fixture.CirculationFixture.createTransactionHoldDTO;
import static org.folio.innreach.util.UUIDEncoder.encode;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import lombok.SneakyThrows;
import org.folio.innreach.controller.base.BaseApiControllerTest;
import org.folio.innreach.domain.dto.OwningSiteCancelsRequestDTO;
import org.folio.innreach.domain.service.RecordContributionService;
import org.folio.innreach.external.service.InnReachExternalService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlMergeMode;

import org.folio.innreach.client.CirculationClient;
import org.folio.innreach.domain.dto.folio.circulation.RequestDTO;
import org.folio.innreach.domain.service.HoldingsService;
import org.folio.innreach.domain.service.RequestService;
import org.folio.innreach.repository.InnReachTransactionRepository;
import org.folio.innreach.util.JsonHelper;
import org.folio.innreach.util.UUIDEncoder;

@Sql(
  scripts = {
    "classpath:db/central-server/clear-central-server-tables.sql",
    "classpath:db/inn-reach-transaction/clear-inn-reach-transaction-tables.sql"
  },
  executionPhase = AFTER_TEST_METHOD
)
@SqlMergeMode(MERGE)
@ExtendWith(MockitoExtension.class)
class CirculationApiTest extends BaseApiControllerTest {

  public static final String INNREACH_LOCALSERVERS_URL = "/innreach/v2/contribution/localservers";
  public static final String HRID_SETTINGS_URL = "/hrid-settings-storage/hrid-settings";
  public static final String HOLDINGS_URL = "/holdings-storage/holdings";
  public static final String INSTANCES_URL = "/inventory/instances";
  public static final String ITEMS_URL = "/inventory/items";
  public static final String ITEMS_STORAGE_URL = "/item-storage/items";
  public static final String CIRCULATION_REQUESTS_URL = "/circulation/requests";
  public static final String USERS_URL = "/users";
  public static final String REQUEST_PREFERENCE_URL = "/request-preference-storage/request-preference";
  public static final String HOLDINGS_SOURCES_URL = "/holdings-sources";
  public static final String SERVICE_POINTS_URL = "/service-points";
  public static final String INSTANCE_TYPES_URL = "/instance-types";
  public static final String CONTRIBUTOR_NAME_TYPES_URL = "/contributor-name-types";
  public static final String LOAN_URL = "/circulation/loans/fd5109c7-8934-4294-9504-c1a4a4f07c96";
  public static final String QUERY_INSTANCE_BY_ID_URL_TEMPLATE = "/inventory/instances/%s";
  public static final String MOVE_CIRCULATION_REQUEST_URL_TEMPLATE = "/circulation/requests/%s/move";
  public static final String USER_BY_ID_URL_TEMPLATE = "/users/%s";
  private static final String HOLDING_URL = "/holdings-storage/holdings/%s";

  private static final String INSTANCE_TYPE_NAME_URLENCODED = "INN-Reach temporary record";
  private static final String INSTANCE_CONTRIBUTOR_NAME_URLENCODED = "INN-Reach author";

  private static final String PATRON_HOLD_OPERATION = "patronhold";
  private static final String ITEM_HOLD_OPERATION = "itemhold";
  private static final String LOCAL_HOLD_OPERATION = "localhold";
  private static final String CANCEL_REQ_OPERATION = "cancelrequest";
  private static final String CIRCULATION_ENDPOINT = "/inn-reach/d2ir/circ/{circulationOperationName}/{trackingId}/{centralCode}";

  private static final String PRE_POPULATED_TRACKING_ID = "tracking1";
  private static final String NEW_TRACKING_ID = "trackingNew";
  private static final String PRE_POPULATED_CENTRAL_CODE = "d2ir";
  private static final UUID PRE_POPULATED_CENTRAL_SERVER_ID = UUID.fromString("edab6baf-c696-42b1-89bb-1bbb8759b0d2");
  private static final String PRE_POPULATED_CENTRAL_AGENCY_CODE = "5east";
  private static final String PRE_POPULATED_INSTANCE_ID = "b81bcffd-9dd9-4e17-b6fd-eeecf790aad5";
  private static final UUID FOLIO_INSTANCE_ID = UUID.fromString("76834d5a-08e8-45ea-84ca-4d9b10aa341c");
  private static final String PRE_POPULATED_LOCAL_AGENCY_CODE1 = "q1w2e";
  private static final String PRE_POPULATED_LOCAL_AGENCY_CODE2 = "w2e3r";
  private static final Integer PRE_POPULATED_CENTRAL_PATRON_TYPE = 200;
  private static final String PRE_POPULATED_PATRON_ID = "ifkkmbcnljgy5elaav74pnxgxa";
  private static final int PRE_POPULATED_CENTRAL_ITEM_TYPE = 1;
  private static final UUID PRE_POPULATED_REQUEST_ID = UUID.fromString("ea11eba7-3c0f-4d15-9cca-c8608cd6bc8a");
  private static final String PRE_POPULATED_ITEM_ID = "9a326225-6530-41cc-9399-a61987bfab3c";

  private static final UUID FOLIO_PATRON_ID = UUID.fromString("ea11eba7-3c0f-4d15-9cca-c8608cd6bc8a");
  private static final UUID NEW_HOLDING_ID = UUID.fromString("16f40c4e-235d-4912-a683-2ad919cc8b07");
  private static final UUID FOLIO_HOLDING_ID = UUID.fromString("76834d5a-08e8-45ea-84ca-4d9b10aa342c");
  private static final UUID UPDATED_REQUEST_ITEM_ID = UUID.fromString("195efae1-588f-47bd-a181-13a2eb437701");
  private static final UUID UPDATED_REQUEST_INSTANCE_ID = UUID.fromString("86c722c3-2f5e-42e1-bd0e-7ffbbd3b4972");
  private static final UUID UPDATED_REQUEST_HOLDING_ID = UUID.fromString("e63273e7-48f5-4c43-ab4e-1751ecacaa21");

  private static final String ITEM_HRID = "itnewtrackingid5east";
  private static final String SERVICE_POINT_ID = "a197450b-6103-4206-8125-1a1cacc66edc";
  private static final String PAGE_REQUEST_TYPE = "Page";
  private static final String FOLIO_HOLDING_SOURCE = "FOLIO";
  private static final String PATRON_ID = "0000098765";
  private static final String USER_ID = "5d9bd03d-f031-4820-baea-6dc953ef4b7b";
  private static final String PICKUP_LOCATION = "cd1:Circ Desk 1:CD1DeliveryStop";
  private static final String PICKUP_LOCATION_CODE = "cd1";

  @MockitoBean
  private InnReachExternalService innReachExternalService;

  @MockitoBean
  private RecordContributionService recordContributionService;

  @MockitoSpyBean
  private InnReachTransactionRepository repository;

  @MockitoSpyBean
  private CirculationClient circulationClient;

  @MockitoSpyBean
  private RequestService requestService;

  @MockitoSpyBean
  private HoldingsService holdingsService;

  @Autowired
  private JsonHelper jsonHelper;

  @Captor
  private ArgumentCaptor<RequestDTO> requestDtoCaptor;

  @BeforeEach
  void setup() {
    wm.resetAll();
    reset(repository, circulationClient, requestService, holdingsService);
  }

  @SneakyThrows
  @Test
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/patron-type-mapping/pre-populate-patron-type-mapping.sql",
    "classpath:db/agency-loc-mapping/pre-populate-agency-location-mapping.sql",
    "classpath:db/item-type-mapping/pre-populate-item-type-mapping.sql",
    "classpath:db/mtype-mapping/pre-populate-material-type-mapping.sql",
    "classpath:db/central-patron-type-mapping/pre-populate-central-patron_type-mapping-table.sql"
  })
  void issueOwningSideCancelsAndReContributeItem_when_createInnReachTransaction_and_creatingRequestFails() {
    var transactionHoldDTO = createTransactionHoldDTO();
    transactionHoldDTO.setItemId(ITEM_HRID);
    transactionHoldDTO.setCentralPatronType(PRE_POPULATED_CENTRAL_PATRON_TYPE);
    var owningSiteCancelPath = "/circ/owningsitecancel/%s/%s".formatted(NEW_TRACKING_ID, PRE_POPULATED_CENTRAL_CODE);
    stubGet(ITEMS_URL, "inventory/query-items-response.json", Map.of("query", "hrid==" + ITEM_HRID));
    stubGet(format(HOLDING_URL, NEW_HOLDING_ID), "inventory-storage/holding-response.json");
    stubGet(format(QUERY_INSTANCE_BY_ID_URL_TEMPLATE, PRE_POPULATED_INSTANCE_ID), "inventory/instance-response.json");
    stubGet(USERS_URL, "users/query-users-response.json", Map.of("query", "barcode==" + cqlEncode(PATRON_ID)));
    stubGet(REQUEST_PREFERENCE_URL, "request-preference-storage/query-request-preference-response.json",
      Map.of("query", "userId==" + cqlEncode(USER_ID)));
    stubGet(CIRCULATION_REQUESTS_URL, "circulation/empty-requests-response.json", Map.of("query", "itemId==" + cqlEncode(PRE_POPULATED_ITEM_ID)));
    stubPost(CIRCULATION_REQUESTS_URL, "circulation/not-allowed-error-response.json",
      respAction -> respAction.withStatus(422), MappingActions.none());
    stubGet(ITEMS_STORAGE_URL, "item-storage/query-items-response.json", Map.of("query", "hrid==" + cqlEncode(ITEM_HRID)));
    when(innReachExternalService.postInnReachApi(eq(PRE_POPULATED_CENTRAL_CODE), eq(owningSiteCancelPath), any(OwningSiteCancelsRequestDTO.class)))
      .thenReturn("success");
    when(recordContributionService.contributeItems(eq(PRE_POPULATED_CENTRAL_SERVER_ID), any(), anyList()))
      .thenReturn(1);

    mockMvc.perform(post(CIRCULATION_ENDPOINT, ITEM_HOLD_OPERATION, NEW_TRACKING_ID, PRE_POPULATED_CENTRAL_CODE)
        .content(jsonHelper.toJson(transactionHoldDTO))
        .contentType(MediaType.APPLICATION_JSON)
        .headers(getOkapiHeaders()))
      .andExpect(status().isOk());

    awaitAssertion(() -> {
      var innReachTransaction = repository.findByTrackingIdAndCentralServerCode(
        NEW_TRACKING_ID, PRE_POPULATED_CENTRAL_CODE).orElse(null);

      assertNotNull(innReachTransaction);
      assertEquals(CANCEL_REQUEST, innReachTransaction.getState());
    });

    verify(innReachExternalService).postInnReachApi(eq(PRE_POPULATED_CENTRAL_CODE), eq(owningSiteCancelPath), any());
    awaitAssertion(() ->
      verify(recordContributionService).contributeItems(eq(PRE_POPULATED_CENTRAL_SERVER_ID), any(), anyList()));
  }

  @Test
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/agency-loc-mapping/pre-populate-agency-location-mapping.sql",
    "classpath:db/item-type-mapping/pre-populate-item-type-mapping.sql",
    "classpath:db/inn-reach-transaction/pre-populate-inn-reach-transaction.sql",
    "classpath:db/patron-type-mapping/pre-populate-patron-type-mapping-circulation.sql"
  })
  void patronHold_updateVirtualItems() throws Exception {
    var transactionHoldDTO = createTransactionHoldDTO();
    transactionHoldDTO.setItemAgencyCode(PRE_POPULATED_CENTRAL_AGENCY_CODE);
    transactionHoldDTO.setCentralItemType(PRE_POPULATED_CENTRAL_ITEM_TYPE);
    transactionHoldDTO.setPatronId(PRE_POPULATED_PATRON_ID);
    var patronId = UUIDEncoder.decode(transactionHoldDTO.getPatronId());

    stubGet(format(USER_BY_ID_URL_TEMPLATE, patronId), "users/user.json");
    stubGet(HRID_SETTINGS_URL, "inventory-storage/hrid-settings-response.json");
    stubGet(INSTANCES_URL, "inventory/query-instance-response.json", Map.of("query", "(hrid==intracking1d2ir)"));
    stubGet(INNREACH_LOCALSERVERS_URL, "agency-codes/d2ir-local-servers-response-01.json");
    stubGet(HOLDINGS_SOURCES_URL, "inventory-storage/holding-sources-response.json",
      Map.of("query", "name==" + cqlEncode(FOLIO_HOLDING_SOURCE), "limit", "1"));
    stubPost(HOLDINGS_URL, "inventory-storage/holding-response.json");
    stubPost(ITEMS_URL, "inventory/item-response.json");
    stubGet(ITEMS_URL, "inventory/query-items-response.json", Map.of("query", "hrid==" + cqlEncode("ittracking15east")));
    stubGet(CIRCULATION_REQUESTS_URL, "circulation/empty-requests-response.json", Map.of("query", "itemId==(" + cqlEncode(PRE_POPULATED_ITEM_ID) + ")"));
    stubPost(format(MOVE_CIRCULATION_REQUEST_URL_TEMPLATE, PRE_POPULATED_REQUEST_ID), "circulation/updated-request-response.json");

    mockMvc.perform(post(CIRCULATION_ENDPOINT, PATRON_HOLD_OPERATION, PRE_POPULATED_TRACKING_ID, PRE_POPULATED_CENTRAL_CODE)
        .content(jsonHelper.toJson(transactionHoldDTO))
        .contentType(MediaType.APPLICATION_JSON)
        .headers(getOkapiHeaders()))
      .andExpect(status().isOk());

    awaitAssertion(() ->
      verify(repository).save(argThat(t -> UPDATED_REQUEST_ITEM_ID.equals(t.getHold().getFolioItemId()))));

    verify(holdingsService).create(argThat(holding -> holding.getSourceId() != null));
    verify(circulationClient).moveRequest(eq(PRE_POPULATED_REQUEST_ID), any());

    var updatedTransaction = repository.fetchOneByTrackingId(PRE_POPULATED_TRACKING_ID).orElseThrow();
    var updatedHold = updatedTransaction.getHold();

    assertEquals(PRE_POPULATED_REQUEST_ID, updatedHold.getFolioRequestId());
    assertEquals(UPDATED_REQUEST_HOLDING_ID, updatedHold.getFolioHoldingId());
    assertEquals(UPDATED_REQUEST_INSTANCE_ID, updatedHold.getFolioInstanceId());
  }

  @Test
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/agency-loc-mapping/pre-populate-agency-location-mapping.sql",
    "classpath:db/item-type-mapping/pre-populate-item-type-mapping.sql",
    "classpath:db/inn-reach-transaction/pre-populate-inn-reach-transaction.sql",
    "classpath:db/patron-type-mapping/pre-populate-patron-type-mapping-circulation.sql"
  })
  void patronHold_createVirtualItems() throws Exception {
    var transactionHoldDTO = createTransactionHoldDTO();
    transactionHoldDTO.setItemId(ITEM_HRID);
    transactionHoldDTO.setItemAgencyCode(PRE_POPULATED_CENTRAL_AGENCY_CODE);
    transactionHoldDTO.setCentralItemType(PRE_POPULATED_CENTRAL_ITEM_TYPE);
    transactionHoldDTO.setPatronId(PRE_POPULATED_PATRON_ID);
    transactionHoldDTO.setPickupLocation(PICKUP_LOCATION);
    var patronId = UUIDEncoder.decode(transactionHoldDTO.getPatronId());

    stubGet(format(USER_BY_ID_URL_TEMPLATE, patronId), "users/user.json");
    stubGet(format(USER_BY_ID_URL_TEMPLATE, FOLIO_PATRON_ID), "users/user-response.json");
    stubGet(INSTANCE_TYPES_URL, "inventory-storage/query-instance-types-response.json", Map.of("query", "name==" + cqlEncode(INSTANCE_TYPE_NAME_URLENCODED)));
    stubGet(SERVICE_POINTS_URL, "inventory-storage/query-service-points-response.json", Map.of("query", "code==" + cqlEncode(PICKUP_LOCATION_CODE)));
    stubGet(CONTRIBUTOR_NAME_TYPES_URL, "inventory-storage/query-contributor-name-types-response.json", Map.of("query", "name==" + cqlEncode(INSTANCE_CONTRIBUTOR_NAME_URLENCODED)));
    stubGet(HRID_SETTINGS_URL, "inventory-storage/hrid-settings-response.json");
    stubGet(INSTANCES_URL, "inventory/query-instance-response.json", Map.of("query", "(hrid==innewtrackingidd2ir)"));
    stubGet(INNREACH_LOCALSERVERS_URL, "agency-codes/d2ir-local-servers-response-01.json");
    stubPost(INSTANCES_URL, "inventory/instance-response.json");
    stubGet(HOLDINGS_SOURCES_URL, "inventory-storage/holding-sources-response.json", Map.of("query", "name==" + cqlEncode(FOLIO_HOLDING_SOURCE), "limit", "1"));
    stubPost(HOLDINGS_URL, "inventory-storage/holding-response.json");
    stubPost(ITEMS_URL, "inventory/item-response.json");
    stubPost(CIRCULATION_REQUESTS_URL, "circulation/item-request-response.json");
    stubGet(ITEMS_URL, "inventory/query-items-response.json", Map.of("query", "hrid==" + cqlEncode(ITEM_HRID)));
    stubGet(CIRCULATION_REQUESTS_URL, "inventory/query-items-response.json", Map.of("query", "(itemId==" + PRE_POPULATED_ITEM_ID + ")"));

    mockMvc.perform(post(CIRCULATION_ENDPOINT, PATRON_HOLD_OPERATION, "newtrackingid", PRE_POPULATED_CENTRAL_CODE)
        .content(jsonHelper.toJson(transactionHoldDTO))
        .contentType(MediaType.APPLICATION_JSON)
        .headers(getOkapiHeaders()))
      .andExpect(status().isOk());

    awaitAssertion(() -> verify(circulationClient).sendRequest(any()));

    verify(holdingsService).create(argThat(holding -> holding.getSourceId() != null));
  }

  @Test
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/agency-loc-mapping/pre-populate-agency-location-mapping.sql",
    "classpath:db/item-type-mapping/pre-populate-item-type-mapping.sql",
    "classpath:db/inn-reach-transaction/pre-populate-inn-reach-transaction.sql",
    "classpath:db/patron-type-mapping/pre-populate-patron-type-mapping-circulation.sql"
  })
  void patronHold_cancelRequest() throws Exception {
    Map<String, Object> requestPayload = Map
      .of("transactionTime", Instant.now().getEpochSecond(),
        "patronId", "12534",
        "patronAgencyCode", "12345",
        "itemAgencyCode", PRE_POPULATED_CENTRAL_AGENCY_CODE,
        "itemId", ITEM_HRID,
        "reason", "Test reason",
        "reasonCode", 7);

    var transactionHoldDTO = createTransactionHoldDTO();
    transactionHoldDTO.setPatronId(PRE_POPULATED_PATRON_ID);
    transactionHoldDTO.setItemId(ITEM_HRID);
    transactionHoldDTO.setItemAgencyCode(PRE_POPULATED_CENTRAL_AGENCY_CODE);
    transactionHoldDTO.setCentralItemType(PRE_POPULATED_CENTRAL_ITEM_TYPE);
    var patronId = UUIDEncoder.decode(transactionHoldDTO.getPatronId());
    stubGet(format(USER_BY_ID_URL_TEMPLATE, patronId), "users/user.json");
    stubGet(format("%s/%s", ITEMS_URL, PRE_POPULATED_ITEM_ID), "inventory/item-response.json");
    stubPut(format("%s/%s", ITEMS_URL, PRE_POPULATED_ITEM_ID));
    stubDelete(format("%s/%s", ITEMS_URL, PRE_POPULATED_ITEM_ID));
    stubGet(format("%s/%s", HOLDINGS_URL, NEW_HOLDING_ID), "inventory-storage/holding-response.json");
    stubPut(format("%s/%s", HOLDINGS_URL, NEW_HOLDING_ID));
    stubDelete(format("%s/%s", HOLDINGS_URL, FOLIO_HOLDING_ID));
    stubGet(format("%s/%s", CIRCULATION_REQUESTS_URL, PRE_POPULATED_REQUEST_ID), "circulation/item-request-response.json");
    stubPut(format("%s/%s", CIRCULATION_REQUESTS_URL, PRE_POPULATED_REQUEST_ID));
    stubDelete(format("%s/%s", INSTANCES_URL, FOLIO_INSTANCE_ID));
    stubGet(LOAN_URL, "circulation/loan-response.json");
    stubDelete(LOAN_URL);

    mockMvc.perform(put(CIRCULATION_ENDPOINT, CANCEL_REQ_OPERATION, PRE_POPULATED_TRACKING_ID, PRE_POPULATED_CENTRAL_CODE)
        .content(jsonHelper.toJson(requestPayload))
        .contentType(MediaType.APPLICATION_JSON)
        .headers(getOkapiHeaders()))
      .andExpect(status().isOk());

    verify(requestService).cancelRequest(anyString(), any(UUID.class), any(UUID.class), eq("Test reason"));
    verify(circulationClient).updateRequest(any(), any());
  }

  @Test
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/agency-loc-mapping/pre-populate-agency-location-mapping.sql",
    "classpath:db/item-type-mapping/pre-populate-item-type-mapping.sql",
    "classpath:db/inn-reach-transaction/pre-populate-inn-reach-transaction.sql"
  })
  void patronHold_cancelRequest_badRequest() throws Exception {
    Map<String, Object> requestPayload = Map
      .of("transactionTime", Instant.now().getEpochSecond(),
        "patronId", "12534",
        "patronAgencyCode", "invalid_code",
        "itemAgencyCode", PRE_POPULATED_CENTRAL_AGENCY_CODE,
        "itemId", ITEM_HRID,
        "reason", "Test reason",
        "reasonCode", 7);

    stubGet(format("%s/%s", ITEMS_URL, PRE_POPULATED_ITEM_ID), "inventory/item-response.json");
    stubPut(format("%s/%s", ITEMS_URL, PRE_POPULATED_ITEM_ID));
    stubGet(format("%s/%s", HOLDINGS_URL, NEW_HOLDING_ID), "inventory-storage/holding-response.json");
    stubPut(format("%s/%s", HOLDINGS_URL, NEW_HOLDING_ID));
    stubGet(format("%s/%s", CIRCULATION_REQUESTS_URL, PRE_POPULATED_REQUEST_ID), "circulation/item-request-response.json");
    stubPut(format("%s/%s", CIRCULATION_REQUESTS_URL, PRE_POPULATED_REQUEST_ID));

    mockMvc.perform(put(CIRCULATION_ENDPOINT, CANCEL_REQ_OPERATION, PRE_POPULATED_TRACKING_ID, PRE_POPULATED_CENTRAL_CODE)
        .content(jsonHelper.toJson(requestPayload))
        .contentType(MediaType.APPLICATION_JSON)
        .headers(getOkapiHeaders()))
      .andExpect(status().isBadRequest());

    verifyNoInteractions(requestService);
  }

  @Test
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/agency-loc-mapping/pre-populate-agency-location-mapping.sql",
    "classpath:db/item-type-mapping/pre-populate-item-type-mapping.sql",
    "classpath:db/inn-reach-transaction/pre-populate-inn-reach-transaction.sql"
  })
  void localHold_createRequest() throws Exception {
    var transactionHoldDTO = createTransactionHoldDTO();
    transactionHoldDTO.setItemId(ITEM_HRID);
    transactionHoldDTO.setItemAgencyCode(PRE_POPULATED_LOCAL_AGENCY_CODE1);
    transactionHoldDTO.setPatronAgencyCode(PRE_POPULATED_LOCAL_AGENCY_CODE2);
    transactionHoldDTO.setCentralItemType(PRE_POPULATED_CENTRAL_ITEM_TYPE);
    transactionHoldDTO.setPatronId(encode(FOLIO_PATRON_ID));
    transactionHoldDTO.setPickupLocation(PICKUP_LOCATION);

    stubGet(format(USER_BY_ID_URL_TEMPLATE, FOLIO_PATRON_ID), "users/user-response.json");
    stubGet(SERVICE_POINTS_URL, "inventory-storage/query-service-points-response.json", Map.of("query", "code==" + cqlEncode(PICKUP_LOCATION_CODE)));
    stubPost(HOLDINGS_URL, "inventory-storage/holding-response.json");
    stubPost(ITEMS_URL, "inventory/item-response.json");
    stubPost(CIRCULATION_REQUESTS_URL, "circulation/item-request-response.json");
    stubGet(ITEMS_URL, "inventory/query-items-response.json", Map.of("query", "hrid==" + ITEM_HRID));
    stubGet(CIRCULATION_REQUESTS_URL, "circulation/empty-requests-response.json",
      Map.of("query", "(itemId==" + cqlEncode(PRE_POPULATED_ITEM_ID) + ")"));
    stubGet(format(HOLDING_URL, NEW_HOLDING_ID), "inventory-storage/holding-response.json");

    mockMvc.perform(put(CIRCULATION_ENDPOINT, LOCAL_HOLD_OPERATION, "newtrackingid", PRE_POPULATED_CENTRAL_CODE)
        .content(jsonHelper.toJson(transactionHoldDTO))
        .contentType(MediaType.APPLICATION_JSON)
        .headers(getOkapiHeaders()))
      .andExpect(status().isOk());

    awaitAssertion(() -> verify(circulationClient).sendRequest(requestDtoCaptor.capture()));

    var requestDTO = requestDtoCaptor.getValue();
    assertEquals(PRE_POPULATED_INSTANCE_ID, requestDTO.getInstanceId().toString());
    assertEquals(UUID.fromString(PRE_POPULATED_ITEM_ID), requestDTO.getItemId());
    assertEquals(NEW_HOLDING_ID, requestDTO.getHoldingsRecordId());
    assertEquals(RequestDTO.RequestLevel.ITEM.getName(), requestDTO.getRequestLevel());
    assertEquals(PAGE_REQUEST_TYPE, requestDTO.getRequestType());
    assertEquals(FOLIO_PATRON_ID, requestDTO.getRequesterId());
    assertEquals(SERVICE_POINT_ID, requestDTO.getPickupServicePointId().toString());
  }

}
