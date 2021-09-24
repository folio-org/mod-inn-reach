package org.folio.innreach.controller;

import org.folio.innreach.controller.base.BaseControllerTest;
import org.folio.innreach.domain.entity.TransactionItemHold;
import org.folio.innreach.dto.D2irResponseDTO;
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
  private static final String CENTRAL_SERVER_CODE = "d2ir";

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
      "/innreach/v2/circ/itemHold/{trackingId}/{centralCode}", itemHoldDTO, D2irResponseDTO.class, TRACKING_ID,
      CENTRAL_SERVER_CODE);

    assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
    assertTrue(responseEntity.hasBody());
    assertEquals("ok", responseEntity.getBody().getStatus());

    var createdTransaction = repository.fetchOneByTrackingId(TRACKING_ID);

    assertTrue(createdTransaction.isPresent());
    assertEquals(CENTRAL_SERVER_CODE, createdTransaction.get().getCentralServerCode());
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
      "/innreach/v2/circ/itemHold/{trackingId}/{centralCode}", itemHoldDTO, D2irResponseDTO.class, TRACKING_ID,
      CENTRAL_SERVER_CODE);

    assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
    assertEquals("failed", responseEntity.getBody().getStatus());
  }

  @Test
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql"
  })
  void return409HttpCode_when_createInnReachTransactionWithItemHoldWithInvalidCentralItemType() {
    var itemHoldDTO = deserializeFromJsonFile(
      "/inn-reach-transaction/create-item-hold-invalid-central-item-type-request.json", TransactionItemHoldDTO.class);

    var responseEntity = testRestTemplate.postForEntity(
      "/innreach/v2/circ/itemHold/{trackingId}/{centralCode}", itemHoldDTO, D2irResponseDTO.class, TRACKING_ID,
      CENTRAL_SERVER_CODE);

    assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
    assertEquals("failed", responseEntity.getBody().getStatus());
  }
}
