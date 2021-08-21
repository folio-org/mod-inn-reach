package org.folio.innreach.repository;

import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import static org.folio.innreach.fixture.MappingFixture.createPatronTypeMapping;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.apache.commons.lang3.RandomUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.jdbc.Sql;

import org.folio.innreach.domain.entity.PatronTypeMapping;
import org.folio.innreach.fixture.TestUtil;

class PatronTypeMappingRepositoryTest extends BaseRepositoryTest {
  private static final String PRE_POPULATED_PATRON_TYPE_MAPPING_ID1 = "5c39c67f-1373-4ec9-b356-fb71aba3e659";
  private static final String PRE_POPULATED_PATRON_TYPE_MAPPING_ID2 = "1af0b16e-24bc-44cb-9c9a-ca07167e41d4";
  private static final String PRE_POPULATED_PATRON_TYPE_MAPPING_ID3 = "70649b94-da26-48fa-a2e8-a90dfb381027";
  private static final String PRE_POPULATED_PATRON_TYPE_MAPPING_ID4 = "97949544-e637-4671-acd6-a96847840c98";

  private static final String PRE_POPULATED_PATRON_GROUP_ID = "54e17c4c-e315-4d20-8879-efc694dea1ce";

  private static final String PRE_POPULATED_USER = "admin";

  @Autowired
  private PatronTypeMappingRepository repository;

  @Test
  @Sql(scripts = {"classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/central-server/pre-populate-another-central-server.sql",
    "classpath:db/patron-type-mapping/pre-populate-patron-type-mapping.sql",
    "classpath:db/patron-type-mapping/pre-populate-another-patron-type-mapping.sql"})
  void shouldFindAllExistingMappings() {
    var mappings = repository.findAll();

    assertEquals(4, mappings.size());

    List<String> ids = mappings.stream()
      .map(mapping -> mapping.getId().toString())
      .collect(toList());

    assertEquals(ids, List.of(PRE_POPULATED_PATRON_TYPE_MAPPING_ID1, PRE_POPULATED_PATRON_TYPE_MAPPING_ID2,
      PRE_POPULATED_PATRON_TYPE_MAPPING_ID3, PRE_POPULATED_PATRON_TYPE_MAPPING_ID4));
  }

  @Test
  @Sql(scripts = {"classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/patron-type-mapping/pre-populate-patron-type-mapping.sql"})
  void shouldGetMappingWithMetadata() {
    var mapping = repository.getOne(UUID.fromString(PRE_POPULATED_PATRON_TYPE_MAPPING_ID1));

    assertNotNull(mapping);
    assertEquals(UUID.fromString(PRE_POPULATED_PATRON_TYPE_MAPPING_ID1), mapping.getId());
    assertEquals(1, mapping.getPatronType());
    assertEquals(UUID.fromString(PRE_POPULATED_PATRON_GROUP_ID), mapping.getPatronGroupId());

    assertEquals(PRE_POPULATED_USER, mapping.getCreatedBy());
    assertNotNull(mapping.getCreatedDate());
    assertEquals(PRE_POPULATED_USER, mapping.getUpdatedBy());
    assertNotNull(mapping.getUpdatedDate());
  }

  @Test
  @Sql(scripts = {"classpath:db/central-server/pre-populate-central-server.sql"})
  void shouldSaveNewMapping() {
    var newMapping = createPatronTypeMapping();

    var saved = repository.saveAndFlush(newMapping);

    var found = repository.getOne(saved.getId());

    assertNotNull(found);
    assertEquals(newMapping.getId(), found.getId());
    assertEquals(saved.getPatronType(), found.getPatronType());
    assertEquals(saved.getPatronGroupId(), found.getPatronGroupId());
  }

  @Test
  @Sql(scripts = {"classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/patron-type-mapping/pre-populate-patron-type-mapping.sql"})
  void shouldUpdateExistingMapping() {
    var mapping = repository.getOne(fromString(PRE_POPULATED_PATRON_TYPE_MAPPING_ID1));

    UUID newPatronGroupId = randomUUID();
    int newPatronType = RandomUtils.nextInt(0, 256);
    mapping.setPatronGroupId(newPatronGroupId);
    mapping.setPatronType(newPatronType);

    repository.saveAndFlush(mapping);

    var saved = repository.getOne(mapping.getId());

    assertEquals(newPatronGroupId, saved.getPatronGroupId());
    assertEquals(newPatronType, saved.getPatronType());
  }

  @Test
  @Sql(scripts = {"classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/patron-type-mapping/pre-populate-patron-type-mapping.sql"})
  void shouldDeleteExistingMapping() {
    UUID id = fromString(PRE_POPULATED_PATRON_TYPE_MAPPING_ID1);

    repository.deleteById(id);

    Optional<PatronTypeMapping> deleted = repository.findById(id);
    assertTrue(deleted.isEmpty());
  }

  @Test
  @Sql(scripts = {"classpath:db/central-server/pre-populate-central-server.sql"})
  void throwExceptionWhenSavingWithoutPatronType() {
    var mapping = createPatronTypeMapping();
    mapping.setPatronType(null);

    assertThrows(DataIntegrityViolationException.class, () -> repository.saveAndFlush(mapping));
  }

  @Test
  @Sql(scripts = {"classpath:db/central-server/pre-populate-central-server.sql"})
  void throwExceptionWhenSavingWithInvalidCentralServerReference() {
    var mapping = createPatronTypeMapping();

    mapping.setCentralServer(TestUtil.refCentralServer(randomUUID()));

    var ex = assertThrows(DataIntegrityViolationException.class, () -> repository.saveAndFlush(mapping));
    assertThat(ex.getMessage(), containsString("constraint [fk_patron_type_mapping_central_server]"));
  }

  @Test
  @Sql(scripts = {"classpath:db/central-server/pre-populate-central-server.sql"})
  void throwExceptionWhenSavingWithInvalidPatronType() {
    var mapping = createPatronTypeMapping();
    mapping.setPatronType(256);

    assertThrows(DataIntegrityViolationException.class, () -> repository.saveAndFlush(mapping));
  }

  @Test
  @Sql(scripts = {"classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/patron-type-mapping/pre-populate-patron-type-mapping.sql"})
  void throwExceptionWhenSavingMappingWithGroupIdThatAlreadyExists() {
    var mapping = createPatronTypeMapping();

    mapping.setPatronGroupId(UUID.fromString(PRE_POPULATED_PATRON_GROUP_ID));

    var ex = assertThrows(DataIntegrityViolationException.class, () -> repository.saveAndFlush(mapping));
    assertThat(ex.getMessage(), containsString("constraint [unq_patron_group_id_central_server]"));
  }
}
