package org.folio.innreach.repository;

import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import static org.folio.innreach.fixture.MappingFixture.createMaterialTypeMapping;
import static org.folio.innreach.util.ListUtils.mapItems;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.apache.commons.lang3.RandomUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.jdbc.Sql;

import org.folio.innreach.domain.entity.MaterialTypeMapping;
import org.folio.innreach.domain.entity.base.AuditableUser;
import org.folio.innreach.fixture.TestUtil;

@Sql(scripts = "classpath:db/central-server/pre-populate-central-server.sql")
@Sql(scripts = "classpath:db/mtype-mapping/pre-populate-material-type-mapping.sql")
class MaterialTypeMappingRepositoryTest extends BaseRepositoryTest {

  private static final String PRE_POPULATED_MAPPING1_ID = "71bd0beb-28cb-40bb-9f40-87463d61a553";
  private static final String PRE_POPULATED_MAPPING2_ID = "d9985d0d-b121-4ccd-ac16-5ebd0ccccf7f";
  private static final String PRE_POPULATED_MAPPING3_ID = "57fad69e-8c91-48c0-a61f-a6122f52737a";
  private static final String PRE_POPULATED_MAPPING4_ID = "6f783255-e0ee-42c2-aa84-669d8c70f107";
  private static final AuditableUser PRE_POPULATED_USER = AuditableUser.SYSTEM;

  @Autowired
  private MaterialTypeMappingRepository repository;

  @Test
  void shouldFindAllExistingMappings() {
    var mappings = repository.findAll();

    assertEquals(4, mappings.size());

    List<String> ids = mapItems(mappings, mapping -> mapping.getId().toString());

    assertEquals(ids, List.of(PRE_POPULATED_MAPPING1_ID, PRE_POPULATED_MAPPING2_ID, PRE_POPULATED_MAPPING3_ID, PRE_POPULATED_MAPPING4_ID));
  }

  @Test
  void shouldGetMappingWithMetadata() {
    var mapping = repository.getOne(fromString(PRE_POPULATED_MAPPING1_ID));

    assertEquals(PRE_POPULATED_USER, mapping.getCreatedBy());
    assertNotNull(mapping.getCreatedDate());
  }

  @Test
  void shouldSaveNewMapping() {
    var newMapping = createMaterialTypeMapping();

    var saved = repository.saveAndFlush(newMapping);

    MaterialTypeMapping found = repository.getOne(saved.getId());
    assertEquals(saved, found);
  }

  @Test
  void shouldUpdateMaterialTypeIdAndItemType() {
    var mapping = repository.getOne(fromString(PRE_POPULATED_MAPPING1_ID));

    UUID newMaterialTypeId = randomUUID();
    int newCentralItemType = RandomUtils.nextInt(0, 256);
    mapping.setMaterialTypeId(newMaterialTypeId);
    mapping.setCentralItemType(newCentralItemType);

    repository.saveAndFlush(mapping);

    var saved = repository.getOne(mapping.getId());

    assertEquals(newMaterialTypeId, saved.getMaterialTypeId());
    assertEquals(newCentralItemType, saved.getCentralItemType());
  }

  @Test
  void shouldDeleteExistingMapping() {
    UUID id = fromString(PRE_POPULATED_MAPPING1_ID);

    repository.deleteById(id);

    Optional<MaterialTypeMapping> deleted = repository.findById(id);
    assertTrue(deleted.isEmpty());
  }

  @Test
  void throwExceptionWhenSavingWithoutMaterialTypeId() {
    var mapping = createMaterialTypeMapping();
    mapping.setMaterialTypeId(null);

    assertThrows(DataIntegrityViolationException.class, () -> repository.saveAndFlush(mapping));
  }

  @Test
  void throwExceptionWhenSavingWithInvalidCentralServerReference() {
    var mapping = createMaterialTypeMapping();

    mapping.setCentralServer(TestUtil.refCentralServer(randomUUID()));

    var ex = assertThrows(DataIntegrityViolationException.class, () -> repository.saveAndFlush(mapping));
    assertThat(ex.getMessage(), containsString("constraint [fk_mtype_mapping_central_server]"));
  }

  @Test
  void throwExceptionWhenNewMaterialTypeIdMappingExistsForTheServer() {
    var existing = repository.getOne(fromString(PRE_POPULATED_MAPPING1_ID));

    var mapping = createMaterialTypeMapping();
    mapping.setMaterialTypeId(existing.getMaterialTypeId());
    mapping.setCentralServer(existing.getCentralServer());

    var ex = assertThrows(DataIntegrityViolationException.class, () -> repository.saveAndFlush(mapping));
    assertThat(ex.getMessage(), containsString("constraint [unq_mtype_mapping_server_mtype]"));
  }

}
