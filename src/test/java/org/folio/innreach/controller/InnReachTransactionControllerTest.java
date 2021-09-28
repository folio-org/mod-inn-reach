package org.folio.innreach.controller;

import org.folio.innreach.controller.base.BaseControllerTest;
import org.folio.innreach.domain.entity.TransactionItemHold;
import org.folio.innreach.dto.InnReachResponseDTO;
import org.folio.innreach.dto.TransactionItemHoldDTO;
import org.folio.innreach.mapper.InnReachTransactionMapper;
import org.folio.innreach.repository.InnReachTransactionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlMergeMode;

import static org.folio.innreach.domain.entity.InnReachTransaction.TransactionType.ITEM;
import static org.folio.innreach.fixture.TestUtil.deserializeFromJsonFile;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.AFTER_TEST_METHOD;
import static org.springframework.test.context.jdbc.SqlMergeMode.MergeMode.MERGE;

@Sql(
  scripts = {
    "classpath:db/central-server/clear-central-server-tables.sql"},
  executionPhase = AFTER_TEST_METHOD
)
@SqlMergeMode(MERGE)
class InnReachTransactionControllerTest extends BaseControllerTest {

  private static final String TRACKING_ID = "trackingid1";
  private static final String PRE_POPULATED_TRACKING_ID = "tracking1";
  private static final String PRE_POPULATED_CENTRAL_SERVER_CODE = "d2ir";

  @Autowired
  private TestRestTemplate testRestTemplate;
  @Autowired
  private InnReachTransactionRepository repository;
  @Autowired
  private InnReachTransactionMapper mapper;

  @Test
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql"
  })
  void return200HttpCode_and_createdInnReachTransactionEntity_when_createInnReachTransactionWithItemHold() {
    var itemHoldDTO = deserializeFromJsonFile(
      "/inn-reach-transaction/create-item-hold-request.json", TransactionItemHoldDTO.class);

    var responseEntity = testRestTemplate.postForEntity(
      "/innreach/v2/circ/itemHold/{trackingId}/{centralCode}", itemHoldDTO, InnReachResponseDTO.class, TRACKING_ID,
      PRE_POPULATED_CENTRAL_SERVER_CODE);

    assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
    assertTrue(responseEntity.hasBody());
    assertEquals("ok", responseEntity.getBody().getStatus());

    var createdTransaction = repository.fetchOneByTrackingId(TRACKING_ID);

    assertTrue(createdTransaction.isPresent());
    assertEquals(PRE_POPULATED_CENTRAL_SERVER_CODE, createdTransaction.get().getCentralServerCode());
    assertEquals(ITEM, createdTransaction.get().getType());
    assertEquals(itemHoldDTO.getItemId(), createdTransaction.get().getHold().getItemId());
    assertEquals(itemHoldDTO.getItemAgencyCode(), createdTransaction.get().getHold().getItemAgencyCode());
    assertEquals(mapper.map(itemHoldDTO.getPickupLocation()).getDisplayName(),
      createdTransaction.get().getHold().getPickupLocation().getDisplayName());
    assertEquals(itemHoldDTO.getTransactionTime(), createdTransaction.get().getHold().getTransactionTime());
    assertEquals(itemHoldDTO.getPatronName(), ((TransactionItemHold) createdTransaction.get().getHold()).getPatronName());
  }

  @Test
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql"
  })
  void return409HttpCode_when_createInnReachTransactionWithItemHoldWithInvalidPatronId() {
    var itemHoldDTO = deserializeFromJsonFile(
      "/inn-reach-transaction/create-item-hold-invalid-patron-id-request.json", TransactionItemHoldDTO.class);

    var responseEntity = testRestTemplate.postForEntity(
      "/innreach/v2/circ/itemHold/{trackingId}/{centralCode}", itemHoldDTO, InnReachResponseDTO.class, TRACKING_ID,
      PRE_POPULATED_CENTRAL_SERVER_CODE);

    assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
    assertEquals("failed", responseEntity.getBody().getStatus());
    assertThat(responseEntity.getBody().getReason(), containsString("must match \"[a-z,0-9]{1,32}\""));
  }

  @Test
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql"
  })
  void return409HttpCode_when_createInnReachTransactionWithItemHoldWithInvalidCentralItemType() {
    var itemHoldDTO = deserializeFromJsonFile(
      "/inn-reach-transaction/create-item-hold-invalid-central-item-type-request.json", TransactionItemHoldDTO.class);

    var responseEntity = testRestTemplate.postForEntity(
      "/innreach/v2/circ/itemHold/{trackingId}/{centralCode}", itemHoldDTO,InnReachResponseDTO.class, TRACKING_ID,
      PRE_POPULATED_CENTRAL_SERVER_CODE);

    assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
    assertEquals("failed", responseEntity.getBody().getStatus());
    assertThat(responseEntity.getBody().getReason(), containsString("must be less than or equal to 255"));
  }

  @Test
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/inn-reach-transaction/pre-populate-inn-reach-transaction.sql",
  })
  void return409HttpCode_when_createInnReachTransaction_and_trackingIdAlreadyExists() {
    var itemHoldDTO = deserializeFromJsonFile(
      "/inn-reach-transaction/create-item-hold-request.json", TransactionItemHoldDTO.class);

    var responseEntity = testRestTemplate.postForEntity(
      "/innreach/v2/circ/itemHold/{trackingId}/{centralCode}", itemHoldDTO, InnReachResponseDTO.class, PRE_POPULATED_TRACKING_ID,
      PRE_POPULATED_CENTRAL_SERVER_CODE);

    assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
    assertEquals("failed", responseEntity.getBody().getStatus());
    assertThat(responseEntity.getBody().getReason(), containsString("constraint [unq_tracking_id]"));
  }

  @Test
  void return409HttpCode_when_createInnReachTransaction_and_centralServerDoesNotExist() {
    var itemHoldDTO = deserializeFromJsonFile(
      "/inn-reach-transaction/create-item-hold-request.json", TransactionItemHoldDTO.class);

    var responseEntity = testRestTemplate.postForEntity(
      "/innreach/v2/circ/itemHold/{trackingId}/{centralCode}", itemHoldDTO, InnReachResponseDTO.class, PRE_POPULATED_TRACKING_ID,
      PRE_POPULATED_CENTRAL_SERVER_CODE);

    assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
    assertEquals("failed", responseEntity.getBody().getStatus());
    assertEquals("Central server with code: d2ir not found", responseEntity.getBody().getReason());
  }
}
