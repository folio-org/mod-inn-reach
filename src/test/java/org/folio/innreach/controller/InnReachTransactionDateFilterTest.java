package org.folio.innreach.controller;

import static java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.AFTER_TEST_METHOD;
import static org.springframework.test.context.jdbc.SqlMergeMode.MergeMode.MERGE;

import static org.folio.innreach.dto.FilterDateOperation.between;
import static org.folio.innreach.dto.FilterDateOperation.equal;
import static org.folio.innreach.dto.FilterDateOperation.greater;
import static org.folio.innreach.dto.FilterDateOperation.greaterOrEqual;
import static org.folio.innreach.dto.FilterDateOperation.less;
import static org.folio.innreach.dto.FilterDateOperation.lessOrEqual;
import static org.folio.innreach.dto.FilterDateOperation.notEqual;
import static org.folio.innreach.util.ListUtils.mapItems;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlMergeMode;

import org.folio.innreach.controller.base.BaseControllerTest;
import org.folio.innreach.dto.FilterDateOperation;
import org.folio.innreach.dto.InnReachTransactionDTO;
import org.folio.innreach.dto.InnReachTransactionsDTO;

@Sql(
    scripts = {
        "classpath:db/central-server/pre-populate-central-server.sql",
        "classpath:db/inn-reach-transaction/pre-populate-inn-reach-transaction.sql"
    }
)
@Sql(
    scripts = {
        "classpath:db/inn-reach-transaction/clear-inn-reach-transaction-tables.sql",
        "classpath:db/central-server/clear-central-server-tables.sql"
    },
    executionPhase = AFTER_TEST_METHOD
)
@SqlMergeMode(MERGE)
class InnReachTransactionDateFilterTest extends BaseControllerTest {

  private static final String PRE_POPULATED_TRACKING_ID1 = "tracking1";
  private static final String PRE_POPULATED_TRACKING_ID2 = "tracking2";
  private static final String PRE_POPULATED_TRACKING_ID3 = "tracking3";

  private static final OffsetDateTime TARGET_DATE = OffsetDateTime.parse(
      "2022-02-24T05:00:00+02:00",
      ISO_OFFSET_DATE_TIME);

  @Autowired
  private TestRestTemplate testRestTemplate;


  @ParameterizedTest
  @MethodSource("dateTestData")
  @Sql(scripts = "classpath:db/inn-reach-transaction/set-auditing-dates-for-inn-reach-transactions.sql")
  void returnTransactions_when_filteredByCreatedDate(List<OffsetDateTime> dates, FilterDateOperation operation,
      List<String> expectedTrxTrackingIds) {
    var responseEntity = testRestTemplate.getForEntity(constructUri("createdDate", dates, operation),
        InnReachTransactionsDTO.class);

    assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
    assertNotNull(responseEntity.getBody());
    assertEquals(expectedTrxTrackingIds.size(), responseEntity.getBody().getTotalRecords());

    var transactions = responseEntity.getBody().getTransactions();
    assertEquals(expectedTrxTrackingIds.size(), transactions.size());

    var trackingIds = mapItems(transactions, InnReachTransactionDTO::getTrackingId);
    assertThat(trackingIds, containsInAnyOrder(expectedTrxTrackingIds.toArray()));
  }

  @Test
  @Sql(scripts = "classpath:db/inn-reach-transaction/set-auditing-dates-for-inn-reach-transactions.sql")
  void returnTransaction_when_filteredByCreatedDate_and_otherField() {
    var responseEntity = testRestTemplate.getForEntity(
          constructUri("createdDate", List.of(TARGET_DATE), equal) + "&type=PATRON",
          InnReachTransactionsDTO.class);

    assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
    assertNotNull(responseEntity.getBody());
    assertEquals(1, responseEntity.getBody().getTotalRecords());

    var transactions = responseEntity.getBody().getTransactions();
    assertEquals(1, transactions.size());

    assertEquals(PRE_POPULATED_TRACKING_ID1, transactions.get(0).getTrackingId());
  }

  @ParameterizedTest
  @MethodSource("dateTestData")
  @Sql(scripts = "classpath:db/inn-reach-transaction/set-auditing-dates-for-inn-reach-transactions.sql")
  void returnTransactions_when_filteredByUpdatedDate(List<OffsetDateTime> dates, FilterDateOperation operation,
      List<String> expectedTrxTrackingIds) {
    var responseEntity = testRestTemplate.getForEntity(constructUri("updatedDate", dates, operation),
        InnReachTransactionsDTO.class);

    assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
    assertNotNull(responseEntity.getBody());
    assertEquals(expectedTrxTrackingIds.size(), responseEntity.getBody().getTotalRecords());

    var transactions = responseEntity.getBody().getTransactions();
    assertEquals(expectedTrxTrackingIds.size(), transactions.size());

    var trackingIds = mapItems(transactions, InnReachTransactionDTO::getTrackingId);
    assertThat(trackingIds, containsInAnyOrder(expectedTrxTrackingIds.toArray()));
  }

  @Test
  @Sql(scripts = "classpath:db/inn-reach-transaction/set-auditing-dates-for-inn-reach-transactions.sql")
  void returnTransaction_when_filteredByUpdatedDate_and_otherField() {
    var responseEntity = testRestTemplate.getForEntity(
        constructUri("updatedDate", List.of(TARGET_DATE), equal) + "&state=PATRON_HOLD",
        InnReachTransactionsDTO.class);

    assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
    assertNotNull(responseEntity.getBody());
    assertEquals(1, responseEntity.getBody().getTotalRecords());

    var transactions = responseEntity.getBody().getTransactions();
    assertEquals(1, transactions.size());

    assertEquals(PRE_POPULATED_TRACKING_ID1, transactions.get(0).getTrackingId());
  }

  @ParameterizedTest
  @MethodSource("dateTestData")
  @Sql(scripts = "classpath:db/inn-reach-transaction/set-auditing-dates-for-hold-transactions.sql")
  void returnTransactions_when_filteredByHoldCreatedDate(List<OffsetDateTime> dates, FilterDateOperation operation,
      List<String> expectedTrxTrackingIds) {
    var responseEntity = testRestTemplate.getForEntity(
        constructUri("holdCreatedDate", dates, operation),
        InnReachTransactionsDTO.class);

    assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
    assertNotNull(responseEntity.getBody());
    assertEquals(expectedTrxTrackingIds.size(), responseEntity.getBody().getTotalRecords());

    var transactions = responseEntity.getBody().getTransactions();
    assertEquals(expectedTrxTrackingIds.size(), transactions.size());

    var trackingIds = mapItems(transactions, InnReachTransactionDTO::getTrackingId);
    assertThat(trackingIds, containsInAnyOrder(expectedTrxTrackingIds.toArray()));
  }

  @Test
  @Sql(scripts = "classpath:db/inn-reach-transaction/set-auditing-dates-for-hold-transactions.sql")
  void returnTransaction_when_filteredByHoldCreatedDate_and_otherField() {
    var responseEntity = testRestTemplate.getForEntity(
        constructUri("holdCreatedDate", List.of(TARGET_DATE), equal) + "&patronAgencyCode=qwe12",
        InnReachTransactionsDTO.class);

    assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
    assertNotNull(responseEntity.getBody());
    assertEquals(1, responseEntity.getBody().getTotalRecords());

    var transactions = responseEntity.getBody().getTransactions();
    assertEquals(1, transactions.size());

    assertEquals(PRE_POPULATED_TRACKING_ID1, transactions.get(0).getTrackingId());
  }

  @ParameterizedTest
  @MethodSource("dateTestData")
  @Sql(scripts = "classpath:db/inn-reach-transaction/set-auditing-dates-for-hold-transactions.sql")
  void returnTransactions_when_filteredByHoldUpdatedDate(List<OffsetDateTime> dates, FilterDateOperation operation,
      List<String> expectedTrxTrackingIds) {
    var responseEntity = testRestTemplate.getForEntity(
        constructUri("holdUpdatedDate", dates, operation),
        InnReachTransactionsDTO.class);

    assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
    assertNotNull(responseEntity.getBody());
    assertEquals(expectedTrxTrackingIds.size(), responseEntity.getBody().getTotalRecords());

    var transactions = responseEntity.getBody().getTransactions();
    assertEquals(expectedTrxTrackingIds.size(), transactions.size());

    var trackingIds = mapItems(transactions, InnReachTransactionDTO::getTrackingId);
    assertThat(trackingIds, containsInAnyOrder(expectedTrxTrackingIds.toArray()));
  }

  @Test
  @Sql(scripts = "classpath:db/inn-reach-transaction/set-auditing-dates-for-hold-transactions.sql")
  void returnTransaction_when_filteredByHoldUpdatedDate_and_otherField() {
    var responseEntity = testRestTemplate.getForEntity(
        constructUri("holdUpdatedDate", List.of(TARGET_DATE), equal) + "&patronAgencyCode=qwe12",
        InnReachTransactionsDTO.class);

    assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
    assertNotNull(responseEntity.getBody());
    assertEquals(1, responseEntity.getBody().getTotalRecords());

    var transactions = responseEntity.getBody().getTransactions();
    assertEquals(1, transactions.size());

    assertEquals(PRE_POPULATED_TRACKING_ID1, transactions.get(0).getTrackingId());
  }

  @ParameterizedTest
  @MethodSource("dateTestData")
  @Sql(scripts = "classpath:db/inn-reach-transaction/set-due-dates-for-hold-transactions.sql")
  void returnTransactions_when_filteredByDueDate(List<OffsetDateTime> dates, FilterDateOperation operation,
      List<String> expectedTrxTrackingIds) {
    var responseEntity = testRestTemplate.getForEntity(
        constructUri("dueDate", dates, operation),
        InnReachTransactionsDTO.class);

    assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
    assertNotNull(responseEntity.getBody());
    assertEquals(expectedTrxTrackingIds.size(), responseEntity.getBody().getTotalRecords());

    var transactions = responseEntity.getBody().getTransactions();
    assertEquals(expectedTrxTrackingIds.size(), transactions.size());

    var trackingIds = mapItems(transactions, InnReachTransactionDTO::getTrackingId);
    assertThat(trackingIds, containsInAnyOrder(expectedTrxTrackingIds.toArray()));
  }

  @Test
  @Sql(scripts = "classpath:db/inn-reach-transaction/set-due-dates-for-hold-transactions.sql")
  void returnTransaction_when_filteredByDueDate_and_otherField() {
    var responseEntity = testRestTemplate.getForEntity(
        constructUri("dueDate", List.of(TARGET_DATE), equal) + "&patronAgencyCode=qwe12",
        InnReachTransactionsDTO.class);

    assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
    assertNotNull(responseEntity.getBody());
    assertEquals(1, responseEntity.getBody().getTotalRecords());

    var transactions = responseEntity.getBody().getTransactions();
    assertEquals(1, transactions.size());

    assertEquals(PRE_POPULATED_TRACKING_ID1, transactions.get(0).getTrackingId());
  }

  static Stream<Arguments> dateTestData() {
    return Stream.of(
        arguments(List.of(TARGET_DATE), less, List.of(PRE_POPULATED_TRACKING_ID2)),
        arguments(List.of(TARGET_DATE), lessOrEqual, List.of(PRE_POPULATED_TRACKING_ID1, PRE_POPULATED_TRACKING_ID2)),
        arguments(List.of(TARGET_DATE), equal, List.of(PRE_POPULATED_TRACKING_ID1)),
        arguments(List.of(TARGET_DATE), notEqual, List.of(PRE_POPULATED_TRACKING_ID2, PRE_POPULATED_TRACKING_ID3)),
        arguments(List.of(TARGET_DATE), greater, List.of(PRE_POPULATED_TRACKING_ID3)),
        arguments(List.of(TARGET_DATE), greaterOrEqual, List.of(PRE_POPULATED_TRACKING_ID1, PRE_POPULATED_TRACKING_ID3)),
        arguments(List.of(TARGET_DATE.minusDays(1), TARGET_DATE.plusDays(1)), between, List.of(PRE_POPULATED_TRACKING_ID1)),
        // special case when operation is omitted -- treated as "equal"
        arguments(List.of(TARGET_DATE), null, List.of(PRE_POPULATED_TRACKING_ID1)),
        // special case when dates are not present -- date condition is not constructed, all records returned
        arguments(Collections.emptyList(), less,
            List.of(PRE_POPULATED_TRACKING_ID1, PRE_POPULATED_TRACKING_ID2, PRE_POPULATED_TRACKING_ID3))
    );
  }

  private static String constructUri(String dateParam, List<OffsetDateTime> dates, FilterDateOperation operation) {
    StringBuilder uri = new StringBuilder("/inn-reach/transactions?");

    uri.append(String.join("&", 
        mapItems(dates, dt -> dateParam + "=" + URLEncoder.encode(dt.format(ISO_OFFSET_DATE_TIME), StandardCharsets.UTF_8))
    ));

    if (operation != null) {
      uri.append('&').append(dateParam).append("Op").append("=").append(operation.getValue());
    }

    return uri.toString(); 
  }

}
