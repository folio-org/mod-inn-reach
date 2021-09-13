package org.folio.innreach.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import static org.folio.innreach.fixture.MappingFixture.createCentralPatronTypeMapping;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;


@Sql(scripts = {
  "classpath:db/central-server/pre-populate-central-server.sql",
  "classpath:db/central-patron-type-mapping/pre-populate-central-patron_type-mapping-table.sql.sql"
})
@Sql(
  scripts = {
    "classpath:db/central-server/clear-central-server-tables.sql",
    "classpath:db/central-patron-type-mapping/clear-central-patron-type-mapping-table.sql.sql"
  },
  executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD
)
class CentralPatronTypeMappingRepositoryTest extends BaseRepositoryTest {

  private static final UUID PRE_POPULATED_CENTRAL_PATRON_TYPE_MAPPING_ID = UUID.fromString("903dd2c6-5752-431a-aa43-ef6e1eb1292e");

  @Autowired
  private CentralPatronTypeMappingRepository repository;

  @Test
  void saveNewCentralPatronTypeMapping() {
    var centralPatronTypeMapping = createCentralPatronTypeMapping();

    var savedMapping = repository.saveAndFlush(centralPatronTypeMapping);

    assertNotNull(savedMapping.getId());
  }

  @Test
  void getCentralPatronTypeMappingById() {
    var centralPatronTypeMapping = repository.getOne(PRE_POPULATED_CENTRAL_PATRON_TYPE_MAPPING_ID);
    assertNotNull(centralPatronTypeMapping);
  }

  @Test
  void updateCentralPatronTypeMapping() {
    var centralPatronTypeMapping = repository.getOne(PRE_POPULATED_CENTRAL_PATRON_TYPE_MAPPING_ID);

    centralPatronTypeMapping.setBarcode("000054321");

    repository.saveAndFlush(centralPatronTypeMapping);

    var updatedCentralPatronTypeMapping = repository.getOne(PRE_POPULATED_CENTRAL_PATRON_TYPE_MAPPING_ID);

    assertEquals("000054321", updatedCentralPatronTypeMapping.getBarcode());
  }

  @Test
  void deleteCentralPatronTypeMapping() {
    repository.deleteById(PRE_POPULATED_CENTRAL_PATRON_TYPE_MAPPING_ID);

    assertEquals(0, repository.count());
  }

}
