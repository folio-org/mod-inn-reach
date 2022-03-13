package org.folio.innreach.controller.d2ir;

import static java.lang.String.format;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.AFTER_TEST_METHOD;
import static org.springframework.test.context.jdbc.SqlMergeMode.MergeMode.MERGE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import static org.folio.innreach.controller.d2ir.CirculationResultUtils.emptyErrors;
import static org.folio.innreach.controller.d2ir.CirculationResultUtils.exceptionMatch;
import static org.folio.innreach.controller.d2ir.CirculationResultUtils.failedWithReason;
import static org.folio.innreach.controller.d2ir.CirculationResultUtils.logResponse;
import static org.folio.innreach.domain.entity.InnReachTransaction.TransactionState.TRANSFER;
import static org.folio.innreach.fixture.CirculationFixture.createTransferRequestDTO;
import static org.folio.innreach.fixture.TestUtil.randomAlphanumeric32Max;
import static org.folio.innreach.fixture.TestUtil.randomAlphanumeric5;

import java.util.stream.Stream;

import org.folio.innreach.util.JsonHelper;
import org.folio.innreach.util.UUIDEncoder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlMergeMode;

import org.folio.innreach.controller.base.BaseApiControllerTest;
import org.folio.innreach.domain.entity.InnReachTransaction;
import org.folio.innreach.domain.exception.EntityNotFoundException;
import org.folio.innreach.dto.TransferRequestDTO;
import org.folio.innreach.repository.InnReachTransactionRepository;

@Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/inn-reach-transaction/pre-populate-inn-reach-transaction.sql"
})
@Sql(scripts = {
        "classpath:db/central-server/clear-central-server-tables.sql",
        "classpath:db/inn-reach-transaction/clear-inn-reach-transaction-tables.sql"},
    executionPhase = AFTER_TEST_METHOD
)
@SqlMergeMode(MERGE)
class TransferRequestCirculationApiTest extends BaseApiControllerTest {

  private static final String PRE_POPULATED_TRACKING_ID = "tracking1";
  private static final String PRE_POPULATED_CENTRAL_CODE = "d2ir";
  private static final String PRE_POPULATED_ITEM_ID = "item1";
  private static final String PRE_POPULATED_ITEM_AGENCY_CODE = "asd34";
  private static final String NEW_ITEM_ID = "newitem";
  private static final String PRE_POPULATED_PATRON_ID = "ifkkmbcnljgy5elaav74pnxgxa";

  private static final String TRANSFERREQ_URL = "/inn-reach/d2ir/circ/transferrequest/{trackingId}/{centralCode}";
  private static final String USER_BY_ID_URL_TEMPLATE = "/users/%s";
  private static final String CIRCULATION_ENDPOINT = "/inn-reach/d2ir/circ/{circulationOperationName}/{trackingId}/{centralCode}";
  private static final String TRANSFER_REQUEST = "transferrequest";

  @Autowired
  private InnReachTransactionRepository repository;
  @Autowired
  private JsonHelper jsonHelper;

  @Test
  @Sql(scripts = {
    "classpath:db/agency-loc-mapping/pre-populate-agency-location-mapping.sql",
    "classpath:db/item-type-mapping/pre-populate-item-type-mapping.sql",
    "classpath:db/patron-type-mapping/pre-populate-patron-type-mapping-circulation.sql"
  })
  void updateTransactionItemId_with_newItemFromRequest() throws Exception {
    var req = createTransferRequest();
    req.setNewItemId(NEW_ITEM_ID);
    req.setPatronId(PRE_POPULATED_PATRON_ID);
    var patronId = UUIDEncoder.decode(req.getPatronId());

    stubGet(format(USER_BY_ID_URL_TEMPLATE, patronId), "users/user.json");

    mockMvc.perform(put(CIRCULATION_ENDPOINT, TRANSFER_REQUEST, PRE_POPULATED_TRACKING_ID, PRE_POPULATED_CENTRAL_CODE)
      .content(jsonHelper.toJson(req))
      .contentType(MediaType.APPLICATION_JSON)
      .headers(getOkapiHeaders()))
      .andExpect(status().isOk());

    var trx = getTransaction(PRE_POPULATED_TRACKING_ID, PRE_POPULATED_CENTRAL_CODE);

    assertEquals(NEW_ITEM_ID, trx.getHold().getItemId());
    assertEquals(TRANSFER, trx.getState());
  }

  @ParameterizedTest
  @MethodSource("transactionNotFoundArgProvider")
  void return400_when_TransactionNotFound(String trackingId, String centralCode, TransferRequestDTO req)
      throws Exception {
    var patronId = UUIDEncoder.decode(req.getPatronId());
    stubGet(format(USER_BY_ID_URL_TEMPLATE, patronId), "users/user.json");
    putReq(transferReqUri(trackingId, centralCode), req)
        .andDo(logResponse())
        .andExpect(status().isBadRequest())
        .andExpect(failedWithReason(containsString(trackingId), containsString(centralCode)))
        .andExpect(emptyErrors())
        .andExpect(exceptionMatch(EntityNotFoundException.class));
  }

  @Test
  @Sql(scripts = {
    "classpath:db/agency-loc-mapping/pre-populate-agency-location-mapping.sql",
    "classpath:db/item-type-mapping/pre-populate-item-type-mapping.sql",
    "classpath:db/patron-type-mapping/pre-populate-patron-type-mapping-circulation.sql"
  })
  void return400_when_ItemIdDoesntMatch() throws Exception {
    var req = createTransferRequest();
    req.setItemId(randomAlphanumeric32Max());
    req.setPatronId(PRE_POPULATED_PATRON_ID);
    var patronId = UUIDEncoder.decode(req.getPatronId());
    stubGet(format(USER_BY_ID_URL_TEMPLATE, patronId), "users/user.json");

    putReq(transferReqUri(), req)
        .andDo(logResponse())
        .andExpect(status().isBadRequest())
        .andExpect(failedWithReason(containsString(req.getItemId()), containsString(PRE_POPULATED_ITEM_ID)))
        .andExpect(emptyErrors())
        .andExpect(exceptionMatch(IllegalArgumentException.class));
  }

  @Test
  @Sql(scripts = {
    "classpath:db/agency-loc-mapping/pre-populate-agency-location-mapping.sql",
    "classpath:db/item-type-mapping/pre-populate-item-type-mapping.sql",
    "classpath:db/patron-type-mapping/pre-populate-patron-type-mapping-circulation.sql"
  })
  void return400_when_ItemAgencyCodeDoesntMatch() throws Exception {
    var req = createTransferRequest();
    req.setItemAgencyCode(randomAlphanumeric5());
    req.setNewItemId(NEW_ITEM_ID);
    req.setPatronId(PRE_POPULATED_PATRON_ID);
    var patronId = UUIDEncoder.decode(req.getPatronId());
    stubGet(format(USER_BY_ID_URL_TEMPLATE, patronId), "users/user.json");

    putReq(transferReqUri(), req)
        .andDo(logResponse())
        .andExpect(status().isBadRequest())
        .andExpect(failedWithReason(containsString(req.getItemAgencyCode()), containsString(PRE_POPULATED_ITEM_AGENCY_CODE)))
        .andExpect(emptyErrors())
        .andExpect(exceptionMatch(IllegalArgumentException.class));
  }

  private static TransferRequestDTO createTransferRequest() {
    var req = createTransferRequestDTO();

    req.setItemId(PRE_POPULATED_ITEM_ID);
    req.setItemAgencyCode(PRE_POPULATED_ITEM_AGENCY_CODE);

    return req;
  }

  static Stream<Arguments> transactionNotFoundArgProvider() {
    return Stream.of(
        arguments(PRE_POPULATED_TRACKING_ID, randomAlphanumeric5(), createTransferRequestDTO()),
        arguments(randomAlphanumeric32Max(), PRE_POPULATED_CENTRAL_CODE, createTransferRequestDTO())
    );
  }

  private static URI transferReqUri() {
    return transferReqUri(PRE_POPULATED_TRACKING_ID, PRE_POPULATED_CENTRAL_CODE);
  }

  private static URI transferReqUri(String trackingId, String centralCode) {
    return URI.of(TRANSFERREQ_URL, trackingId, centralCode);
  }

  private InnReachTransaction getTransaction(String trackingId, String centralCode) {
    return repository.findByTrackingIdAndCentralServerCode(trackingId, centralCode).orElseThrow();
  }

  private void putAndExpectOk(URI uri, Object requestBody) throws Exception {
    putAndExpect(uri, requestBody, Template.of("circulation/ok-response.json"));
  }

}
