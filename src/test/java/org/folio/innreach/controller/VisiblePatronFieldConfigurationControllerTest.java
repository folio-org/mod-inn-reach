package org.folio.innreach.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.AFTER_TEST_METHOD;
import static org.springframework.test.context.jdbc.SqlMergeMode.MergeMode.MERGE;

import static org.folio.innreach.fixture.TestUtil.deserializeFromJsonFile;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlMergeMode;

import org.folio.innreach.controller.base.BaseControllerTest;
import org.folio.innreach.dto.VisiblePatronFieldConfigurationDTO;

@Sql(
  scripts = {
    "classpath:db/visible-fields/clear-visible-patron-field-configuration-tables.sql",
    "classpath:db/central-server/clear-central-server-tables.sql"
  },
  executionPhase = AFTER_TEST_METHOD
)
@SqlMergeMode(MERGE)
class VisiblePatronFieldConfigurationControllerTest extends BaseControllerTest {
  private static final String PRE_POPULATED_VISIBLE_PATRON_FIELD_CONFIG_ID = "58173d4f-5dce-407a-8f63-80d1a0df3218";
  private static final String PRE_POPULATED_CENTRAL_SERVER_ID = "edab6baf-c696-42b1-89bb-1bbb8759b0d2";

  @Autowired
  private TestRestTemplate testRestTemplate;

  @Test
  @Sql(scripts = {"classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/visible-fields/pre-populate-visible-patron-field-configuration.sql"
  })
  void return200HttpCode_and_visiblePatronFieldConfig_when_getForOneVisiblePatronFieldConfig() {
    var responseEntity = testRestTemplate.getForEntity(
      "/inn-reach/central-servers/{centralServerId}/visible-patron-field-configuration",
      VisiblePatronFieldConfigurationDTO.class, PRE_POPULATED_CENTRAL_SERVER_ID);

    assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
    assertTrue(responseEntity.hasBody());

    var fieldConfigDTO = responseEntity.getBody();

    assertEquals(UUID.fromString(PRE_POPULATED_VISIBLE_PATRON_FIELD_CONFIG_ID), fieldConfigDTO.getId());
    assertNotNull(fieldConfigDTO.getFields());
    assertNotNull(fieldConfigDTO.getUserCustomFields());
  }

  @Test
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql"
  })
  void return200HttpCode_and_createdVisiblePatronFieldConfig_when_createVisiblePatronFieldConfig() {
    var fieldConfigDTO = deserializeFromJsonFile(
      "/visible-fields/create-visible-patron-field-config-request.json", VisiblePatronFieldConfigurationDTO.class);

    var responseEntity = testRestTemplate.postForEntity(
      "/inn-reach/central-servers/{centralServerId}/visible-patron-field-configuration", fieldConfigDTO,
      VisiblePatronFieldConfigurationDTO.class, PRE_POPULATED_CENTRAL_SERVER_ID);

    assertEquals(HttpStatus.CREATED, responseEntity.getStatusCode());
    assertTrue(responseEntity.hasBody());

    var created = responseEntity.getBody();

    assertNotNull(created);
    assertNotNull(created.getId());
    assertTrue(created.getFields().containsAll(fieldConfigDTO.getFields()));
    assertEquals(fieldConfigDTO.getUserCustomFields(), created.getUserCustomFields());
  }

  @Test
  @Sql(scripts = {"classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/visible-fields/pre-populate-visible-patron-field-configuration.sql"
  })
  void return204HttpCode_when_updateVisiblePatronFieldConfig() {
    var fieldsConfigDTO = deserializeFromJsonFile(
      "/visible-fields/update-visible-patron-field-config-request.json", VisiblePatronFieldConfigurationDTO.class);

    var responseEntity = testRestTemplate.exchange(
      "/inn-reach/central-servers/{centralServerId}/visible-patron-field-configuration",
      HttpMethod.PUT, new HttpEntity<>(fieldsConfigDTO), VisiblePatronFieldConfigurationDTO.class,
      PRE_POPULATED_CENTRAL_SERVER_ID);

    assertEquals(HttpStatus.NO_CONTENT, responseEntity.getStatusCode());
  }

  @Test
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql"
  })
  void return404HttpCode_when_visiblePatronFieldConfigNotFound() {
    var responseEntity = testRestTemplate.getForEntity(
      "/inn-reach/central-servers/{centralServerId}/visible-patron-field-configuration",
      VisiblePatronFieldConfigurationDTO.class, PRE_POPULATED_CENTRAL_SERVER_ID);

    assertEquals(HttpStatus.NOT_FOUND, responseEntity.getStatusCode());
  }
}
