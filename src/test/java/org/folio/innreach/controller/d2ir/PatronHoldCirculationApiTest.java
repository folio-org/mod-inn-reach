package org.folio.innreach.controller.d2ir;

import static java.lang.String.format;
import static org.awaitility.Awaitility.await;
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

import static org.folio.innreach.domain.CirculationOperation.CANCEL_PATRON_HOLD;
import static org.folio.innreach.domain.CirculationOperation.PATRON_HOLD;
import static org.folio.innreach.fixture.CirculationFixture.createTransactionHoldDTO;
import static org.folio.innreach.util.UUIDHelper.toStringWithoutHyphens;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlMergeMode;

import org.folio.innreach.client.RequestStorageClient;
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
class PatronHoldCirculationApiTest extends BaseApiControllerTest {

  public static final String INNREACH_LOCALSERVERS_URL = "/innreach/v2/contribution/localservers";
  public static final String HRID_SETTINGS_URL = "/hrid-settings-storage/hrid-settings";
  public static final String HOLDINGS_URL = "/holdings-storage/holdings";
  public static final String INSTANCES_URL = "/instance-storage/instances";
  public static final String ITEMS_URL = "/item-storage/items";
  public static final String REQUESTS_URL = "/request-storage/requests";
  public static final String QUERY_INSTANCE_BY_HRID_URL_TEMPLATE = "/instance-storage/instances?query=(hrid==%s)";
  public static final String QUERY_INVENTORY_ITEM_BY_HRID_URL_TEMPLATE = "/inventory/items?query=hrid==%s";
  public static final String QUERY_REQUEST_BY_ITEM_ID_URL_TEMPLATE = "/request-storage/requests?query=(itemId==%s)";
  public static final String MOVE_CIRCULATION_REQUEST_URL_TEMPLATE = "/circulation/requests/%s/move";
  public static final String QUERY_CONTRIBUTOR_TYPE_BY_NAME_URL_TEMPLATE = "/contributor-name-types?query=(name==%s)";
  public static final String QUERY_INSTANCE_TYPE_BY_NAME_URL_TEMPLATE = "/instance-types?query=(name==%s)";
  public static final String QUERY_SERVICE_POINTS_BY_CODE_ULR_TEMPLATE = "/service-points?query=code==%s";

  private static final String INSTANCE_TYPE_NAME_URLENCODED = "INN-Reach%20temporary%20record";
  private static final String INSTANCE_CONTRIBUTOR_NAME_URLENCODED = "INN-Reach%20author";

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

  @SpyBean
  private InnReachTransactionRepository repository;

  @SpyBean
  private RequestStorageClient requestStorageClient;

  @SpyBean
  private RequestService requestService;

  @Autowired
  private JsonHelper jsonHelper;

  @Autowired
  private InnReachTransactionPickupLocationMapper pickupLocationMapper;

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
    stubGet(format(QUERY_INSTANCE_BY_HRID_URL_TEMPLATE, "intracking1d2ir"), "inventory-storage/query-instance-response.json");
    stubGet(INNREACH_LOCALSERVERS_URL, "agency-codes/d2ir-local-servers-response-01.json");
    stubPost(HOLDINGS_URL, "inventory-storage/holding-response.json");
    stubPost(ITEMS_URL, "inventory-storage/item-response.json");
    stubGet(format(QUERY_INVENTORY_ITEM_BY_HRID_URL_TEMPLATE, "ittracking15east"), "inventory/query-items-response.json");
    stubGet(format(QUERY_REQUEST_BY_ITEM_ID_URL_TEMPLATE, PRE_POPULATED_ITEM_ID), "request-storage/empty-requests-response.json");
    stubPost(format(MOVE_CIRCULATION_REQUEST_URL_TEMPLATE, PRE_POPULATED_REQUEST_ID), "request-storage/updated-request-response.json");

    mockMvc.perform(post(CIRCULATION_ENDPOINT, PATRON_HOLD.getOperationName(), PRE_POPULATED_TRACKING_ID, PRE_POPULATED_CENTRAL_CODE)
        .content(jsonHelper.toJson(transactionHoldDTO))
        .contentType(MediaType.APPLICATION_JSON)
        .headers(getOkapiHeaders()))
      .andExpect(status().isOk());

    await().atMost(Duration.ofSeconds(10)).untilAsserted(() ->
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
    transactionHoldDTO.setPatronId(toStringWithoutHyphens(UUID.randomUUID()));
    var pickupLocation = pickupLocationMapper.fromString(transactionHoldDTO.getPickupLocation());

    stubGet(format(QUERY_INSTANCE_TYPE_BY_NAME_URL_TEMPLATE, INSTANCE_TYPE_NAME_URLENCODED), "inventory-storage/query-instance-types-response.json");
    stubGet(format(QUERY_SERVICE_POINTS_BY_CODE_ULR_TEMPLATE, pickupLocation.getPickupLocCode()), "inventory-storage/query-service-points-response.json");
    stubGet(format(QUERY_CONTRIBUTOR_TYPE_BY_NAME_URL_TEMPLATE, INSTANCE_CONTRIBUTOR_NAME_URLENCODED), "inventory-storage/query-contributor-name-types-response.json");
    stubGet(HRID_SETTINGS_URL, "inventory-storage/hrid-settings-response.json");
    stubGet(format(QUERY_INSTANCE_BY_HRID_URL_TEMPLATE, "innewtrackingidd2ir"), "inventory-storage/query-instance-response.json");
    stubGet(INNREACH_LOCALSERVERS_URL, "agency-codes/d2ir-local-servers-response-01.json");
    stubPost(INSTANCES_URL, "inventory-storage/instance-response.json");
    stubPost(HOLDINGS_URL, "inventory-storage/holding-response.json");
    stubPost(ITEMS_URL, "inventory-storage/item-response.json");
    stubPost(REQUESTS_URL, "request-storage/item-request-response.json");
    stubGet(format(QUERY_INVENTORY_ITEM_BY_HRID_URL_TEMPLATE, ITEM_HRID), "inventory/query-items-response.json");
    stubGet(format(QUERY_REQUEST_BY_ITEM_ID_URL_TEMPLATE, PRE_POPULATED_ITEM_ID), "request-storage/empty-requests-response.json");

    mockMvc.perform(post(CIRCULATION_ENDPOINT, PATRON_HOLD.getOperationName(), "newtrackingid", PRE_POPULATED_CENTRAL_CODE)
        .content(jsonHelper.toJson(transactionHoldDTO))
        .contentType(MediaType.APPLICATION_JSON)
        .headers(getOkapiHeaders()))
      .andExpect(status().isOk());

    await().atMost(Duration.ofSeconds(10)).untilAsserted(() ->
      verify(requestStorageClient).sendRequest(any()));
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

    stubGet(format("%s/%s", ITEMS_URL, PRE_POPULATED_ITEM_ID), "inventory-storage/item-response.json");
    stubPut(format("%s/%s", ITEMS_URL, PRE_POPULATED_ITEM_ID), "inventory-storage/item-response.json");
    stubGet(format("%s/%s", HOLDINGS_URL, HOLDING_ID), "inventory-storage/holding-response.json");
    stubPut(format("%s/%s", HOLDINGS_URL, HOLDING_ID), "inventory-storage/holding-response.json");
    stubGet(format("%s/%s", REQUESTS_URL, PRE_POPULATED_REQUEST_ID), "request-storage/item-request-response.json");
    stubPut(format("%s/%s", REQUESTS_URL, PRE_POPULATED_REQUEST_ID), "request-storage/item-request-response.json");

    mockMvc.perform(put(CIRCULATION_ENDPOINT, CANCEL_PATRON_HOLD.getOperationName(), PRE_POPULATED_TRACKING_ID, PRE_POPULATED_CENTRAL_CODE)
        .content(jsonHelper.toJson(requestPayload))
        .contentType(MediaType.APPLICATION_JSON)
        .headers(getOkapiHeaders()))
      .andExpect(status().isOk());

    verify(requestService).cancelRequest(any(), eq("Test reason"));
    verify(requestStorageClient).updateRequest(any(), any());
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

    stubGet(format("%s/%s", ITEMS_URL, PRE_POPULATED_ITEM_ID), "inventory-storage/item-response.json");
    stubPut(format("%s/%s", ITEMS_URL, PRE_POPULATED_ITEM_ID), "inventory-storage/item-response.json");
    stubGet(format("%s/%s", HOLDINGS_URL, HOLDING_ID), "inventory-storage/holding-response.json");
    stubPut(format("%s/%s", HOLDINGS_URL, HOLDING_ID), "inventory-storage/holding-response.json");
    stubGet(format("%s/%s", REQUESTS_URL, PRE_POPULATED_REQUEST_ID), "request-storage/item-request-response.json");
    stubPut(format("%s/%s", REQUESTS_URL, PRE_POPULATED_REQUEST_ID), "request-storage/item-request-response.json");

    mockMvc.perform(put(CIRCULATION_ENDPOINT, CANCEL_PATRON_HOLD.getOperationName(), PRE_POPULATED_TRACKING_ID, PRE_POPULATED_CENTRAL_CODE)
        .content(jsonHelper.toJson(requestPayload))
        .contentType(MediaType.APPLICATION_JSON)
        .headers(getOkapiHeaders()))
      .andExpect(status().isBadRequest());

    verifyNoInteractions(requestService);
  }

}
