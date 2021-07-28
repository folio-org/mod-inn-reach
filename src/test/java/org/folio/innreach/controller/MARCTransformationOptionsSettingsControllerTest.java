package org.folio.innreach.controller;

import org.folio.innreach.controller.base.BaseControllerTest;
import org.folio.innreach.dto.MARCTransformationOptionsSettingsDTO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlMergeMode;

import java.util.UUID;

import static org.folio.innreach.fixture.TestUtil.deserializeFromJsonFile;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.AFTER_TEST_METHOD;
import static org.springframework.test.context.jdbc.SqlMergeMode.MergeMode.MERGE;

@Sql(
  scripts = {
    "classpath:db/marc-transform-opt-set/clear-marc-transform-opt-set-tables.sql",
    "classpath:db/central-server/clear-central-server-tables.sql"
  },
  executionPhase = AFTER_TEST_METHOD
)
@SqlMergeMode(MERGE)
class MARCTransformationOptionsSettingsControllerTest extends BaseControllerTest {
  private static final String PRE_POPULATED_MARC_TRANSFORM_OPT_SET_ID = "51768f15-41e8-494d-bc4d-a308568e7052";
  private static final String PRE_POPULATED_CENTRAL_SERVER_ID = "edab6baf-c696-42b1-89bb-1bbb8759b0d2";

  @Autowired
  private TestRestTemplate testRestTemplate;

  @Test
  @Sql(scripts = {"classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/marc-transform-opt-set/pre-populate-marc-transform-opt-set.sql"
  })
  void return200HttpCode_and_marcTransformOptSet_when_getForOneMARCTransformOptSet() {
    var responseEntity = testRestTemplate.getForEntity(
      "/inn-reach/central-servers/{centralServerId}/marc-transformation-options", MARCTransformationOptionsSettingsDTO.class,
      PRE_POPULATED_CENTRAL_SERVER_ID);

    assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
    assertTrue(responseEntity.hasBody());

    var marcTransformOptSetDTO = responseEntity.getBody();

    assertEquals(UUID.fromString(PRE_POPULATED_MARC_TRANSFORM_OPT_SET_ID), marcTransformOptSetDTO.getId());
    assertNotNull(marcTransformOptSetDTO.getExcludedMARCFields());
    assertNotNull(marcTransformOptSetDTO.getConfigIsActive());
    assertNotNull(marcTransformOptSetDTO.getModifiedFieldsForContributedRecords());
  }

  @Test
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql"
  })
  void return200HttpCode_and_createdMARCTransformOptSetEntity_when_createMARCTransformOptSet() {
    var marcTransformOptSetDTO = deserializeFromJsonFile(
      "/marc-transform-opt-set/create-marc-transform-opt-set-request.json", MARCTransformationOptionsSettingsDTO.class);

    var responseEntity = testRestTemplate.postForEntity(
      "/inn-reach/central-servers/{centralServerId}/marc-transformation-options", marcTransformOptSetDTO, MARCTransformationOptionsSettingsDTO.class,
      PRE_POPULATED_CENTRAL_SERVER_ID);

    assertEquals(HttpStatus.CREATED, responseEntity.getStatusCode());
    assertTrue(responseEntity.hasBody());

    var created = responseEntity.getBody();

    assertNotNull(created);
    assertNotNull(created.getId());
    assertEquals(marcTransformOptSetDTO.getConfigIsActive(), created.getConfigIsActive());
    assertEquals(marcTransformOptSetDTO.getExcludedMARCFields(), created.getExcludedMARCFields());
    assertEquals(marcTransformOptSetDTO.getModifiedFieldsForContributedRecords().size(), created.getModifiedFieldsForContributedRecords().size());
  }

  @Test
  @Sql(scripts = {"classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/marc-transform-opt-set/pre-populate-marc-transform-opt-set.sql"
  })
  void return204HttpCode_when_updateMARCTransformOptSet() {
    var marcTransformOptSetDTO = deserializeFromJsonFile(
      "/marc-transform-opt-set/update-marc-transform-opt-set-request.json", MARCTransformationOptionsSettingsDTO.class);

    var responseEntity = testRestTemplate.exchange(
      "/inn-reach/central-servers/{centralServerId}/marc-transformation-options", HttpMethod.PUT, new HttpEntity<>(marcTransformOptSetDTO),
      MARCTransformationOptionsSettingsDTO.class, PRE_POPULATED_CENTRAL_SERVER_ID);

    assertEquals(HttpStatus.NO_CONTENT, responseEntity.getStatusCode());
  }

  @Test
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql"
  })
  void return404HttpCode_when_marcTransformOptSetByIdNotFound() {
    var responseEntity = testRestTemplate.getForEntity(
      "/inn-reach/central-servers/{centralServerId}/marc-transformation-options", MARCTransformationOptionsSettingsDTO.class,
      PRE_POPULATED_CENTRAL_SERVER_ID);

    assertEquals(HttpStatus.NOT_FOUND, responseEntity.getStatusCode());
  }
}
