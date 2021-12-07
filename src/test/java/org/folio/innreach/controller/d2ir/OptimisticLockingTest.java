package org.folio.innreach.controller.d2ir;

import static com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED;
import static java.lang.String.format;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.AFTER_TEST_METHOD;
import static org.springframework.test.context.jdbc.SqlMergeMode.MergeMode.MERGE;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import static org.folio.innreach.controller.d2ir.CirculationResultUtils.emptyErrors;
import static org.folio.innreach.controller.d2ir.CirculationResultUtils.exceptionMatch;
import static org.folio.innreach.controller.d2ir.CirculationResultUtils.failedWithReason;
import static org.folio.innreach.controller.d2ir.CirculationResultUtils.logResponse;
import static org.folio.innreach.domain.entity.InnReachTransaction.TransactionState.ITEM_SHIPPED;
import static org.folio.innreach.domain.entity.InnReachTransaction.TransactionState.PATRON_HOLD;
import static org.folio.innreach.fixture.CirculationFixture.createItemShippedDTO;

import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlMergeMode;

import org.folio.innreach.controller.base.BaseApiControllerTest;
import org.folio.innreach.domain.entity.InnReachTransaction;
import org.folio.innreach.domain.entity.InnReachTransaction.TransactionState;
import org.folio.innreach.domain.exception.ResourceVersionConflictException;
import org.folio.innreach.dto.ItemShippedDTO;
import org.folio.innreach.repository.InnReachTransactionRepository;

@Sql(
    scripts = {
        "classpath:db/central-server/clear-central-server-tables.sql",
        "classpath:db/inn-reach-transaction/clear-inn-reach-transaction-tables.sql"
    },
    executionPhase = AFTER_TEST_METHOD
)
@SqlMergeMode(MERGE)
class OptimisticLockingTest extends BaseApiControllerTest {

  private static final String CIRCULATION_ENDPOINT =
      "/inn-reach/d2ir/circ/{circulationOperationName}/{trackingId}/{centralCode}";

  private static final String ITEM_SHIPPED_OPERATION = "itemshipped";
  private static final String PRE_POPULATED_TRACKING_ID = "tracking1";
  private static final String PRE_POPULATED_CENTRAL_CODE = "d2ir";
  private static final String PRE_POPULATED_ITEM_ID = "9a326225-6530-41cc-9399-a61987bfab3c";
  private static final TransactionState PRE_POPULATED_STATE = PATRON_HOLD;
  private static final String RETRY_SCENARIO = "Retry Scenario";
  private static final String CONFLICT_STATE = "Conflict";

  @Autowired
  private InnReachTransactionRepository repository;


  @Test
  @Sql(scripts = {
      "classpath:db/central-server/pre-populate-central-server.sql",
      "classpath:db/inn-reach-transaction/pre-populate-inn-reach-transaction.sql"
  })
  void recover_from_ItemVersionConflict_when_itemShipped() throws Exception {
    var req = createItemShippedDTO();
    
    stubGet(format("/inventory/items?query=barcode==%s", req.getItemBarcode()), "inventory/query-items-response.json");

    stubGet(itemUrl(), "inventory/item-response.json",
        ResponseActions.none(),
        mapping -> mapping.inScenario(RETRY_SCENARIO)
            .whenScenarioStateIs(STARTED));
    stubPut(itemUrl(),
        conflictResponse(),
        mapping -> mapping.inScenario(RETRY_SCENARIO)
            .whenScenarioStateIs(STARTED)
            .willSetStateTo(CONFLICT_STATE)
    );

    stubGet(itemUrl(), "inventory/item-response.json",
        ResponseActions.none(),
        mapping -> mapping.inScenario(RETRY_SCENARIO)
            .whenScenarioStateIs(CONFLICT_STATE));
    stubPut(itemUrl(),
        ResponseActions.none(),
        mapping -> mapping
            .inScenario(RETRY_SCENARIO)
            .whenScenarioStateIs(CONFLICT_STATE)
    );

    putAndExpectOk(itemShippedReqUri(), req);

    assertTrxState(ITEM_SHIPPED);
  }

  @Test
  @Sql(scripts = {
      "classpath:db/central-server/pre-populate-central-server.sql",
      "classpath:db/inn-reach-transaction/pre-populate-inn-reach-transaction.sql"
  })
  void fail_from_ItemVersionConflict_when_itemShipped_if_RetriesExhausted() throws Exception {
    var req = createItemShippedDTO();

    stubGet(format("/inventory/items?query=barcode==%s", req.getItemBarcode()), "inventory/query-items-response.json");

    stubGet(itemUrl(), "inventory/item-response.json");
    stubPut(itemUrl(), conflictResponse(), MappingActions.none());

    putAndExpectConflict(itemShippedReqUri(), req);

    assertTrxState(PRE_POPULATED_STATE);
  }

  private BaseApiControllerTest.ResponseActions conflictResponse() {
    return response -> response.withStatus(HttpStatus.SC_CONFLICT)
        .withBodyFile("inventory/version-conflict.json");
  }

  private void assertTrxState(TransactionState state) {
    var trx = getTransaction(PRE_POPULATED_TRACKING_ID, PRE_POPULATED_CENTRAL_CODE);
    assertEquals(state, trx.getState());
  }

  private static String itemUrl() {
    return format("/inventory/items/%s", PRE_POPULATED_ITEM_ID);
  }

  private static URI itemShippedReqUri() {
    return circulationReqUri(ITEM_SHIPPED_OPERATION, PRE_POPULATED_TRACKING_ID, PRE_POPULATED_CENTRAL_CODE);
  }

  private static URI circulationReqUri(String operation, String trackingId, String centralCode) {
    return URI.of(CIRCULATION_ENDPOINT, operation, trackingId, centralCode);
  }

  private InnReachTransaction getTransaction(String trackingId, String centralCode) {
    return repository.findByTrackingIdAndCentralServerCode(trackingId, centralCode).orElseThrow();
  }

  private void putAndExpectOk(URI uri, Object requestBody) throws Exception {
    putAndExpect(uri, requestBody, Template.of("circulation/ok-response.json"));
  }

  private void putAndExpectConflict(URI uri, ItemShippedDTO req) throws Exception {
    putReq(uri, req)
        .andDo(logResponse())
        .andExpect(status().isInternalServerError())
        .andExpect(failedWithReason(containsString("optimistic locking")))
        .andExpect(emptyErrors())
        .andExpect(exceptionMatch(ResourceVersionConflictException.class));
  }
  
}
