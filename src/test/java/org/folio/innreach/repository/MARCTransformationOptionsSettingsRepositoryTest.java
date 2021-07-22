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
  private static final String PRE_POPULATED_CENTRAL_SERVER_RECORD_ID = "ff0a9220-d9bc-4c21-8087-78387e734d89";

  @Autowired
  private MARCTransformationOptionsSettingsRepository repository;

  @Test
  @Sql(scripts = {"classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/marc-transform-opt-set/pre-populate-marc-transform-opt-set.sql"})
  void getMARCTransformOptSet_when_MARCTransformOptSetExists() {
    var MARCTransformOptSetById = repository.getOne(UUID.fromString(PRE_POPULATED_MARC_TRANSFORM_OPT_SET_ID));

    assertNotNull(MARCTransformOptSetById);
    assertEquals(UUID.fromString(PRE_POPULATED_CENTRAL_SERVER_ID), MARCTransformOptSetById.getCentralServer().getId());
    assertEquals(UUID.fromString(PRE_POPULATED_CENTRAL_SERVER_RECORD_ID), MARCTransformOptSetById.getCentralServerRecordId());
    assertEquals(true, MARCTransformOptSetById.getConfigIsActive());
    assertEquals(1, MARCTransformOptSetById.getExcludedMARCFields().size());
    assertEquals(1, MARCTransformOptSetById.getModifiedFieldsForContributedRecords().size());
  }

  @Test
  @Sql(scripts = {"classpath:db/central-server/pre-populate-central-server.sql"})
  void saveMARCTransformOptSet_when_MARCTransformOptSetDoesNotExists() {
    var MARCTransformOptSet = createMARCTransformOptSet();
    MARCTransformOptSet.setCentralServer(refCentralServer(UUID.fromString(PRE_POPULATED_CENTRAL_SERVER_ID)));
    var savedMARCTransformOptSet = repository.save(MARCTransformOptSet);

    assertNotNull(savedMARCTransformOptSet);
    assertNotNull(savedMARCTransformOptSet.getId());
    assertEquals(MARCTransformOptSet.getCentralServer(), savedMARCTransformOptSet.getCentralServer());
    assertEquals(MARCTransformOptSet.getConfigIsActive(), savedMARCTransformOptSet.getConfigIsActive());
    assertEquals(MARCTransformOptSet.getCentralServerRecordId(), savedMARCTransformOptSet.getCentralServerRecordId());
    assertEquals(MARCTransformOptSet.getExcludedMARCFields(), savedMARCTransformOptSet.getExcludedMARCFields());
    assertEquals(MARCTransformOptSet.getModifiedFieldsForContributedRecords(), savedMARCTransformOptSet.getModifiedFieldsForContributedRecords());
  }

  @Test
  @Sql(scripts = {"classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/marc-transform-opt-set/pre-populate-marc-transform-opt-set.sql"})
  void updateMARCTransformOptSet_when_MARCTransformOptSetDataIsValid() {
    var savedMARCTransformOptSet = repository.getOne(UUID.fromString(PRE_POPULATED_MARC_TRANSFORM_OPT_SET_ID));

    var updatedModifiedFields = savedMARCTransformOptSet.getModifiedFieldsForContributedRecords();
    updatedModifiedFields.add(createFieldConfig());
    var updatedConfigIsActive = !savedMARCTransformOptSet.getConfigIsActive();
    var updatedExcludedMARCFields = savedMARCTransformOptSet.getExcludedMARCFields();
    updatedExcludedMARCFields.clear();
    var updatedCentralServerRecordId = UUID.randomUUID();

    savedMARCTransformOptSet.setModifiedFieldsForContributedRecords(updatedModifiedFields);
    savedMARCTransformOptSet.setConfigIsActive(updatedConfigIsActive);
    savedMARCTransformOptSet.setExcludedMARCFields(updatedExcludedMARCFields);
    savedMARCTransformOptSet.setCentralServerRecordId(updatedCentralServerRecordId);

    var updatedMARCTransformOptSet = repository.save(savedMARCTransformOptSet);

    assertEquals(updatedModifiedFields, updatedMARCTransformOptSet.getModifiedFieldsForContributedRecords());
    assertEquals(updatedConfigIsActive, updatedMARCTransformOptSet.getConfigIsActive());
    assertEquals(updatedExcludedMARCFields, updatedMARCTransformOptSet.getExcludedMARCFields());
    assertEquals(updatedCentralServerRecordId, updatedMARCTransformOptSet.getCentralServerRecordId());
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
