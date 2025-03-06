package org.folio.innreach.repository;

import static java.util.UUID.fromString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import static org.folio.innreach.domain.entity.VisiblePatronFieldConfiguration.VisiblePatronField.EXTERNAL_SYSTEM_ID;
import static org.folio.innreach.domain.entity.VisiblePatronFieldConfiguration.VisiblePatronField.FOLIO_RECORD_NUMBER;
import static org.folio.innreach.domain.entity.VisiblePatronFieldConfiguration.VisiblePatronField.USERNAME;
import static org.folio.innreach.domain.entity.VisiblePatronFieldConfiguration.VisiblePatronField.USER_CUSTOM_FIELDS;
import static org.folio.innreach.fixture.MappingFixture.refCentralServer;
import static org.folio.innreach.fixture.VisiblePatronFieldConfigurationFixture.createVisiblePatronFieldConfiguration;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;

import org.folio.innreach.domain.entity.base.AuditableUser;


class VisiblePatronFieldConfigurationRepositoryTest extends BaseRepositoryTest {

  private static final String PRE_POPULATED_CENTRAL_SERVER_CODE = "d2ir";
  private static final String PRE_POPULATED_VISIBLE_PATRON_FIELD_CONFIGURATION_ID = "58173d4f-5dce-407a-8f63-80d1a0df3218";
  private static final AuditableUser PRE_POPULATED_USER = AuditableUser.SYSTEM;

  @Autowired
  private VisiblePatronFieldConfigurationRepository repository;

  @Test
  @Sql(scripts = {"classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/visible-fields/pre-populate-visible-patron-field-configuration.sql"
  })
  void shouldFindAllExistingConfigurations() {
    var configurations = repository.findAll();

    assertEquals(1, configurations.size());

    var configuration = configurations.get(0);
    var patronFields = configuration.getFields();

    assertEquals(3, patronFields.size());
    assertTrue(patronFields.containsAll(List.of(FOLIO_RECORD_NUMBER, USERNAME, USER_CUSTOM_FIELDS)));

    var customFields = configuration.getUserCustomFields();

    assertEquals(2, customFields.size());
    assertTrue(customFields.containsAll(List.of("field1", "field2")));
  }

  @Test
  @Sql(scripts = {"classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/visible-fields/pre-populate-visible-patron-field-configuration.sql"
  })
  void shouldGetConfigurationWithMetadata() {
    var configuration = repository.findByCentralServerCode(PRE_POPULATED_CENTRAL_SERVER_CODE).get();

    assertNotNull(configuration);
    assertEquals(UUID.fromString(PRE_POPULATED_VISIBLE_PATRON_FIELD_CONFIGURATION_ID), configuration.getId());

    assertEquals(PRE_POPULATED_USER, configuration.getCreatedBy());
    assertNotNull(configuration.getCreatedDate());
  }

  @Test
  @Sql(scripts = {"classpath:db/central-server/pre-populate-central-server.sql"})
  void shouldSaveNewConfiguration() {
    var newConfig = createVisiblePatronFieldConfiguration();
    newConfig.setCentralServer(refCentralServer());
    newConfig.setId(null);

    var savedConfig = repository.saveAndFlush(newConfig);

    assertNotNull(savedConfig);
    assertNotNull(savedConfig.getId());
    assertEquals(newConfig.getFields(), savedConfig.getFields());
    assertEquals(newConfig.getUserCustomFields(), savedConfig.getUserCustomFields());
  }

  @Test
  @Sql(scripts = {"classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/visible-fields/pre-populate-visible-patron-field-configuration.sql"
  })
  void shouldUpdateExistingConfiguration() {
    var configuration = repository.findByCentralServerCode(PRE_POPULATED_CENTRAL_SERVER_CODE).get();

    configuration.getFields().remove(USERNAME);
    configuration.getFields().add(EXTERNAL_SYSTEM_ID);
    configuration.getUserCustomFields().remove("field2");
    configuration.getUserCustomFields().add("field3");

    var newFields = configuration.getFields();
    var newCustomFields = configuration.getUserCustomFields();

    repository.saveAndFlush(configuration);

    var saved = repository.findByCentralServerCode(PRE_POPULATED_CENTRAL_SERVER_CODE).get();

    assertEquals(newFields, configuration.getFields());
    assertEquals(newCustomFields, configuration.getUserCustomFields());
  }

  @Test
  @Sql(scripts = {"classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/visible-fields/pre-populate-visible-patron-field-configuration.sql"
  })
  void shouldDeleteExistingMapping() {
    var id = fromString(PRE_POPULATED_VISIBLE_PATRON_FIELD_CONFIGURATION_ID);

    repository.deleteById(id);

    var deleted = repository.findById(id);
    assertTrue(deleted.isEmpty());
  }
}
