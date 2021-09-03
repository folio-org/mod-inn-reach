package org.folio.innreach.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import static org.folio.innreach.fixture.MappingFixture.createInnReachPatronTypeMapping;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;


@Sql(scripts = {
  "classpath:db/central-server/pre-populate-central-server.sql",
  "classpath:db/inn-reach-patron-type-mapping/pre-populate-inn_reach_patron_type_mapping_table.sql"
})
@Sql(
  scripts = {
    "classpath:db/central-server/clear-central-server-tables.sql",
    "classpath:db/inn-reach-patron-type-mapping/clear-inn_reach_patron_type_mapping_table.sql"
  },
  executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD
)
class InnReachPatronTypeMappingRepositoryTest extends BaseRepositoryTest {

  private static final UUID PRE_POPULATED_INN_REACH_PATRON_TYPE_MAPPING_ID = UUID.fromString("903dd2c6-5752-431a-aa43-ef6e1eb1292e");

  @Autowired
  private InnReachPatronTypeMappingRepository repository;

  @Test
  @Sql(scripts = {
    "classpath:db/central-server/clear-central-server-tables.sql",
    "classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/inn-reach-patron-type-mapping/clear-inn_reach_patron_type_mapping_table.sql",
    "classpath:db/inn-reach-patron-type-mapping/pre-populate-inn_reach_patron_type_mapping_table.sql"
  })
  void saveNewInnReachPatronTypeMapping() {
    var innReachPatronTypeMapping = createInnReachPatronTypeMapping();

    var savedMapping = repository.saveAndFlush(innReachPatronTypeMapping);

    assertNotNull(savedMapping.getId());
  }

  @Test
  @Sql(scripts = {
    "classpath:db/central-server/clear-central-server-tables.sql",
    "classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/inn-reach-patron-type-mapping/clear-inn_reach_patron_type_mapping_table.sql",
    "classpath:db/inn-reach-patron-type-mapping/pre-populate-inn_reach_patron_type_mapping_table.sql"
  })
  void getInnReachPatronTypeMappingById() {
    var innReachPatronTypeMapping = repository.getOne(PRE_POPULATED_INN_REACH_PATRON_TYPE_MAPPING_ID);
    assertNotNull(innReachPatronTypeMapping);
  }

  @Test
  @Sql(scripts = {
    "classpath:db/central-server/clear-central-server-tables.sql",
    "classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/inn-reach-patron-type-mapping/clear-inn_reach_patron_type_mapping_table.sql",
    "classpath:db/inn-reach-patron-type-mapping/pre-populate-inn_reach_patron_type_mapping_table.sql"
  })
  void updateInnReachPatronTypeMapping() {
    var innReachPatronTypeMapping = repository.getOne(PRE_POPULATED_INN_REACH_PATRON_TYPE_MAPPING_ID);

    innReachPatronTypeMapping.setFolioUserBarcode("000054321");

    repository.saveAndFlush(innReachPatronTypeMapping);

    var updatedInnReachPatronMapping = repository.getOne(PRE_POPULATED_INN_REACH_PATRON_TYPE_MAPPING_ID);

    assertEquals("000054321", updatedInnReachPatronMapping.getFolioUserBarcode());
  }

  @Test
  @Sql(scripts = {
    "classpath:db/central-server/clear-central-server-tables.sql",
    "classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/inn-reach-patron-type-mapping/clear-inn_reach_patron_type_mapping_table.sql",
    "classpath:db/inn-reach-patron-type-mapping/pre-populate-inn_reach_patron_type_mapping_table.sql"
  })
  void deleteInnReachPatronTypeMapping() {
    repository.deleteById(PRE_POPULATED_INN_REACH_PATRON_TYPE_MAPPING_ID);

    assertEquals(0, repository.count());
  }

}
