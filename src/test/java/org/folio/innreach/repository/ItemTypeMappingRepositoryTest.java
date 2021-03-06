package org.folio.innreach.repository;

import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import static org.folio.innreach.fixture.MappingFixture.createItemTypeMapping;
import static org.folio.innreach.fixture.TestUtil.randomIntegerExcept;
import static org.folio.innreach.util.ListUtils.mapItems;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.jdbc.Sql;

import org.folio.innreach.domain.entity.ItemTypeMapping;
import org.folio.innreach.domain.entity.base.AuditableUser;
import org.folio.innreach.fixture.TestUtil;

class ItemTypeMappingRepositoryTest extends BaseRepositoryTest {
  private static final String PRE_POPULATED_ITEM_TYPE_MAPPING_ID1 = "f8c5d329-c3db-40c1-9e96-d6176f76b0da";
  private static final String PRE_POPULATED_ITEM_TYPE_MAPPING_ID2 = "606f5a38-9e1f-45d0-856e-899c5667410d";
  private static final String PRE_POPULATED_ITEM_TYPE_MAPPING_ID3 = "c6e73e3c-eca6-40d0-a6fc-4588f64ef4a9";
  private static final String PRE_POPULATED_ITEM_TYPE_MAPPING_ID4 = "9de1d6de-6508-4d12-b8d7-0a956c19bfdf";

  private static final Integer PRE_POPULATED_CENTRAL_ITEM_TYPE = 1;
  private static final String PRE_POPULATED_MATERIAL_TYPE_ID = "0d1fb482-4012-4e16-9427-aaffdf4c0722";

  private static final AuditableUser PRE_POPULATED_USER = AuditableUser.SYSTEM;
  private static final String PRE_POPULATED_CENTRAL_SERVER_ID = "edab6baf-c696-42b1-89bb-1bbb8759b0d2";

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

    List<String> ids = mapItems(mappings, mapping -> mapping.getId().toString());

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
    assertEquals(PRE_POPULATED_CENTRAL_ITEM_TYPE, mapping.getCentralItemType());
    assertEquals(UUID.fromString(PRE_POPULATED_MATERIAL_TYPE_ID), mapping.getMaterialTypeId());

    assertEquals(PRE_POPULATED_USER, mapping.getCreatedBy());
    assertNotNull(mapping.getCreatedDate());
  }

  @Test
  @Sql(scripts = {"classpath:db/central-server/pre-populate-central-server.sql"})
  void shouldSaveNewMapping() {
    var newMapping = createItemTypeMapping();
    newMapping.setCentralServer(TestUtil.refCentralServer(UUID.fromString(PRE_POPULATED_CENTRAL_SERVER_ID)));

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
    var mapping1 = repository.getOne(fromString(PRE_POPULATED_ITEM_TYPE_MAPPING_ID1));
    var mapping2 = repository.getOne(fromString(PRE_POPULATED_ITEM_TYPE_MAPPING_ID2));

    UUID newMaterialTypeId = randomUUID();
    int newItemType = randomIntegerExcept(256, Set.of(mapping1.getCentralItemType(), mapping2.getCentralItemType()));
    mapping1.setCentralItemType(newItemType);
    mapping1.setMaterialTypeId(newMaterialTypeId);

    repository.saveAndFlush(mapping1);

    var saved = repository.getOne(mapping1.getId());

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
  @Sql(scripts = {"classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/item-type-mapping/pre-populate-item-type-mapping.sql"})
  void shouldFindMappingByCentralItemType() {
    var centralServerId = fromString(PRE_POPULATED_CENTRAL_SERVER_ID);
    var mapping =
      repository.findByCentralServerIdAndCentralItemType(centralServerId, PRE_POPULATED_CENTRAL_ITEM_TYPE).get();

    assertEquals(PRE_POPULATED_CENTRAL_ITEM_TYPE, mapping.getCentralItemType());
    assertEquals(UUID.fromString(PRE_POPULATED_MATERIAL_TYPE_ID), mapping.getMaterialTypeId());
  }

  @Test
  @Sql(scripts = {"classpath:db/central-server/pre-populate-central-server.sql"})
  void throwExceptionWhenSavingWithoutCentralItemType() {
    var mapping = createItemTypeMapping();
    mapping.setCentralServer(TestUtil.refCentralServer(UUID.fromString(PRE_POPULATED_CENTRAL_SERVER_ID)));
    mapping.setCentralItemType(null);

    assertThrows(DataIntegrityViolationException.class, () -> repository.saveAndFlush(mapping));
  }

  @Test
  @Sql(scripts = {"classpath:db/central-server/pre-populate-central-server.sql"})
  void throwExceptionWhenSavingWithInvalidCentralServerReference() {
    var mapping = createItemTypeMapping();

    mapping.setCentralServer(TestUtil.refCentralServer(UUID.randomUUID()));

    var ex = assertThrows(DataIntegrityViolationException.class, () -> repository.saveAndFlush(mapping));
    assertThat(ex.getMessage(), containsString("constraint [fk_item_type_mapping_central_server]"));
  }

  @Test
  @Sql(scripts = {"classpath:db/central-server/pre-populate-central-server.sql"})
  void throwExceptionWhenSavingWithInvalidCentralItemType() {
    var mapping = createItemTypeMapping();
    mapping.setCentralServer(TestUtil.refCentralServer(UUID.fromString(PRE_POPULATED_CENTRAL_SERVER_ID)));

    mapping.setCentralItemType(256);

    assertThrows(DataIntegrityViolationException.class, () -> repository.saveAndFlush(mapping));
  }

  @Test
  @Sql(scripts = {"classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/item-type-mapping/pre-populate-item-type-mapping.sql"})
  void throwExceptionWhenSavingMappingWithItemTypeThatAlreadyExists() {
    var mapping = createItemTypeMapping();
    mapping.setCentralServer(TestUtil.refCentralServer(UUID.fromString(PRE_POPULATED_CENTRAL_SERVER_ID)));
    mapping.setCentralItemType(PRE_POPULATED_CENTRAL_ITEM_TYPE);

    var ex = assertThrows(DataIntegrityViolationException.class, () -> repository.saveAndFlush(mapping));
    assertThat(ex.getMessage(), containsString("constraint [unq_item_type_central_server]"));
  }
}
