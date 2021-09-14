package org.folio.innreach.repository;

import org.folio.innreach.domain.entity.InnReachTransaction;
import org.folio.innreach.domain.entity.TransactionPatronHold;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.jdbc.Sql;

import java.util.Optional;
import java.util.UUID;

import static org.folio.innreach.domain.entity.InnReachTransaction.TransactionState.PATRON_HOLD;
import static org.folio.innreach.fixture.InnReachTransactionFixture.createInnReachTransaction;
import static org.folio.innreach.fixture.TestUtil.randomFiveCharacterCode;
import static org.folio.innreach.fixture.TestUtil.randomInteger;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InnReachTransactionRepositoryTest extends BaseRepositoryTest {
  private static final String PRE_POPULATED_INN_REACH_TRANSACTION_ID1 = "0aab1720-14b4-4210-9a19-0d0bf1cd64d3";
  private static final String PRE_POPULATED_TRANSACTION_TRACKING_ID1 = "65097d7c-2697-468d-ad20-1568d9cffccc";
  private static final String PRE_POPULATED_TRANSACTION_PICKUP_LOCATION_ID1 = "809adcde-3e67-4822-9916-fd653a681358";

  private static final String PRE_POPULATED_CENTRAL_SERVER_CODE = "fli01";

  @Autowired
  private InnReachTransactionRepository repository;

  @Test
  @Sql(scripts = {"classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/inn-reach-transaction/pre-populate-inn-reach-transaction.sql"})
  void getInnReachTransaction_when_innReachTractionExists() {
    var fromDb = repository.fetchOneByTrackingId(UUID.fromString(PRE_POPULATED_TRANSACTION_TRACKING_ID1)).get();

    assertNotNull(fromDb);
    assertEquals(UUID.fromString(PRE_POPULATED_INN_REACH_TRANSACTION_ID1), fromDb.getId());
    assertEquals(PRE_POPULATED_CENTRAL_SERVER_CODE, fromDb.getCentralServerCode());
    assertEquals(PATRON_HOLD, fromDb.getState());
    assertEquals(UUID.fromString(PRE_POPULATED_TRANSACTION_PICKUP_LOCATION_ID1), fromDb.getHold().getPickupLocation().getId());

    var hold = (TransactionPatronHold) fromDb.getHold();
    assertEquals("title1", hold.getTitle());
  }

  @Test
  @Sql(scripts = {"classpath:db/central-server/pre-populate-central-server.sql"})
  void saveInnReachTransaction_when_innReachTransactionDoesNotExists() {
    var created = createInnReachTransaction();
    var saved = repository.save(created);

    assertNotNull(saved);
    assertNotNull(saved.getId());
    assertEquals(created.getCentralServerCode(), saved.getCentralServerCode());
    assertEquals(created.getHold(), saved.getHold());
    assertEquals(created.getState(), saved.getState());
  }

  @Test
  @Sql(scripts = {"classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/inn-reach-transaction/pre-populate-inn-reach-transaction.sql"})
  void updateInnReachTransaction_when_innReachTransactionDataIsValid() {
    var saved = repository.fetchOneByTrackingId(UUID.fromString(PRE_POPULATED_TRANSACTION_TRACKING_ID1)).get();

    var updatedHold = (TransactionPatronHold) saved.getHold();
    var updatedState = InnReachTransaction.TransactionState.values()[randomInteger(16)];
    var updatedTitle = "updatedTitle";
    updatedHold.setCentralItemType(randomInteger(256));
    updatedHold.setItemAgencyCode(randomFiveCharacterCode());
    updatedHold.setPatronId(UUID.randomUUID());
    updatedHold.setTitle(updatedTitle);

    saved.setHold(updatedHold);
    saved.setState(updatedState);

    var updated = repository.save(saved);
    var savedUpdatedHold = (TransactionPatronHold) updated.getHold();

    assertEquals(updatedHold.getCentralItemType(), savedUpdatedHold.getCentralItemType());
    assertEquals(updatedHold.getItemAgencyCode(), savedUpdatedHold.getItemAgencyCode());
    assertEquals(updatedHold.getPatronId(), savedUpdatedHold.getPatronId());
    assertEquals(updatedTitle, savedUpdatedHold.getTitle());
    assertEquals(updatedState, updated.getState());
  }

  @Test
  @Sql(scripts = {"classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/inn-reach-transaction/pre-populate-inn-reach-transaction.sql"})
  void deleteInnReachTransaction_when_innReachTransactionExists() {
    UUID id = UUID.fromString(PRE_POPULATED_INN_REACH_TRANSACTION_ID1);
    repository.deleteById(id);

    Optional<InnReachTransaction> deletedTransaction = repository.findById(id);
    assertTrue(deletedTransaction.isEmpty());
  }

  @Test
  @Sql(scripts = {"classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/inn-reach-transaction/pre-populate-inn-reach-transaction.sql"})
  void throwException_when_updatingInnReachTransactionWithInvalidAgencyCode() {
    var saved = repository.fetchOneByTrackingId(UUID.fromString(PRE_POPULATED_TRANSACTION_TRACKING_ID1)).get();

    saved.getHold().setItemAgencyCode("qwerty123");

    assertThrows(DataIntegrityViolationException.class, () -> repository.saveAndFlush(saved));
  }

  @Test
  @Sql(scripts = {"classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/inn-reach-transaction/pre-populate-inn-reach-transaction.sql"})
  void throwException_when_updatingInnReachTransactionWithInvalidItemType() {
    var saved = repository.fetchOneByTrackingId(UUID.fromString(PRE_POPULATED_TRANSACTION_TRACKING_ID1)).get();

    saved.getHold().setCentralItemType(256);

    assertThrows(DataIntegrityViolationException.class, () -> repository.saveAndFlush(saved));
  }

  @Test
  @Sql(scripts = {"classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/inn-reach-transaction/pre-populate-inn-reach-transaction.sql"})
  void throwException_when_updatingInnReachTransactionWithoutRequiredFields() {
    var saved = repository.fetchOneByTrackingId(UUID.fromString(PRE_POPULATED_TRANSACTION_TRACKING_ID1)).get();

    saved.getHold().setPatronId(null);

    assertThrows(DataIntegrityViolationException.class, () -> repository.saveAndFlush(saved));
  }
}
