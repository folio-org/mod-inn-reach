package org.folio.innreach.repository;

import org.apache.commons.lang3.RandomUtils;
import org.folio.innreach.domain.entity.ItemTypeMapping;
import org.folio.innreach.fixture.TestUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.jdbc.Sql;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toList;
import static org.folio.innreach.fixture.MappingFixture.createItemTypeMapping;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ItemTypeMappingRepositoryTest extends BaseRepositoryTest {
  private static final String PRE_POPULATED_ITEM_TYPE_MAPPING_ID1 = "f8c5d329-c3db-40c1-9e96-d6176f76b0da";
  private static final String PRE_POPULATED_ITEM_TYPE_MAPPING_ID2 = "606f5a38-9e1f-45d0-856e-899c5667410d";
  private static final String PRE_POPULATED_ITEM_TYPE_MAPPING_ID3 = "c6e73e3c-eca6-40d0-a6fc-4588f64ef4a9";
  private static final String PRE_POPULATED_ITEM_TYPE_MAPPING_ID4 = "9de1d6de-6508-4d12-b8d7-0a956c19bfdf";

  private static final String PRE_POPULATED_MATERIAL_TYPE_ID = "0d1fb482-4012-4e16-9427-aaffdf4c0722";

  private static final String PRE_POPULATED_USER = "admin";
  private static final String PRE_POPULATED_LOCAL_SERVER_CODE = "fli01";
  private static final String PRE_POPULATED_INN_REACH_CENTRAL_SERVER_ID = "a849e242-9665-4519-81b2-92e75e4fda7c";

  @Autowired
  private ItemTypeMappingRepository repository;

  @Test
  @Sql(scripts = {"classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/central-server/pre-populate-another-central-server.sql",
    "classpath:db/item-type-mapping/pre-populate-item-type-mapping.sql",
    "classpath:db/item-type-mapping/pre-populate-another-item-type-mapping.sql"})
  void shouldFindAllExistingMappings() {
    var mappings = repository.findAll();

    assertEquals(4, mappings.size());

    List<String> ids = mappings.stream()
      .map(mapping -> mapping.getId().toString())
      .collect(toList());

    assertEquals(List.of(PRE_POPULATED_ITEM_TYPE_MAPPING_ID1, PRE_POPULATED_ITEM_TYPE_MAPPING_ID2,
      PRE_POPULATED_ITEM_TYPE_MAPPING_ID3, PRE_POPULATED_ITEM_TYPE_MAPPING_ID4), ids);
  }

  @Test
  @Sql(scripts = {"classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/item-type-mapping/pre-populate-item-type-mapping.sql"})
  void shouldGetMappingWithMetadata() {
    var mapping = repository.getOne(UUID.fromString(PRE_POPULATED_ITEM_TYPE_MAPPING_ID1));

    assertNotNull(mapping);
    assertEquals(UUID.fromString(PRE_POPULATED_ITEM_TYPE_MAPPING_ID1), mapping.getId());
    assertEquals(1, mapping.getCentralItemType());
    assertEquals(UUID.fromString(PRE_POPULATED_MATERIAL_TYPE_ID), mapping.getMaterialTypeId());

    assertEquals(PRE_POPULATED_USER, mapping.getCreatedBy());
    assertNotNull(mapping.getCreatedDate());
    assertEquals(PRE_POPULATED_USER, mapping.getLastModifiedBy());
    assertNotNull(mapping.getLastModifiedDate());
  }

  @Test
  @Sql(scripts = {"classpath:db/central-server/pre-populate-central-server.sql"})
  void shouldSaveNewMapping() {
    var newMapping = createItemTypeMapping();
    newMapping.setLocalServerCode(PRE_POPULATED_LOCAL_SERVER_CODE);

    var saved = repository.saveAndFlush(newMapping);

    var found = repository.getOne(saved.getId());

    assertNotNull(found);
    assertEquals(newMapping.getId(), found.getId());
    assertEquals(saved.getCentralItemType(), found.getCentralItemType());
    assertEquals(saved.getMaterialTypeId(), found.getMaterialTypeId());
  }

  @Test
  @Sql(scripts = {"classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/item-type-mapping/pre-populate-item-type-mapping.sql"})
  void shouldUpdateExistingMapping() {
    var mapping = repository.getOne(fromString(PRE_POPULATED_ITEM_TYPE_MAPPING_ID1));

    UUID newMaterialTypeId = randomUUID();
    int newItemType = RandomUtils.nextInt(0, 256);
    mapping.setCentralItemType(newItemType);
    mapping.setMaterialTypeId(newMaterialTypeId);

    repository.saveAndFlush(mapping);

    var saved = repository.getOne(mapping.getId());

    assertEquals(newItemType, saved.getCentralItemType());
    assertEquals(newMaterialTypeId, saved.getMaterialTypeId());
  }

  @Test
  @Sql(scripts = {"classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/item-type-mapping/pre-populate-item-type-mapping.sql"})
  void shouldDeleteExistingMapping() {
    UUID id = fromString(PRE_POPULATED_ITEM_TYPE_MAPPING_ID1);

    repository.deleteById(id);

    Optional<ItemTypeMapping> deleted = repository.findById(id);
    assertTrue(deleted.isEmpty());
  }

  @Test
  @Sql(scripts = {"classpath:db/central-server/pre-populate-central-server.sql"})
  void throwExceptionWhenSavingWithoutCentralItemType() {
    var mapping = createItemTypeMapping();
    mapping.setLocalServerCode(PRE_POPULATED_LOCAL_SERVER_CODE);
    mapping.setCentralItemType(null);

    assertThrows(DataIntegrityViolationException.class, () -> repository.saveAndFlush(mapping));
  }

  @Test
  @Sql(scripts = {"classpath:db/central-server/pre-populate-central-server.sql"})
  void throwExceptionWhenSavingWithInvalidCentralServerReference() {
    var mapping = createItemTypeMapping();

    mapping.setLocalServerCode(TestUtil.randomFiveCharacterCode());

    var ex = assertThrows(DataIntegrityViolationException.class, () -> repository.saveAndFlush(mapping));
    assertThat(ex.getMessage(), containsString("constraint [fk_item_type_mapping_local_server_code]"));
  }

  @Test
  @Sql(scripts = {"classpath:db/central-server/pre-populate-central-server.sql"})
  void throwExceptionWhenSavingWithInvalidCentralItemType() {
    var mapping = createItemTypeMapping();
    mapping.setLocalServerCode(PRE_POPULATED_LOCAL_SERVER_CODE);

    mapping.setCentralItemType(256);

    assertThrows(DataIntegrityViolationException.class, () -> repository.saveAndFlush(mapping));
  }

  @Test
  @Sql(scripts = {"classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/item-type-mapping/pre-populate-item-type-mapping.sql"})
  void throwExceptionWhenSavingMappingWithItemTypeThatAlreadyExists() {
    var mapping = createItemTypeMapping();
    mapping.setLocalServerCode(PRE_POPULATED_LOCAL_SERVER_CODE);
    mapping.setInnReachCentralServerId(UUID.fromString(PRE_POPULATED_INN_REACH_CENTRAL_SERVER_ID));

    mapping.setCentralItemType(1);

    var ex = assertThrows(DataIntegrityViolationException.class, () -> repository.saveAndFlush(mapping));
    assertThat(ex.getMessage(), containsString("constraint [unq_item_type_inn_reach_central_server]"));
  }
}
