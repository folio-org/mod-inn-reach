package org.folio.innreach.controller.d2ir;

import static io.github.benas.randombeans.FieldPredicates.named;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.AFTER_TEST_METHOD;
import static org.springframework.test.context.jdbc.SqlMergeMode.MergeMode.MERGE;

import io.github.benas.randombeans.EnhancedRandomBuilder;
import io.github.benas.randombeans.api.EnhancedRandom;
import io.github.benas.randombeans.api.Randomizer;
import io.github.glytching.junit.extension.random.Random;
import io.github.glytching.junit.extension.random.RandomBeansExtension;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlMergeMode;

import org.folio.innreach.controller.base.BaseApiControllerTest;
import org.folio.innreach.domain.entity.InnReachTransaction;
import org.folio.innreach.dto.TransferRequestDTO;
import org.folio.innreach.repository.InnReachTransactionRepository;

@Sql(scripts = {
        "classpath:db/central-server/clear-central-server-tables.sql",
        "classpath:db/inn-reach-transaction/clear-inn-reach-transaction-tables.sql"},
    executionPhase = AFTER_TEST_METHOD
)
@SqlMergeMode(MERGE)
public class TransferRequestCirculationApiTest extends BaseApiControllerTest {

  private static final String PRE_POPULATED_TRACKING_ID = "tracking1";
  private static final String PRE_POPULATED_CENTRAL_CODE = "d2ir";
  private static final String PRE_POPULATED_ITEM_ID = "item1";
  private static final String NEW_ITEM_ID = "newitem";

  private static final String TRANSFERREQ_URL = "/inn-reach/d2ir/circ/transferrequest/{trackingId}/{centralCode}";

  static EnhancedRandom enhancedRandom = EnhancedRandomBuilder.aNewEnhancedRandomBuilder()
      .randomize(named("patronId"), randomAlphanumeric32Max())
      .randomize(named("patronAgencyCode"), randomAlphanumeric5())
      .randomize(named("itemAgencyCode"), randomAlphanumeric5())
      .randomize(named("itemId"), randomAlphanumeric32Max())
      .randomize(named("newItemId"), randomAlphanumeric32Max())
      .build();

  @RegisterExtension
  static RandomBeansExtension randomBeansExtension = new RandomBeansExtension(enhancedRandom);

  @Autowired
  private InnReachTransactionRepository repository;


  @Test
  @Sql(scripts = {
      "classpath:db/central-server/pre-populate-central-server.sql",
      "classpath:db/inn-reach-transaction/pre-populate-inn-reach-transaction.sql"
  })
  void updateTransactionItemId_with_newItemFromRequest(@Random TransferRequestDTO req) throws Exception {
    req.setItemId(PRE_POPULATED_ITEM_ID);
    req.setNewItemId(NEW_ITEM_ID);

    putAndExpectOk(transferReqUri(), req);

    var trx = getTransaction(PRE_POPULATED_TRACKING_ID, PRE_POPULATED_CENTRAL_CODE);

    assertEquals(NEW_ITEM_ID, trx.getHold().getItemId());
  }

  private static Randomizer<String> randomAlphanumeric32Max() {
    return () -> RandomStringUtils.randomAlphanumeric(1, 33).toLowerCase();
  }

  private static Randomizer<String> randomAlphanumeric5() {
    return () -> RandomStringUtils.randomAlphanumeric(5).toLowerCase();
  }

  private static URI transferReqUri() {
    return URI.of(TRANSFERREQ_URL, PRE_POPULATED_TRACKING_ID, PRE_POPULATED_CENTRAL_CODE);
  }

  private InnReachTransaction getTransaction(String trackingId, String centralCode) {
    return repository.findByTrackingIdAndCentralServerCode(trackingId, centralCode).orElseThrow();
  }

  private void putAndExpectOk(URI uri, Object requestBody) throws Exception {
    putAndExpect(uri, requestBody, Template.of("circulation/ok-response.json"));
  }
}