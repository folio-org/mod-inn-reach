package org.folio.innreach.repository;

import org.folio.innreach.domain.entity.MARCTransformationOptionsSettings;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;

import java.util.Optional;
import java.util.UUID;

import static org.folio.innreach.fixture.MARCTransformationOptionsSettingsFixture.createFieldConfig;
import static org.folio.innreach.fixture.MARCTransformationOptionsSettingsFixture.createMARCTransformOptSet;
import static org.folio.innreach.fixture.TestUtil.refCentralServer;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MARCTransformationOptionsSettingsRepositoryTest extends BaseRepositoryTest {
  private static final String PRE_POPULATED_MARC_TRANSFORM_OPT_SET_ID = "51768f15-41e8-494d-bc4d-a308568e7052";
  private static final String PRE_POPULATED_CENTRAL_SERVER_ID = "edab6baf-c696-42b1-89bb-1bbb8759b0d2";

  @Autowired
  private MARCTransformationOptionsSettingsRepository repository;

  @Test
  @Sql(scripts = {"classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/marc-transform-opt-set/pre-populate-marc-transform-opt-set.sql"})
  void getMARCTransformOptSet_when_MARCTransformOptSetExists() {
    var fromDb = repository.getOne(UUID.fromString(PRE_POPULATED_MARC_TRANSFORM_OPT_SET_ID));

    assertNotNull(fromDb);
    assertEquals(UUID.fromString(PRE_POPULATED_CENTRAL_SERVER_ID), fromDb.getCentralServer().getId());
    assertTrue(fromDb.getConfigIsActive());
    assertEquals(1, fromDb.getExcludedMARCFields().size());
    assertEquals(1, fromDb.getModifiedFieldsForContributedRecords().size());
  }

  @Test
  @Sql(scripts = {"classpath:db/central-server/pre-populate-central-server.sql"})
  void saveMARCTransformOptSet_when_MARCTransformOptSetDoesNotExists() {
    var created = createMARCTransformOptSet();
    created.setCentralServer(refCentralServer(UUID.fromString(PRE_POPULATED_CENTRAL_SERVER_ID)));
    var saved = repository.save(created);

    assertNotNull(saved);
    assertNotNull(saved.getId());
    assertEquals(created.getCentralServer(), saved.getCentralServer());
    assertEquals(created.getConfigIsActive(), saved.getConfigIsActive());
    assertEquals(created.getExcludedMARCFields(), saved.getExcludedMARCFields());
    assertEquals(created.getModifiedFieldsForContributedRecords(), saved.getModifiedFieldsForContributedRecords());
  }

  @Test
  @Sql(scripts = {"classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/marc-transform-opt-set/pre-populate-marc-transform-opt-set.sql"})
  void updateMARCTransformOptSet_when_MARCTransformOptSetDataIsValid() {
    var saved = repository.getOne(UUID.fromString(PRE_POPULATED_MARC_TRANSFORM_OPT_SET_ID));

    var updatedModifiedFields = saved.getModifiedFieldsForContributedRecords();
    updatedModifiedFields.add(createFieldConfig());
    var updatedConfigIsActive = !saved.getConfigIsActive();
    var updatedExcludedMARCFields = saved.getExcludedMARCFields();
    updatedExcludedMARCFields.clear();
    var updatedCentralServerRecordId = UUID.randomUUID();

    saved.setModifiedFieldsForContributedRecords(updatedModifiedFields);
    saved.setConfigIsActive(updatedConfigIsActive);
    saved.setExcludedMARCFields(updatedExcludedMARCFields);

    var updated = repository.save(saved);

    assertEquals(updatedModifiedFields, updated.getModifiedFieldsForContributedRecords());
    assertEquals(updatedConfigIsActive, updated.getConfigIsActive());
    assertEquals(updatedExcludedMARCFields, updated.getExcludedMARCFields());
  }

  @Test
  @Sql(scripts = {"classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/marc-transform-opt-set/pre-populate-marc-transform-opt-set.sql"})
  void deleteItmContribOptConf_when_itmContribOptConfExists() {
    UUID id = UUID.fromString(PRE_POPULATED_MARC_TRANSFORM_OPT_SET_ID);
    repository.deleteById(id);

    Optional<MARCTransformationOptionsSettings> deletedItmContribOptConf = repository.findById(id);
    assertTrue(deletedItmContribOptConf.isEmpty());
  }
}
