package org.folio.innreach.controller.d2ir;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.AFTER_TEST_METHOD;
import static org.springframework.test.context.jdbc.SqlMergeMode.MergeMode.MERGE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import static org.folio.innreach.domain.CirculationOperation.PATRON_HOLD;
import static org.folio.innreach.fixture.CirculationFixture.createTransactionHoldDTO;
import static org.folio.innreach.util.UUIDHelper.toStringWithoutHyphens;

import java.util.Collections;
import java.util.Map;
import java.util.UUID;

import com.github.tomakehurst.wiremock.client.MappingBuilder;
import com.github.tomakehurst.wiremock.client.WireMock;
import org.awaitility.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlMergeMode;

import org.folio.innreach.client.RequestStorageClient;
import org.folio.innreach.controller.base.BaseApiControllerTest;
import org.folio.innreach.domain.InnReachResponseStatus;
import org.folio.innreach.domain.entity.InnReachTransaction;
import org.folio.innreach.dto.InnReachResponseDTO;
import org.folio.innreach.mapper.InnReachTransactionPickupLocationMapper;
import org.folio.innreach.repository.InnReachTransactionRepository;
import org.folio.innreach.util.JsonHelper;
import org.folio.spring.integration.XOkapiHeaders;

@Sql(
  scripts = {
    "classpath:db/central-server/clear-central-server-tables.sql",
    "classpath:db/inn-reach-transaction/clear-inn-reach-transaction-tables.sql"
  },
  executionPhase = AFTER_TEST_METHOD
)
@SqlMergeMode(MERGE)
class InnReachCirculationControllerTest extends BaseApiControllerTest {

  public static final String INNREACH_LOCALSERVERS_URL = "/innreach/v2/contribution/localservers";
  public static final String HRID_SETTINGS_URL = "/hrid-settings-storage/hrid-settings";
  public static final String HOLDINGS_URL = "/holdings-storage/holdings";
  public static final String INSTANCES_URL = "/instance-storage/instances";
  public static final String ITEMS_URL = "/item-storage/items";
  public static final String REQUEST_STORAGE_REQUESTS_URL = "/request-storage/requests";
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
  private static final UUID NEW_REQUEST_ID = UUID.fromString("89105c06-dbdb-4aa0-9695-d4d19c733270");

  private static final String ITEM_HRID = "itnewtrackingid5east";
  private static final String ITEM_ID = "45b706c2-63ff-4a01-b7c2-5483f19b8c8f";

  @Autowired
  private TestRestTemplate testRestTemplate;

  @SpyBean
  private InnReachTransactionRepository repository;

  @SpyBean
  private RequestStorageClient requestStorageClient;

  @Autowired
  private JsonHelper jsonHelper;

  @Autowired
  private InnReachTransactionPickupLocationMapper pickupLocationMapper;

  @Test
  void processCreatePatronHoldCirculationRequest_and_createNewPatronHold() {
    var transactionHoldDTO = createTransactionHoldDTO();

    var responseEntity = testRestTemplate.postForEntity(
      "/inn-reach/d2ir/circ/{circulationOperationName}/{trackingId}/{centralCode}",
      transactionHoldDTO, InnReachResponseDTO.class, PATRON_HOLD.getOperationName(), "tracking99", PRE_POPULATED_CENTRAL_CODE);

    assertEquals(HttpStatus.OK, responseEntity.getStatusCode());

    var responseBody = responseEntity.getBody();

    assertNotNull(responseBody);
    assertNull(responseBody.getErrors());
    assertEquals(InnReachResponseStatus.OK.getResponseStatus(), responseBody.getStatus());

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
      transactionHoldDTO, InnReachResponseDTO.class, PATRON_HOLD.getOperationName(), PRE_POPULATED_TRACKING_ID, PRE_POPULATED_CENTRAL_CODE);

    assertEquals(HttpStatus.OK, responseEntity.getStatusCode());

    var responseBody = responseEntity.getBody();

    assertNotNull(responseBody);
    assertNull(responseBody.getErrors());
    assertEquals(InnReachResponseStatus.OK.getResponseStatus(), responseBody.getStatus());

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
  void returnErrorMessage_whenCirculationOperationIsNotSupported() {
    var circulationRequestDTO = createTransactionHoldDTO();

    var responseEntity = testRestTemplate.postForEntity(
      "/inn-reach/d2ir/circ/{circulationOperationName}/{trackingId}/{centralCode}",
      circulationRequestDTO, InnReachResponseDTO.class, "notExistingOperation", PRE_POPULATED_TRACKING_ID, PRE_POPULATED_CENTRAL_CODE);

    assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
  }

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
    stubGet(QUERY_INSTANCE_BY_HRID_URL_TEMPLATE, "inventory-storage/query-instance-response.json", "intracking1d2ir");
    stubGet(INNREACH_LOCALSERVERS_URL, "agency-codes/d2ir-local-servers-response-01.json");
    stubPost(HOLDINGS_URL, "inventory-storage/holding-response.json");
    stubPost(ITEMS_URL, "inventory-storage/item-response.json");
    stubGet(QUERY_INVENTORY_ITEM_BY_HRID_URL_TEMPLATE, "inventory/query-items-response.json", "ittracking15east");
    stubGet(QUERY_REQUEST_BY_ITEM_ID_URL_TEMPLATE, "request-storage/empty-requests-response.json", ITEM_ID);
    stubPost(MOVE_CIRCULATION_REQUEST_URL_TEMPLATE, "request-storage/item-request-response.json", PRE_POPULATED_REQUEST_ID);

    mockMvc.perform(post(CIRCULATION_ENDPOINT, PATRON_HOLD.getOperationName(), PRE_POPULATED_TRACKING_ID, PRE_POPULATED_CENTRAL_CODE)
        .content(jsonHelper.toJson(transactionHoldDTO))
        .contentType(MediaType.APPLICATION_JSON)
        .headers(getOkapiHeaders()))
      .andExpect(status().isOk());

    await().atMost(Duration.FIVE_MINUTES).untilAsserted(() ->
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

    stubGet(QUERY_INSTANCE_TYPE_BY_NAME_URL_TEMPLATE, "inventory-storage/query-instance-types-response.json", INSTANCE_TYPE_NAME_URLENCODED);
    stubGet(QUERY_SERVICE_POINTS_BY_CODE_ULR_TEMPLATE, "inventory-storage/query-service-points-response.json", pickupLocation.getPickupLocCode());
    stubGet(QUERY_CONTRIBUTOR_TYPE_BY_NAME_URL_TEMPLATE, "inventory-storage/query-contributor-name-types-response.json", INSTANCE_CONTRIBUTOR_NAME_URLENCODED);
    stubGet(HRID_SETTINGS_URL, "inventory-storage/hrid-settings-response.json");
    stubGet(QUERY_INSTANCE_BY_HRID_URL_TEMPLATE, "inventory-storage/query-instance-response.json", "innewtrackingidd2ir");
    stubGet(INNREACH_LOCALSERVERS_URL, "agency-codes/d2ir-local-servers-response-01.json");
    stubPost(INSTANCES_URL, "inventory-storage/instance-response.json");
    stubPost(HOLDINGS_URL, "inventory-storage/holding-response.json");
    stubPost(ITEMS_URL, "inventory-storage/item-response.json");
    stubPost(REQUEST_STORAGE_REQUESTS_URL, "request-storage/item-request-response.json");
    stubGet(QUERY_INVENTORY_ITEM_BY_HRID_URL_TEMPLATE, "inventory/query-items-response.json", ITEM_HRID);
    stubGet(QUERY_REQUEST_BY_ITEM_ID_URL_TEMPLATE, "request-storage/empty-requests-response.json", ITEM_ID);

    mockMvc.perform(post(CIRCULATION_ENDPOINT, PATRON_HOLD.getOperationName(), "newtrackingid", PRE_POPULATED_CENTRAL_CODE)
        .content(jsonHelper.toJson(transactionHoldDTO))
        .contentType(MediaType.APPLICATION_JSON)
        .headers(getOkapiHeaders()))
      .andExpect(status().isOk());

    await().atMost(Duration.TEN_SECONDS).untilAsserted(() ->
      verify(requestStorageClient).sendRequest(any()));
  }

  protected static void stubGet(String urlTemplate, String responsePath, Object... pathVariables) {
    stubGet(String.format(urlTemplate, pathVariables), Collections.emptyMap(), responsePath);
  }

  protected static void stubGet(String url, Map<String, String> requestHeaders, String responsePath) {
    MappingBuilder getBuilder = WireMock.get(urlEqualTo(url));

    requestHeaders.forEach((name, value) -> getBuilder.withHeader(name, equalTo(value)));

    stubFor(getBuilder
      .willReturn(aResponse()
        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
        .withHeader(XOkapiHeaders.URL, wm.baseUrl())
        .withBodyFile(responsePath)));
  }

  protected static void stubPost(String urlTemplate, String responsePath, Object... pathVariables) {
    var url = String.format(urlTemplate, pathVariables);

    MappingBuilder builder = WireMock.post(urlEqualTo(url));

    stubFor(builder
      .willReturn(aResponse()
        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
        .withHeader(XOkapiHeaders.URL, wm.baseUrl())
        .withBodyFile(responsePath)));
  }

  public HttpHeaders getOkapiHeaders() {
    HttpHeaders headers = new HttpHeaders();
    headers.add(XOkapiHeaders.URL, wm.baseUrl());
    return headers;
  }

}
