package org.folio.innreach.controller.d2ir;

import static java.lang.String.format;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.AFTER_TEST_METHOD;
import static org.springframework.test.context.jdbc.SqlMergeMode.MergeMode.MERGE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import static org.folio.innreach.fixture.CirculationFixture.createTransactionHoldDTO;
import static org.folio.innreach.util.UUIDHelper.toStringWithoutHyphens;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import org.folio.innreach.domain.dto.folio.circulation.RequestDTO;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlMergeMode;

import org.folio.innreach.client.CirculationClient;
import org.folio.innreach.controller.base.BaseApiControllerTest;
import org.folio.innreach.domain.entity.InnReachTransaction;
import org.folio.innreach.domain.service.RequestService;
import org.folio.innreach.mapper.InnReachTransactionPickupLocationMapper;
import org.folio.innreach.repository.InnReachTransactionRepository;
import org.folio.innreach.util.JsonHelper;

@Sql(
  scripts = {
    "classpath:db/central-server/clear-central-server-tables.sql",
    "classpath:db/inn-reach-transaction/clear-inn-reach-transaction-tables.sql"
  },
  executionPhase = AFTER_TEST_METHOD
)
@SqlMergeMode(MERGE)
class CirculationApiTest extends BaseApiControllerTest {

  public static final String INNREACH_LOCALSERVERS_URL = "/innreach/v2/contribution/localservers";
  public static final String HRID_SETTINGS_URL = "/hrid-settings-storage/hrid-settings";
  public static final String HOLDINGS_URL = "/holdings-storage/holdings";
  public static final String INSTANCES_URL = "/inventory/instances";
  public static final String ITEMS_URL = "/inventory/items";
  public static final String REQUESTS_URL = "/circulation/requests";
  public static final String QUERY_INSTANCE_BY_HRID_URL_TEMPLATE = "/inventory/instances?query=(hrid==%s)";
  public static final String QUERY_INVENTORY_ITEM_BY_HRID_URL_TEMPLATE = "/inventory/items?query=hrid==%s";
  public static final String QUERY_REQUEST_BY_ITEM_ID_URL_TEMPLATE = "/circulation/requests?query=(itemId==%s)";
  public static final String MOVE_CIRCULATION_REQUEST_URL_TEMPLATE = "/circulation/requests/%s/move";
  public static final String QUERY_CONTRIBUTOR_TYPE_BY_NAME_URL_TEMPLATE = "/contributor-name-types?query=(name==%s)";
  public static final String QUERY_INSTANCE_TYPE_BY_NAME_URL_TEMPLATE = "/instance-types?query=(name==%s)";
  public static final String QUERY_SERVICE_POINTS_BY_CODE_ULR_TEMPLATE = "/service-points?query=code==%s";
  public static final String USER_BY_ID_URL_TEMPLATE = "/users/%s";

  private static final String INSTANCE_TYPE_NAME_URLENCODED = "INN-Reach%20temporary%20record";
  private static final String INSTANCE_CONTRIBUTOR_NAME_URLENCODED = "INN-Reach%20author";

  private static final String PATRON_HOLD_OPERATION = "patronhold";
  private static final String LOCAL_HOLD_OPERATION = "localhold";
  private static final String CANCEL_REQ_OPERATION = "cancelrequest";
  private static final String CIRCULATION_ENDPOINT = "/inn-reach/d2ir/circ/{circulationOperationName}/{trackingId}/{centralCode}";

  private static final String PRE_POPULATED_TRACKING_ID = "tracking1";
  private static final String PRE_POPULATED_CENTRAL_CODE = "d2ir";
  private static final String PRE_POPULATED_CENTRAL_AGENCY_CODE = "5east";
  private static final int PRE_POPULATED_CENTRAL_ITEM_TYPE = 1;
  private static final UUID PRE_POPULATED_REQUEST_ID = UUID.fromString("ea11eba7-3c0f-4d15-9cca-c8608cd6bc8a");
  private static final UUID PRE_POPULATED_ITEM_ID = UUID.fromString("9a326225-6530-41cc-9399-a61987bfab3c");

  private static final UUID NEW_REQUEST_ID = UUID.fromString("89105c06-dbdb-4aa0-9695-d4d19c733270");
  private static final String ITEM_HRID = "itnewtrackingid5east";
  private static final String HOLDING_ID = "16f40c4e-235d-4912-a683-2ad919cc8b07";
  private static final UUID FOLIO_PATRON_ID = UUID.fromString("ea11eba7-3c0f-4d15-9cca-c8608cd6bc8a");

  private static final Duration ASYNC_AWAIT_TIMEOUT = Duration.ofSeconds(15);
  private static final String ITEM_ID = "9a326225-6530-41cc-9399-a61987bfab3c";
  private static final String SERVICE_POINT_ID = "a197450b-6103-4206-8125-1a1cacc66edc";
  private static final String PAGE = "Page";
  private static final String HOLDINGS_RECORD_ID = "16f40c4e-235d-4912-a683-2ad919cc8b07";
  private static final String PRE_POPULATED_INSTANCE_ID = "b81bcffd-9dd9-4e17-b6fd-eeecf790aad5";
  private static final String HOLDING_URL = "/holdings-storage/holdings/%s";

  @SpyBean
  private InnReachTransactionRepository repository;

  @SpyBean
  private CirculationClient circulationClient;

  @SpyBean
  private RequestService requestService;

  @Autowired
  private JsonHelper jsonHelper;

  @Autowired
  private InnReachTransactionPickupLocationMapper pickupLocationMapper;

  @Captor
  ArgumentCaptor<RequestDTO> requestDtoCaptor;

  @Test
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/agency-loc-mapping/pre-populate-agency-location-mapping.sql",
    "classpath:db/item-type-mapping/pre-populate-item-type-mapping.sql",
    "classpath:db/inn-reach-transaction/pre-populate-inn-reach-transaction.sql"
  })
  void patronHold_updateVirtualItems() throws Exception {
    var transactionHoldDTO = createTransactionHoldDTO();
    transactionHoldDTO.setItemAgencyCode(PRE_POPULATED_CENTRAL_AGENCY_CODE);
    transactionHoldDTO.setCentralItemType(PRE_POPULATED_CENTRAL_ITEM_TYPE);

    stubGet(HRID_SETTINGS_URL, "inventory-storage/hrid-settings-response.json");
    stubGet(format(QUERY_INSTANCE_BY_HRID_URL_TEMPLATE, "intracking1d2ir"), "inventory/query-instance-response.json");
    stubGet(INNREACH_LOCALSERVERS_URL, "agency-codes/d2ir-local-servers-response-01.json");
    stubPost(HOLDINGS_URL, "inventory-storage/holding-response.json");
    stubPost(ITEMS_URL, "inventory/item-response.json");
    stubGet(format(QUERY_INVENTORY_ITEM_BY_HRID_URL_TEMPLATE, "ittracking15east"), "inventory/query-items-response.json");
    stubGet(format(QUERY_REQUEST_BY_ITEM_ID_URL_TEMPLATE, PRE_POPULATED_ITEM_ID), "circulation/empty-requests-response.json");
    stubPost(format(MOVE_CIRCULATION_REQUEST_URL_TEMPLATE, PRE_POPULATED_REQUEST_ID), "circulation/updated-request-response.json");

    mockMvc.perform(post(CIRCULATION_ENDPOINT, PATRON_HOLD_OPERATION, PRE_POPULATED_TRACKING_ID, PRE_POPULATED_CENTRAL_CODE)
        .content(jsonHelper.toJson(transactionHoldDTO))
        .contentType(MediaType.APPLICATION_JSON)
        .headers(getOkapiHeaders()))
      .andExpect(status().isOk());

    await().atMost(ASYNC_AWAIT_TIMEOUT).untilAsserted(() ->
      verify(repository).save(
        argThat((InnReachTransaction t) -> NEW_REQUEST_ID.equals(t.getHold().getFolioRequestId()))));
  }

  @Test
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/agency-loc-mapping/pre-populate-agency-location-mapping.sql",
    "classpath:db/item-type-mapping/pre-populate-item-type-mapping.sql",
    "classpath:db/inn-reach-transaction/pre-populate-inn-reach-transaction.sql"
  })
  void patronHold_createVirtualItems() throws Exception {
    var transactionHoldDTO = createTransactionHoldDTO();
    transactionHoldDTO.setItemId(ITEM_HRID);
    transactionHoldDTO.setItemAgencyCode(PRE_POPULATED_CENTRAL_AGENCY_CODE);
    transactionHoldDTO.setCentralItemType(PRE_POPULATED_CENTRAL_ITEM_TYPE);
    transactionHoldDTO.setPatronId(toStringWithoutHyphens(FOLIO_PATRON_ID));
    var pickupLocation = pickupLocationMapper.fromString(transactionHoldDTO.getPickupLocation());

    stubGet(format(USER_BY_ID_URL_TEMPLATE, FOLIO_PATRON_ID), "users/user-response.json");
    stubGet(format(QUERY_INSTANCE_TYPE_BY_NAME_URL_TEMPLATE, INSTANCE_TYPE_NAME_URLENCODED), "inventory-storage/query-instance-types-response.json");
    stubGet(format(QUERY_SERVICE_POINTS_BY_CODE_ULR_TEMPLATE, pickupLocation.getPickupLocCode()), "inventory-storage/query-service-points-response.json");
    stubGet(format(QUERY_CONTRIBUTOR_TYPE_BY_NAME_URL_TEMPLATE, INSTANCE_CONTRIBUTOR_NAME_URLENCODED), "inventory-storage/query-contributor-name-types-response.json");
    stubGet(HRID_SETTINGS_URL, "inventory-storage/hrid-settings-response.json");
    stubGet(format(QUERY_INSTANCE_BY_HRID_URL_TEMPLATE, "innewtrackingidd2ir"), "inventory/query-instance-response.json");
    stubGet(INNREACH_LOCALSERVERS_URL, "agency-codes/d2ir-local-servers-response-01.json");
    stubPost(INSTANCES_URL, "inventory/instance-response.json");
    stubPost(HOLDINGS_URL, "inventory-storage/holding-response.json");
    stubPost(ITEMS_URL, "inventory/item-response.json");
    stubPost(REQUESTS_URL, "circulation/item-request-response.json");
    stubGet(format(QUERY_INVENTORY_ITEM_BY_HRID_URL_TEMPLATE, ITEM_HRID), "inventory/query-items-response.json");
    stubGet(format(QUERY_REQUEST_BY_ITEM_ID_URL_TEMPLATE, PRE_POPULATED_ITEM_ID), "circulation/empty-requests-response.json");

    mockMvc.perform(post(CIRCULATION_ENDPOINT, PATRON_HOLD_OPERATION, "newtrackingid", PRE_POPULATED_CENTRAL_CODE)
        .content(jsonHelper.toJson(transactionHoldDTO))
        .contentType(MediaType.APPLICATION_JSON)
        .headers(getOkapiHeaders()))
      .andExpect(status().isOk());

    await().atMost(ASYNC_AWAIT_TIMEOUT).untilAsserted(() ->
      verify(circulationClient).sendRequest(any()));
  }

  @Test
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/agency-loc-mapping/pre-populate-agency-location-mapping.sql",
    "classpath:db/item-type-mapping/pre-populate-item-type-mapping.sql",
    "classpath:db/inn-reach-transaction/pre-populate-inn-reach-transaction.sql"
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

    stubGet(format("%s/%s", ITEMS_URL, PRE_POPULATED_ITEM_ID), "inventory/item-response.json");
    stubPut(format("%s/%s", ITEMS_URL, PRE_POPULATED_ITEM_ID));
    stubGet(format("%s/%s", HOLDINGS_URL, HOLDING_ID), "inventory-storage/holding-response.json");
    stubPut(format("%s/%s", HOLDINGS_URL, HOLDING_ID));
    stubGet(format("%s/%s", REQUESTS_URL, PRE_POPULATED_REQUEST_ID), "circulation/item-request-response.json");
    stubPut(format("%s/%s", REQUESTS_URL, PRE_POPULATED_REQUEST_ID));

    mockMvc.perform(put(CIRCULATION_ENDPOINT, CANCEL_REQ_OPERATION, PRE_POPULATED_TRACKING_ID, PRE_POPULATED_CENTRAL_CODE)
        .content(jsonHelper.toJson(requestPayload))
        .contentType(MediaType.APPLICATION_JSON)
        .headers(getOkapiHeaders()))
      .andExpect(status().isOk());

    verify(requestService).cancelRequest(any(), eq("Test reason"));
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
    stubGet(format("%s/%s", HOLDINGS_URL, HOLDING_ID), "inventory-storage/holding-response.json");
    stubPut(format("%s/%s", HOLDINGS_URL, HOLDING_ID));
    stubGet(format("%s/%s", REQUESTS_URL, PRE_POPULATED_REQUEST_ID), "circulation/item-request-response.json");
    stubPut(format("%s/%s", REQUESTS_URL, PRE_POPULATED_REQUEST_ID));

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
    transactionHoldDTO.setItemAgencyCode(PRE_POPULATED_CENTRAL_AGENCY_CODE);
    transactionHoldDTO.setPatronAgencyCode(PRE_POPULATED_CENTRAL_AGENCY_CODE);
    transactionHoldDTO.setCentralItemType(PRE_POPULATED_CENTRAL_ITEM_TYPE);
    transactionHoldDTO.setPatronId(toStringWithoutHyphens(FOLIO_PATRON_ID));
    var pickupLocation = pickupLocationMapper.fromString(transactionHoldDTO.getPickupLocation());

    stubGet(format(USER_BY_ID_URL_TEMPLATE, FOLIO_PATRON_ID), "users/user-response.json");
    stubGet(format(QUERY_SERVICE_POINTS_BY_CODE_ULR_TEMPLATE, pickupLocation.getPickupLocCode()), "inventory-storage/query-service-points-response.json");
    stubPost(HOLDINGS_URL, "inventory-storage/holding-response.json");
    stubPost(ITEMS_URL, "inventory/item-response.json");
    stubPost(REQUESTS_URL, "circulation/item-request-response.json");
    stubGet(format(QUERY_INVENTORY_ITEM_BY_HRID_URL_TEMPLATE, ITEM_HRID), "inventory/query-items-response.json");
    stubGet(format(QUERY_REQUEST_BY_ITEM_ID_URL_TEMPLATE, PRE_POPULATED_ITEM_ID), "circulation/empty-requests-response.json");
    stubGet(format(HOLDING_URL, HOLDINGS_RECORD_ID), "inventory-storage/holding-response.json");

    mockMvc.perform(put(CIRCULATION_ENDPOINT, LOCAL_HOLD_OPERATION, "newtrackingid", PRE_POPULATED_CENTRAL_CODE)
        .content(jsonHelper.toJson(transactionHoldDTO))
        .contentType(MediaType.APPLICATION_JSON)
        .headers(getOkapiHeaders()))
      .andExpect(status().isOk());

    await().atMost(ASYNC_AWAIT_TIMEOUT).untilAsserted(() ->
      verify(circulationClient).sendRequest(requestDtoCaptor.capture()));
    RequestDTO requestDTO = requestDtoCaptor.getValue();
    assertEquals(PRE_POPULATED_INSTANCE_ID, requestDTO.getInstanceId().toString());
    assertEquals(ITEM_ID, requestDTO.getItemId().toString());
    assertEquals(HOLDINGS_RECORD_ID, requestDTO.getHoldingsRecordId().toString());
    assertEquals(RequestDTO.RequestLevel.ITEM.getName(), requestDTO.getRequestLevel());
    assertEquals(PAGE, requestDTO.getRequestType());
    assertEquals(FOLIO_PATRON_ID, requestDTO.getRequesterId());
    assertEquals(SERVICE_POINT_ID, requestDTO.getPickupServicePointId().toString());
  }

}
