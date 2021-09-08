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

import static org.folio.innreach.fixture.MappingFixture.createUserCustomFieldMapping;
import static org.folio.innreach.fixture.MappingFixture.refCentralServer;
import static org.folio.innreach.fixture.TestUtil.randomFiveCharacterCode;

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
  private static final String PRE_POPULATED_USER_CUSTOM_FIELD_MAPPING_ID2 = "b23ee9d7-7857-492b-bc89-dd9f37315555";
  private static final String PRE_POPULATED_USER_CUSTOM_FIELD_MAPPING_ID3 = "d9068b3e-7add-462c-a3af-4f793a0aef5c";
  private static final String PRE_POPULATED_USER_CUSTOM_FIELD_MAPPING_ID4 = "25a06994-c488-44a3-b481-ce3fe18b9238";
  private static final String PRE_POPULATED_USER_CUSTOM_FIELD_MAPPING_ID5 = "4cfd7596-fab9-43ab-b6e9-46de33ba3409";

  private static final String PRE_POPULATED_CUSTOM_FIELD_ID = "43a175e3-d876-4235-8a51-56de9fce3247";

  private static final AuditableUser PRE_POPULATED_USER = AuditableUser.SYSTEM;

  @Autowired
  private UserCustomFieldMappingRepository repository;

  @Test
  @Sql(scripts = {"classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/central-server/pre-populate-another-central-server.sql",
    "classpath:db/user-custom-field-mapping/pre-populate-user-custom-field-mapping.sql",
    "classpath:db/user-custom-field-mapping/pre-populate-another-user-custom-field-mapping.sql"})
  void shouldFindAllExistingMappings() {
    var mappings = repository.findAll();

    assertEquals(5, mappings.size());

    List<String> ids = mappings.stream()
      .map(mapping -> mapping.getId().toString())
      .collect(toList());

    assertEquals(ids, List.of(PRE_POPULATED_USER_CUSTOM_FIELD_MAPPING_ID1, PRE_POPULATED_USER_CUSTOM_FIELD_MAPPING_ID2,
      PRE_POPULATED_USER_CUSTOM_FIELD_MAPPING_ID3, PRE_POPULATED_USER_CUSTOM_FIELD_MAPPING_ID4,
      PRE_POPULATED_USER_CUSTOM_FIELD_MAPPING_ID5));
  }

  @Test
  @Sql(scripts = {"classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/user-custom-field-mapping/pre-populate-user-custom-field-mapping.sql"})
  void shouldGetMappingWithMetadata() {
    var mapping = repository.getOne(UUID.fromString(PRE_POPULATED_USER_CUSTOM_FIELD_MAPPING_ID1));

    assertNotNull(mapping);
    assertEquals(UUID.fromString(PRE_POPULATED_USER_CUSTOM_FIELD_MAPPING_ID1), mapping.getId());
    assertEquals(UUID.fromString(PRE_POPULATED_CUSTOM_FIELD_ID), mapping.getCustomFieldId());
    assertEquals("qwerty", mapping.getCustomFieldValue());
    assertEquals("5east", mapping.getAgencyCode());

    assertEquals(PRE_POPULATED_USER, mapping.getCreatedBy());
    assertNotNull(mapping.getCreatedDate());
  }

  @Test
  @Sql(scripts = {"classpath:db/central-server/pre-populate-central-server.sql"})
  void shouldSaveNewMapping() {
    var newMapping = createUserCustomFieldMapping();
    newMapping.setCentralServer(refCentralServer());

    var saved = repository.saveAndFlush(newMapping);

    var found = repository.getOne(saved.getId());

    assertNotNull(found);
    assertEquals(newMapping.getId(), found.getId());
    assertEquals(saved.getCustomFieldId(), found.getCustomFieldId());
    assertEquals(saved.getCustomFieldValue(), found.getCustomFieldValue());
    assertEquals(saved.getAgencyCode(), found.getAgencyCode());
  }

  @Test
  @Sql(scripts = {"classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/user-custom-field-mapping/pre-populate-user-custom-field-mapping.sql"})
  void shouldUpdateExistingMapping() {
    var mapping = repository.getOne(fromString(PRE_POPULATED_USER_CUSTOM_FIELD_MAPPING_ID1));

    var newCustomFieldId = randomUUID();
    var newCustomFieldValue = "qwert1";
    var newAgencyCode = randomFiveCharacterCode();
    mapping.setCustomFieldId(newCustomFieldId);
    mapping.setCustomFieldValue(newCustomFieldValue);
    mapping.setAgencyCode(newAgencyCode);

    repository.saveAndFlush(mapping);

    var saved = repository.getOne(mapping.getId());

    assertEquals(newCustomFieldId, saved.getCustomFieldId());
    assertEquals(newCustomFieldValue, saved.getCustomFieldValue());
    assertEquals(newAgencyCode, saved.getAgencyCode());
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
    mapping.setCustomFieldValue(null);

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
    mapping.setAgencyCode("123456");

    assertThrows(DataIntegrityViolationException.class, () -> repository.saveAndFlush(mapping));
  }

  @Test
  @Sql(scripts = {"classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/user-custom-field-mapping/pre-populate-user-custom-field-mapping.sql"})
  void throwExceptionWhenSavingMappingWithCustomFieldValueThatAlreadyExists() {
    var mapping = createUserCustomFieldMapping();

    mapping.setCustomFieldId(UUID.fromString(PRE_POPULATED_CUSTOM_FIELD_ID));
    mapping.setCustomFieldValue("qwerty");

    var ex = assertThrows(DataIntegrityViolationException.class, () -> repository.saveAndFlush(mapping));
    assertThat(ex.getMessage(), containsString("constraint [unq_custom_field_central_server]"));
  }
}
