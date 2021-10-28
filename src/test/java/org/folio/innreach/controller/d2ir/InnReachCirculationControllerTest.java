package org.folio.innreach.controller.d2ir;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.AFTER_TEST_METHOD;
import static org.springframework.test.context.jdbc.SqlMergeMode.MergeMode.MERGE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import static org.folio.innreach.controller.CentralServerAgencyControllerTest.INNREACH_LOCALSERVERS_URL;
import static org.folio.innreach.domain.CirculationOperation.PATRON_HOLD;
import static org.folio.innreach.fixture.CirculationFixture.createTransactionHoldDTO;

import java.util.UUID;

import org.awaitility.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlMergeMode;

import org.folio.innreach.controller.base.BaseApiControllerTest;
import org.folio.innreach.domain.InnReachResponseStatus;
import org.folio.innreach.domain.service.RequestService;
import org.folio.innreach.dto.InnReachResponseDTO;
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
class InnReachCirculationControllerTest extends BaseApiControllerTest {

  private static final String PRE_POPULATED_TRACKING_ID = "tracking1";
  private static final String PRE_POPULATED_CENTRAL_CODE = "d2ir";
  private static final int PRE_POPULATED_CENTRAL_ITEM_TYPE = 1;
  private static final UUID NEW_REQUEST_ID = UUID.fromString("89105c06-dbdb-4aa0-9695-d4d19c733270");

  @Autowired
  private TestRestTemplate testRestTemplate;

  @Autowired
  private InnReachTransactionRepository repository;

  @Autowired
  private JsonHelper jsonHelper;

  @SpyBean
  private RequestService requestService;

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
    "classpath:db/mtype-mapping/pre-populate-material-type-mapping.sql",
    "classpath:db/inn-reach-transaction/pre-populate-inn-reach-transaction.sql"
  })
  void patronHold_updateVirtualItems() throws Exception {
    var transactionHoldDTO = createTransactionHoldDTO();
    transactionHoldDTO.setItemId("it00000000001");
    transactionHoldDTO.setItemAgencyCode("5east");
    transactionHoldDTO.setCentralItemType(PRE_POPULATED_CENTRAL_ITEM_TYPE);

    stubGet("/hrid-settings-storage/hrid-settings", "inventory-storage/hrid-settings-response.json");
    stubGet("/instance-storage/instances?query=(hrid==intracking1d2ir)", "inventory-storage/query-instance-response.json");
    stubGet(INNREACH_LOCALSERVERS_URL, "agency-codes/d2ir-local-servers-response-01.json");
    stubPost("/holdings-storage/holdings", "inventory-storage/holding-response.json");
    stubPost("/item-storage/items", "inventory-storage/item-response.json");
    stubGet("/inventory/items?query=hrid==it00000000001", "inventory/query-items-response.json");
    stubGet("/request-storage/requests?query=(itemId==45b706c2-63ff-4a01-b7c2-5483f19b8c8f)", "request-storage/empty-requests-response.json");
    stubPost("/circulation/requests/ea11eba7-3c0f-4d15-9cca-c8608cd6bc8a/move", "request-storage/request.json");

    var headers = getOkapiHeaders();

    mockMvc.perform(post("/inn-reach/d2ir/circ/{circulationOperationName}/{trackingId}/{centralCode}", PATRON_HOLD.getOperationName(), PRE_POPULATED_TRACKING_ID, PRE_POPULATED_CENTRAL_CODE)
        .content(jsonHelper.toJson(transactionHoldDTO))
        .contentType(MediaType.APPLICATION_JSON)
        .headers(headers))
      .andExpect(status().isOk());

    await().untilAsserted(() -> NEW_REQUEST_ID.equals(fetchFolioRequestId()));
  }

  private UUID fetchFolioRequestId() {
    return repository.fetchOneByTrackingId(PRE_POPULATED_TRACKING_ID).get().getHold().getFolioRequestId();
  }

}
