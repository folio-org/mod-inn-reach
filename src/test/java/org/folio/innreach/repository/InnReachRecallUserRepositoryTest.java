package org.folio.innreach.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import static org.folio.innreach.fixture.InnReachRecallUserFixture.createInnReachRecallUser;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;

import org.folio.innreach.domain.entity.InnReachRecallUser;

@Sql(scripts = {
  "/db/inn-reach-recall-user/pre-populate-inn-reach-recall-user.sql",
  "/db/central-server/pre-populate-central-server.sql"
})
@Sql(scripts = {
  "/db/central-server/clear-central-server-tables.sql",
  "/db/inn-reach-recall-user/clear-inn-reach-recall-user.sql"
  }, executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
class InnReachRecallUserRepositoryTest extends BaseRepositoryTest {

  private static final UUID PRE_POPULATED_INN_REACH_RECALL_USER_ID = UUID.fromString(
      "ef58f191-ec62-44bb-a571-d59c536bcf4a");

  @Autowired
  private InnReachRecallUserRepository repository;

  @Test
  void shouldFindAllExistingUsers() {
    var innReachRecallUsers = repository.findAll();
    assertEquals(2, innReachRecallUsers.size());
  }

  @Test
  void shouldFindInnReachRecallUserById() {
    var innReachRecallUser = repository.findById(PRE_POPULATED_INN_REACH_RECALL_USER_ID);
    assertNotNull(innReachRecallUser);
  }

  @Test
  void shouldSaveNewInnReachRecallUser() {
    var innReachRecallUser = createInnReachRecallUser();

    var savedRecallUser = repository.save(innReachRecallUser);

    Optional<InnReachRecallUser> found = repository.findById(savedRecallUser.getId());

    assertTrue(found.isPresent());
  }

  @Test
  void shouldDeleteExistingInnReachRecallUser() {
    repository.deleteById(PRE_POPULATED_INN_REACH_RECALL_USER_ID);

    Optional<InnReachRecallUser> deleted = repository.findById(PRE_POPULATED_INN_REACH_RECALL_USER_ID);

    assertTrue(deleted.isEmpty());
  }

}
