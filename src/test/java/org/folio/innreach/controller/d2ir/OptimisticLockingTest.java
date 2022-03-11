package org.folio.innreach.controller.d2ir;

import static com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED;
import static java.lang.String.format;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.AFTER_TEST_METHOD;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import static org.folio.innreach.controller.d2ir.CirculationResultUtils.emptyErrors;
import static org.folio.innreach.controller.d2ir.CirculationResultUtils.exceptionMatch;
import static org.folio.innreach.controller.d2ir.CirculationResultUtils.failedWithReason;
import static org.folio.innreach.controller.d2ir.CirculationResultUtils.logResponse;
import static org.folio.innreach.domain.entity.InnReachTransaction.TransactionState.CANCEL_REQUEST;
import static org.folio.innreach.domain.entity.InnReachTransaction.TransactionState.ITEM_SHIPPED;
import static org.folio.innreach.domain.entity.InnReachTransaction.TransactionState.PATRON_HOLD;
import static org.folio.innreach.fixture.CirculationFixture.createCancelRequestDTO;
import static org.folio.innreach.fixture.CirculationFixture.createItemShippedDTO;

import org.apache.http.HttpStatus;
import org.folio.innreach.domain.dto.folio.User;
import org.folio.innreach.domain.service.CentralServerService;
import org.folio.innreach.domain.service.PatronTypeMappingService;
import org.folio.innreach.domain.service.UserService;
import org.folio.innreach.dto.CentralServerDTO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.jdbc.Sql;

import org.folio.innreach.controller.base.BaseApiControllerTest;
import org.folio.innreach.domain.entity.InnReachTransaction;
import org.folio.innreach.domain.entity.InnReachTransaction.TransactionState;
import org.folio.innreach.domain.exception.ResourceVersionConflictException;
import org.folio.innreach.dto.BaseCircRequestDTO;
import org.folio.innreach.repository.InnReachTransactionRepository;

import java.util.Optional;
import java.util.UUID;

@Sql(
    scripts = {
        "classpath:db/central-server/pre-populate-central-server.sql",
        "classpath:db/inn-reach-transaction/pre-populate-inn-reach-transaction.sql"
})
@Sql(
    scripts = {
        "classpath:db/central-server/clear-central-server-tables.sql",
        "classpath:db/inn-reach-transaction/clear-inn-reach-transaction-tables.sql"
    },
    executionPhase = AFTER_TEST_METHOD
)
class OptimisticLockingTest extends BaseApiControllerTest {

  private static final String CIRCULATION_ENDPOINT =
      "/inn-reach/d2ir/circ/{circulationOperationName}/{trackingId}/{centralCode}";

  private static final String ITEM_SHIPPED_OPERATION = "itemshipped";
  private static final String CANCEL_REQ_OPERATION = "cancelrequest";
  private static final String PRE_POPULATED_TRACKING_ID = "tracking1";
  private static final String PRE_POPULATED_CENTRAL_CODE = "d2ir";
  private static final String PRE_POPULATED_ITEM_ID = "9a326225-6530-41cc-9399-a61987bfab3c";
  private static final String PRE_POPULATED_REQUEST_ID = "ea11eba7-3c0f-4d15-9cca-c8608cd6bc8a";
  private static final String PRE_POPULATED_HOLDING_ID = "16f40c4e-235d-4912-a683-2ad919cc8b07";
  private static final TransactionState PRE_POPULATED_STATE = PATRON_HOLD;
  private static final String ITEM_RETRY_SCENARIO = "Item Retry Scenario";
  private static final String ITEM_CONFLICT_STATE = "Item Conflict";
  private static final String HOLDINGS_RETRY_SCENARIO = "Holdings Retry Scenario";
  private static final String HOLDINGS_CONFLICT_STATE = "Holdings Conflict";
  private static final UUID PRE_POPULATED_PATRON_ID = UUID.fromString("4154a604-4d5a-4d8e-9160-057fc7b6e6b8");
  private static final UUID PRE_POPULATE_PATRON_GROUP_ID = UUID.fromString("8534295a-e031-4738-a952-f7db900df8c0");
  private static final Integer PRE_POPULATED_CENTRAL_PATRON_TYPE = 122;

  @Autowired
  private InnReachTransactionRepository repository;
  @MockBean
  private UserService userService;
  @MockBean
  private PatronTypeMappingService patronTypeMappingService;
  @MockBean
  private CentralServerService centralServerService;


  @Test
  void recover_from_ItemVersionConflict_when_itemShipped() throws Exception {
    var req = createItemShippedDTO();
    var user = populateUser();
    var centralServerDTO = new CentralServerDTO();

    when(userService.getUserById(PRE_POPULATED_PATRON_ID)).thenReturn(Optional.of(user));
    when(centralServerService.getCentralServerByCentralCode(PRE_POPULATED_CENTRAL_CODE)).thenReturn(centralServerDTO);
    when(patronTypeMappingService.getCentralPatronType(centralServerDTO.getId(), user.getPatronGroupId()))
      .thenReturn(Optional.of(PRE_POPULATED_CENTRAL_PATRON_TYPE));

    stubGet(format("/inventory/items?query=barcode==%s", req.getItemBarcode()), "inventory/query-items-response.json");

    stubItemRecoverableScenario();

    putAndExpectOk(itemShippedReqUri(), req);

    assertTrxState(ITEM_SHIPPED);
  }

  @Test
  void fail_from_ItemVersionConflict_when_itemShipped_if_RetriesExhausted() throws Exception {
    var req = createItemShippedDTO();
    var user = populateUser();
    var centralServerDTO = new CentralServerDTO();

    when(userService.getUserById(PRE_POPULATED_PATRON_ID)).thenReturn(Optional.of(user));
    when(centralServerService.getCentralServerByCentralCode(PRE_POPULATED_CENTRAL_CODE)).thenReturn(centralServerDTO);
    when(patronTypeMappingService.getCentralPatronType(centralServerDTO.getId(), user.getPatronGroupId()))
      .thenReturn(Optional.of(PRE_POPULATED_CENTRAL_PATRON_TYPE));

    stubGet(format("/inventory/items?query=barcode==%s", req.getItemBarcode()), "inventory/query-items-response.json");

    stubItemUnrecoverableScenarion();

    putAndExpectConflict(itemShippedReqUri(), req);

    assertTrxState(PRE_POPULATED_STATE);
  }

  @Test
  void recover_from_ItemVersionConflict_when_cancelingTransaction() throws Exception {
    var req = createCancelRequestDTO();
    var user = populateUser();
    var centralServerDTO = new CentralServerDTO();

    when(userService.getUserById(PRE_POPULATED_PATRON_ID)).thenReturn(Optional.of(user));
    when(centralServerService.getCentralServerByCentralCode(PRE_POPULATED_CENTRAL_CODE)).thenReturn(centralServerDTO);
    when(patronTypeMappingService.getCentralPatronType(centralServerDTO.getId(), user.getPatronGroupId()))
      .thenReturn(Optional.of(PRE_POPULATED_CENTRAL_PATRON_TYPE));

    stubGet(circRequestUrl(), "circulation/item-request-response.json");
    stubPut(circRequestUrl());

    stubGet(holdingsUrl(), "inventory-storage/holding-response.json");
    stubPut(holdingsUrl());

    stubItemRecoverableScenario();

    putAndExpectOk(cancelReqUri(), req);

    assertTrxState(CANCEL_REQUEST);
  }

  @Test
  void fail_from_ItemVersionConflict_when_cancelingTransaction_if_RetriesExhausted() throws Exception {
    var req = createCancelRequestDTO();
    var user = populateUser();
    var centralServerDTO = new CentralServerDTO();

    when(userService.getUserById(PRE_POPULATED_PATRON_ID)).thenReturn(Optional.of(user));
    when(centralServerService.getCentralServerByCentralCode(PRE_POPULATED_CENTRAL_CODE)).thenReturn(centralServerDTO);
    when(patronTypeMappingService.getCentralPatronType(centralServerDTO.getId(), user.getPatronGroupId()))
      .thenReturn(Optional.of(PRE_POPULATED_CENTRAL_PATRON_TYPE));

    stubGet(circRequestUrl(), "circulation/item-request-response.json");
    stubPut(circRequestUrl());

    stubGet(holdingsUrl(), "inventory-storage/holding-response.json");
    stubPut(holdingsUrl());

    stubItemUnrecoverableScenarion();

    putAndExpectConflict(cancelReqUri(), req);

    assertTrxState(PRE_POPULATED_STATE);
  }

  @Test
  void recover_from_HoldingsVersionConflict_when_cancelingTransaction() throws Exception {
    var req = createCancelRequestDTO();
    var user = populateUser();
    var centralServerDTO = new CentralServerDTO();

    when(userService.getUserById(PRE_POPULATED_PATRON_ID)).thenReturn(Optional.of(user));
    when(centralServerService.getCentralServerByCentralCode(PRE_POPULATED_CENTRAL_CODE)).thenReturn(centralServerDTO);
    when(patronTypeMappingService.getCentralPatronType(centralServerDTO.getId(), user.getPatronGroupId()))
      .thenReturn(Optional.of(PRE_POPULATED_CENTRAL_PATRON_TYPE));

    stubGet(circRequestUrl(), "circulation/item-request-response.json");
    stubPut(circRequestUrl());

    stubGet(itemUrl(), "inventory/item-response.json");
    stubPut(itemUrl());

    stubHoldingsRecoverableScenario();

    putAndExpectOk(cancelReqUri(), req);

    assertTrxState(CANCEL_REQUEST);
  }

  @Test
  void fail_from_HoldingsVersionConflict_when_cancelingTransaction_if_RetriesExhausted() throws Exception {
    var req = createCancelRequestDTO();
    var user = populateUser();
    var centralServerDTO = new CentralServerDTO();

    when(userService.getUserById(PRE_POPULATED_PATRON_ID)).thenReturn(Optional.of(user));
    when(centralServerService.getCentralServerByCentralCode(PRE_POPULATED_CENTRAL_CODE)).thenReturn(centralServerDTO);
    when(patronTypeMappingService.getCentralPatronType(centralServerDTO.getId(), user.getPatronGroupId()))
      .thenReturn(Optional.of(PRE_POPULATED_CENTRAL_PATRON_TYPE));

    stubGet(circRequestUrl(), "circulation/item-request-response.json");
    stubPut(circRequestUrl());

    stubGet(itemUrl(), "inventory/item-response.json");
    stubPut(itemUrl());

    stubHoldingsUnRecoverableScenario();

    putAndExpectConflict(cancelReqUri(), req);

    assertTrxState(PRE_POPULATED_STATE);
  }

  @Test
  void recover_from_ItemAndHoldingsVersionConflict_when_cancelingTransaction() throws Exception {
    var req = createCancelRequestDTO();
    var user = populateUser();
    var centralServerDTO = new CentralServerDTO();

    when(userService.getUserById(PRE_POPULATED_PATRON_ID)).thenReturn(Optional.of(user));
    when(centralServerService.getCentralServerByCentralCode(PRE_POPULATED_CENTRAL_CODE)).thenReturn(centralServerDTO);
    when(patronTypeMappingService.getCentralPatronType(centralServerDTO.getId(), user.getPatronGroupId()))
      .thenReturn(Optional.of(PRE_POPULATED_CENTRAL_PATRON_TYPE));

    stubGet(circRequestUrl(), "circulation/item-request-response.json");
    stubPut(circRequestUrl());

    stubItemRecoverableScenario();

    stubHoldingsRecoverableScenario();

    putAndExpectOk(cancelReqUri(), req);

    assertTrxState(CANCEL_REQUEST);
  }

  private void stubItemRecoverableScenario() {
    stubGet(itemUrl(), "inventory/item-response.json",
        ResponseActions.none(),
        mapping -> mapping.inScenario(ITEM_RETRY_SCENARIO)
            .whenScenarioStateIs(STARTED));
    stubPut(itemUrl(),
        conflictResponse(),
        mapping -> mapping.inScenario(ITEM_RETRY_SCENARIO)
            .whenScenarioStateIs(STARTED)
            .willSetStateTo(ITEM_CONFLICT_STATE)
    );

    stubGet(itemUrl(), "inventory/item-response.json",
        ResponseActions.none(),
        mapping -> mapping.inScenario(ITEM_RETRY_SCENARIO)
            .whenScenarioStateIs(ITEM_CONFLICT_STATE));
    stubPut(itemUrl(),
        ResponseActions.none(),
        mapping -> mapping
            .inScenario(ITEM_RETRY_SCENARIO)
            .whenScenarioStateIs(ITEM_CONFLICT_STATE)
    );
  }

  private void stubItemUnrecoverableScenarion() {
    stubGet(itemUrl(), "inventory/item-response.json");
    stubPut(itemUrl(), conflictResponse(), MappingActions.none());
  }

  private void stubHoldingsRecoverableScenario() {
    stubGet(holdingsUrl(), "inventory-storage/holding-response.json",
        ResponseActions.none(),
        mapping -> mapping.inScenario(HOLDINGS_RETRY_SCENARIO)
            .whenScenarioStateIs(STARTED));
    stubPut(holdingsUrl(),
        conflictResponse(),
        mapping -> mapping.inScenario(HOLDINGS_RETRY_SCENARIO)
            .whenScenarioStateIs(STARTED)
            .willSetStateTo(HOLDINGS_CONFLICT_STATE)
    );

    stubGet(holdingsUrl(), "inventory-storage/holding-response.json",
        ResponseActions.none(),
        mapping -> mapping.inScenario(HOLDINGS_RETRY_SCENARIO)
            .whenScenarioStateIs(HOLDINGS_CONFLICT_STATE));
    stubPut(holdingsUrl(),
        ResponseActions.none(),
        mapping -> mapping
            .inScenario(HOLDINGS_RETRY_SCENARIO)
            .whenScenarioStateIs(HOLDINGS_CONFLICT_STATE)
    );
  }

  private void stubHoldingsUnRecoverableScenario() {
    stubGet(holdingsUrl(), "inventory-storage/holding-response.json");
    stubPut(holdingsUrl(), conflictResponse(), MappingActions.none());
  }

  private BaseApiControllerTest.ResponseActions conflictResponse() {
    return response -> response.withStatus(HttpStatus.SC_CONFLICT)
        .withBodyFile("inventory/version-conflict.json");
  }

  private void assertTrxState(TransactionState state) {
    var trx = getTransaction(PRE_POPULATED_TRACKING_ID, PRE_POPULATED_CENTRAL_CODE);
    assertEquals(state, trx.getState());
  }

  private static String circRequestUrl() {
    return format("/circulation/requests/%s", PRE_POPULATED_REQUEST_ID);
  }

  private static String holdingsUrl() {
    return format("/holdings-storage/holdings/%s", PRE_POPULATED_HOLDING_ID);
  }

  private static String itemUrl() {
    return format("/inventory/items/%s", PRE_POPULATED_ITEM_ID);
  }

  private static URI itemShippedReqUri() {
    return circulationReqUri(ITEM_SHIPPED_OPERATION, PRE_POPULATED_TRACKING_ID, PRE_POPULATED_CENTRAL_CODE);
  }

  private static URI cancelReqUri() {
    return circulationReqUri(CANCEL_REQ_OPERATION, PRE_POPULATED_TRACKING_ID, PRE_POPULATED_CENTRAL_CODE);
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

  private void putAndExpectConflict(URI uri, BaseCircRequestDTO req) throws Exception {
    putReq(uri, req)
        .andDo(logResponse())
        .andExpect(status().isInternalServerError())
        .andExpect(failedWithReason(containsString("optimistic locking")))
        .andExpect(emptyErrors())
        .andExpect(exceptionMatch(ResourceVersionConflictException.class));
  }

  private User populateUser() {
    var user = new User();
    user.setId(PRE_POPULATED_PATRON_ID);
    user.setPatronGroupId(PRE_POPULATE_PATRON_GROUP_ID);
    var personal = new User.Personal();
    personal.setPreferredFirstName("Paul");
    personal.setFirstName("MuaDibs");
    personal.setLastName("Atreides");
    user.setPersonal(personal);
    return user;
  }
}
