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
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlMergeMode;

import org.folio.innreach.controller.base.BaseControllerTest;
import org.folio.innreach.domain.entity.InnReachTransaction;
import org.folio.innreach.dto.FilterDateOperation;
import org.folio.innreach.dto.InnReachTransactionDTO;
import org.folio.innreach.dto.InnReachTransactionsDTO;
import org.folio.innreach.repository.InnReachTransactionRepository;

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
public class InnReachTransactionDateFilterTest extends BaseControllerTest {

  private static final String PRE_POPULATED_TRACKING_ID1 = "tracking1";
  private static final String PRE_POPULATED_TRACKING_ID2 = "tracking2";
  private static final String PRE_POPULATED_TRACKING_ID3 = "tracking3";

  private static final OffsetDateTime TARGET_DATE = OffsetDateTime.parse(
      "2022-02-24T05:00:00+02:00",
      ISO_OFFSET_DATE_TIME);

  @Autowired
  private TestRestTemplate testRestTemplate;
  @Autowired
  private InnReachTransactionRepository repository;


  @Nested
  class FilterByCreatedDateTest {

    @BeforeEach
    void changeCreatedDate() {
      var transactions = repository.findAll();

      var trx1 = findTrxByTrackingId(transactions, PRE_POPULATED_TRACKING_ID1);
      trx1.setCreatedDate(TARGET_DATE);

      var trx2 = findTrxByTrackingId(transactions, PRE_POPULATED_TRACKING_ID2);
      trx2.setCreatedDate(TARGET_DATE.minusMonths(1));

      var trx3 = findTrxByTrackingId(transactions, PRE_POPULATED_TRACKING_ID3);
      trx3.setCreatedDate(TARGET_DATE.plusMonths(1));

      repository.saveAll(transactions);
    }

    @ParameterizedTest
    @MethodSource("org.folio.innreach.controller.InnReachTransactionDateFilterTest#auditDateTestData")
    void returnTransactions_when_FilteredByCreatedDate(List<OffsetDateTime> dates, FilterDateOperation operation,
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

  }

  @Nested
  class FilterByUpdatedDateTest {

    @BeforeEach
    void changeUpdatedDate() {
      var transactions = repository.findAll();

      var trx1 = findTrxByTrackingId(transactions, PRE_POPULATED_TRACKING_ID1);
      trx1.setUpdatedDate(TARGET_DATE);

      var trx2 = findTrxByTrackingId(transactions, PRE_POPULATED_TRACKING_ID2);
      trx2.setUpdatedDate(TARGET_DATE.minusMonths(1));

      var trx3 = findTrxByTrackingId(transactions, PRE_POPULATED_TRACKING_ID3);
      trx3.setUpdatedDate(TARGET_DATE.plusMonths(1));

      repository.saveAll(transactions);
    }

    @ParameterizedTest
    @MethodSource("org.folio.innreach.controller.InnReachTransactionDateFilterTest#auditDateTestData")
    void returnTransactions_when_FilteredByUpdatedDate(List<OffsetDateTime> dates, FilterDateOperation operation,
        List<String> expectedTrxTrackingIds) {
      var trx = repository.findAll();

      trx.forEach(t -> System.out.printf("Transaction: tracking id = %s, updated date = %s%n",
          t.getTrackingId(), t.getUpdatedDate().format(ISO_OFFSET_DATE_TIME)));

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

  }

  static Stream<Arguments> auditDateTestData() {
    return Stream.of(
        arguments(List.of(TARGET_DATE), less, List.of(PRE_POPULATED_TRACKING_ID2)),
        arguments(List.of(TARGET_DATE), lessOrEqual, List.of(PRE_POPULATED_TRACKING_ID1, PRE_POPULATED_TRACKING_ID2)),
        arguments(List.of(TARGET_DATE), equal, List.of(PRE_POPULATED_TRACKING_ID1)),
        arguments(List.of(TARGET_DATE), notEqual, List.of(PRE_POPULATED_TRACKING_ID2, PRE_POPULATED_TRACKING_ID3)),
        arguments(List.of(TARGET_DATE), greater, List.of(PRE_POPULATED_TRACKING_ID3)),
        arguments(List.of(TARGET_DATE), greaterOrEqual, List.of(PRE_POPULATED_TRACKING_ID1, PRE_POPULATED_TRACKING_ID3)),
        arguments(List.of(TARGET_DATE.minusDays(1), TARGET_DATE.plusDays(1)), between, List.of(PRE_POPULATED_TRACKING_ID1)),
        arguments(List.of(TARGET_DATE), null, List.of(PRE_POPULATED_TRACKING_ID1)) // special case when operation is omitted -- treated as "equal"
    );
  }

  private static InnReachTransaction findTrxByTrackingId(List<InnReachTransaction> transactions,
      String trackingId) {
    return transactions.stream()
        .filter(trx -> trx.getTrackingId().equals(trackingId))
        .findFirst()
        .orElseThrow();
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
