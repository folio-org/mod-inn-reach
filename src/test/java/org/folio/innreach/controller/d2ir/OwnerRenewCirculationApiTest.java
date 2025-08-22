package org.folio.innreach.controller.d2ir;

import static com.github.tomakehurst.wiremock.client.WireMock.and;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToDateTime;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.putRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static java.lang.String.format;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.AFTER_TEST_METHOD;
import static org.springframework.test.context.jdbc.SqlMergeMode.MergeMode.MERGE;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import static org.folio.innreach.controller.d2ir.CirculationResultUtils.emptyErrors;
import static org.folio.innreach.controller.d2ir.CirculationResultUtils.exceptionMatch;
import static org.folio.innreach.controller.d2ir.CirculationResultUtils.failedWithReason;
import static org.folio.innreach.controller.d2ir.CirculationResultUtils.logResponse;
import static org.folio.innreach.domain.entity.InnReachTransaction.TransactionState.OWNER_RENEW;
import static org.folio.innreach.fixture.CirculationFixture.createRenewLoanDTO;
import static org.folio.innreach.fixture.TestUtil.randomAlphanumeric32Max;
import static org.folio.innreach.fixture.TestUtil.randomAlphanumeric5;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.stream.Stream;

import org.folio.innreach.util.UUIDEncoder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlMergeMode;

import org.folio.innreach.controller.base.BaseApiControllerTest;
import org.folio.innreach.domain.entity.InnReachTransaction;
import org.folio.innreach.domain.exception.EntityNotFoundException;
import org.folio.innreach.dto.RenewLoanDTO;
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
class OwnerRenewCirculationApiTest extends BaseApiControllerTest {

  private static final String PRE_POPULATED_TRACKING_ID = "tracking1";
  private static final String PRE_POPULATED_CENTRAL_CODE = "d2ir";

  private static final String RENEWREQ_URL = "/inn-reach/d2ir/circ/ownerrenew/{trackingId}/{centralCode}";
  private static final String USER_BY_ID_URL_TEMPLATE = "/users/%s";
  private static final int REQ_DUE_DATE_TIME_AFTER = (int) Instant.parse("2021-12-31T00:00:00Z").getEpochSecond();
  private static final int REQ_DUE_DATE_TIME_BEFORE = (int) Instant.parse("2021-12-01T00:00:00Z").getEpochSecond();
  private static final String LOAN_ID = "19bb9798-d396-4b37-8fd6-5df0885e020e";
  private static final String PRE_POPULATED_PATRON_ID = "ifkkmbcnljgy5elaav74pnxgxa";

  @Autowired
  private InnReachTransactionRepository repository;

  @Test
  @Sql(
    scripts = {
      "classpath:db/agency-loc-mapping/pre-populate-agency-location-mapping.sql",
      "classpath:db/item-type-mapping/pre-populate-item-type-mapping.sql",
      "classpath:db/patron-type-mapping/pre-populate-patron-type-mapping-circulation.sql"
    })
  void renewLoan_when_RequestDueDateIsAfterLoanDate() throws Exception {
    var req = createRenewLoanDTO();
    req.setDueDateTime(REQ_DUE_DATE_TIME_AFTER);
    req.setPatronId(PRE_POPULATED_PATRON_ID);
    var patronId = UUIDEncoder.decode(req.getPatronId());

    stubGet(format(USER_BY_ID_URL_TEMPLATE, patronId), "users/user.json");

    stubPost("/circulation/renew-by-id", "circulation/renew-loan-response.json");

    putAndExpectOk(ownerRenewUri(), req);

    var trx = getTransaction(PRE_POPULATED_TRACKING_ID, PRE_POPULATED_CENTRAL_CODE);

    assertEquals(req.getDueDateTime(), trx.getHold().getDueDateTime());
    assertEquals(OWNER_RENEW, trx.getState());
  }

  @Test
  @Sql(
    scripts = {
      "classpath:db/agency-loc-mapping/pre-populate-agency-location-mapping.sql",
      "classpath:db/item-type-mapping/pre-populate-item-type-mapping.sql",
      "classpath:db/patron-type-mapping/pre-populate-patron-type-mapping-circulation.sql"
    })
  void renewLoan_when_RequestDueDateIsBeforeLoanDate() throws Exception {
    var req = createRenewLoanDTO();
    req.setDueDateTime(REQ_DUE_DATE_TIME_BEFORE);
    req.setPatronId(PRE_POPULATED_PATRON_ID);
    var patronId = UUIDEncoder.decode(req.getPatronId());

    stubGet(format(USER_BY_ID_URL_TEMPLATE, patronId), "users/user.json");

    stubPost("/circulation/renew-by-id", "circulation/renew-loan-response.json");
    stubPut(loanUrl());

    putAndExpectOk(ownerRenewUri(), req);

    verify(putRequestedFor(urlEqualTo(loanUrl()))
        .withRequestBody(and(
            matchingJsonPath("$.dueDate", equalToDateTime(toZonedDateTime(REQ_DUE_DATE_TIME_BEFORE))),
            matchingJsonPath("$.action", equalTo("dueDateChanged"))
        )));

    var trx = getTransaction(PRE_POPULATED_TRACKING_ID, PRE_POPULATED_CENTRAL_CODE);

    assertEquals(req.getDueDateTime(), trx.getHold().getDueDateTime());
    assertEquals(OWNER_RENEW, trx.getState());
  }

  @ParameterizedTest
  @MethodSource("transactionNotFoundArgProvider")
  @Sql(
    scripts = {
      "classpath:db/agency-loc-mapping/pre-populate-agency-location-mapping.sql",
      "classpath:db/item-type-mapping/pre-populate-item-type-mapping.sql",
      "classpath:db/patron-type-mapping/pre-populate-patron-type-mapping-circulation.sql"
    })
  void return400_when_TransactionNotFound(String trackingId, String centralCode, RenewLoanDTO req)
      throws Exception {
    req.setPatronId(PRE_POPULATED_PATRON_ID);
    var patronId = UUIDEncoder.decode(req.getPatronId());

    stubGet(format(USER_BY_ID_URL_TEMPLATE, patronId), "users/user.json");
    putReq(ownerRenewUri(trackingId, centralCode), req)
        .andDo(logResponse())
        .andExpect(status().isBadRequest())
        .andExpect(failedWithReason(containsString(trackingId), containsString(centralCode)))
        .andExpect(emptyErrors())
        .andExpect(exceptionMatch(EntityNotFoundException.class));
  }

  static Stream<Arguments> transactionNotFoundArgProvider() {
    return Stream.of(
        arguments(PRE_POPULATED_TRACKING_ID, randomAlphanumeric5(), createRenewLoanDTO()),
        arguments(randomAlphanumeric32Max(), PRE_POPULATED_CENTRAL_CODE, createRenewLoanDTO())
    );
  }
  private static URI ownerRenewUri() {
    return ownerRenewUri(PRE_POPULATED_TRACKING_ID, PRE_POPULATED_CENTRAL_CODE);
  }

  private static URI ownerRenewUri(String trackingId, String centralCode) {
    return URI.of(RENEWREQ_URL, trackingId, centralCode);
  }

  private static String loanUrl() {
    return format("/circulation/loans/%s", LOAN_ID);
  }

  private static ZonedDateTime toZonedDateTime(int epochSeconds) {
    return ZonedDateTime.ofInstant(Instant.ofEpochSecond(epochSeconds), ZoneOffset.UTC);
  }

  private InnReachTransaction getTransaction(String trackingId, String centralCode) {
    return repository.findByTrackingIdAndCentralServerCode(trackingId, centralCode).orElseThrow();
  }

  private void putAndExpectOk(URI uri, Object requestBody) throws Exception {
    putAndExpect(uri, requestBody, Template.of("circulation/ok-response.json"));
  }

}
