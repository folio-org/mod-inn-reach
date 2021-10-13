package org.folio.innreach.repository;

import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import static org.folio.innreach.fixture.MappingFixture.createUserCustomFieldMapping;
import static org.folio.innreach.fixture.MappingFixture.refCentralServer;
import static org.folio.innreach.fixture.TestUtil.randomFiveCharacterCode;
import static org.folio.innreach.util.ListUtils.mapItems;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.jdbc.Sql;

import org.folio.innreach.domain.entity.UserCustomFieldMapping;
import org.folio.innreach.domain.entity.base.AuditableUser;
import org.folio.innreach.fixture.TestUtil;

class UserCustomFieldMappingRepositoryTest extends BaseRepositoryTest {

  private static final String PRE_POPULATED_USER_CUSTOM_FIELD_MAPPING_ID1 = "555392b2-9b33-4199-b5eb-73e842c9d5b0";
  private static final String PRE_POPULATED_USER_CUSTOM_FIELD_MAPPING_ID2 = "25a06994-c488-44a3-b481-ce3fe18b9238";

  private static final String PRE_POPULATED_CUSTOM_FIELD_ID = "43a175e3-d876-4235-8a51-56de9fce3247";

  private static final AuditableUser PRE_POPULATED_USER = AuditableUser.SYSTEM;
  private static final String PRE_POPULATED_CENTRAL_SERVER_ID = "edab6baf-c696-42b1-89bb-1bbb8759b0d2";

  @Autowired
  private UserCustomFieldMappingRepository repository;

  @Test
  @Sql(scripts = {"classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/central-server/pre-populate-another-central-server.sql",
    "classpath:db/user-custom-field-mapping/pre-populate-user-custom-field-mapping.sql",
    "classpath:db/user-custom-field-mapping/pre-populate-another-user-custom-field-mapping.sql"})
  void shouldFindAllExistingMappings() {
    var mappings = repository.findAll();

    assertEquals(2, mappings.size());

    List<String> ids = mapItems(mappings, mapping -> mapping.getId().toString());

    assertEquals(ids, List.of(PRE_POPULATED_USER_CUSTOM_FIELD_MAPPING_ID1, PRE_POPULATED_USER_CUSTOM_FIELD_MAPPING_ID2));
  }

  @Test
  @Sql(scripts = {"classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/user-custom-field-mapping/pre-populate-user-custom-field-mapping.sql"})
  void shouldGetMappingWithMetadata() {
    var mapping = repository.findOneByCentralServerId(UUID.fromString(PRE_POPULATED_CENTRAL_SERVER_ID)).get();

    assertNotNull(mapping);
    assertEquals(UUID.fromString(PRE_POPULATED_USER_CUSTOM_FIELD_MAPPING_ID1), mapping.getId());
    assertEquals(UUID.fromString(PRE_POPULATED_CUSTOM_FIELD_ID), mapping.getCustomFieldId());
    assertTrue(mapping.getConfiguredOptions().containsKey("qwerty"));
    assertEquals("5east", mapping.getConfiguredOptions().get("qwerty"));

    assertEquals(PRE_POPULATED_USER, mapping.getCreatedBy());
    assertNotNull(mapping.getCreatedDate());
  }

  @Test
  @Sql(scripts = {"classpath:db/central-server/pre-populate-central-server.sql"})
  void shouldSaveNewMapping() {
    var newMapping = createUserCustomFieldMapping();
    newMapping.setCentralServer(refCentralServer());

    var saved = repository.saveAndFlush(newMapping);

    assertNotNull(saved);
    assertEquals(newMapping.getId(), saved.getId());
    assertEquals(saved.getCustomFieldId(), saved.getCustomFieldId());
    assertEquals(saved.getConfiguredOptions(), saved.getConfiguredOptions());
  }

  @Test
  @Sql(scripts = {"classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/user-custom-field-mapping/pre-populate-user-custom-field-mapping.sql"})
  void shouldUpdateExistingMapping() {
    var mapping = repository.findOneByCentralServerId(UUID.fromString(PRE_POPULATED_CENTRAL_SERVER_ID)).get();

    var newCustomFieldId = randomUUID();
    var newCustomFieldValue = "newCustomFieldValue";
    var newAgencyCode = randomFiveCharacterCode();
    mapping.setCustomFieldId(newCustomFieldId);
    mapping.getConfiguredOptions().put(newCustomFieldValue, newAgencyCode);

    repository.saveAndFlush(mapping);

    var saved = repository.findOneByCentralServerId(UUID.fromString(PRE_POPULATED_CENTRAL_SERVER_ID)).get();

    assertEquals(newCustomFieldId, saved.getCustomFieldId());
    assertEquals(newAgencyCode, saved.getConfiguredOptions().get(newCustomFieldValue));
  }

  @Test
  @Sql(scripts = {"classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/user-custom-field-mapping/pre-populate-user-custom-field-mapping.sql"})
  void shouldDeleteExistingMapping() {
    UUID id = fromString(PRE_POPULATED_USER_CUSTOM_FIELD_MAPPING_ID1);

    repository.deleteById(id);

    Optional<UserCustomFieldMapping> deleted = repository.findById(id);
    assertTrue(deleted.isEmpty());
  }

  @Test
  @Sql(scripts = {"classpath:db/central-server/pre-populate-central-server.sql"})
  void throwExceptionWhenSavingWithoutCustomFieldValue() {
    var mapping = createUserCustomFieldMapping();
    mapping.getConfiguredOptions().put(null, "abc12");

    assertThrows(DataIntegrityViolationException.class, () -> repository.saveAndFlush(mapping));
  }

  @Test
  @Sql(scripts = {"classpath:db/central-server/pre-populate-central-server.sql"})
  void throwExceptionWhenSavingWithInvalidCentralServerReference() {
    var mapping = createUserCustomFieldMapping();

    mapping.setCentralServer(TestUtil.refCentralServer(randomUUID()));

    var ex = assertThrows(DataIntegrityViolationException.class, () -> repository.saveAndFlush(mapping));
    assertThat(ex.getMessage(), containsString("constraint [fk_user_custom_field_mapping_central_server]"));
  }

  @Test
  @Sql(scripts = {"classpath:db/central-server/pre-populate-central-server.sql"})
  void throwExceptionWhenSavingWithInvalidAgencyCode() {
    var mapping = createUserCustomFieldMapping();
    mapping.getConfiguredOptions().put("value1", "12345678");

    assertThrows(DataIntegrityViolationException.class, () -> repository.saveAndFlush(mapping));
  }
}
