package org.folio.innreach.controller;

import org.folio.innreach.controller.base.BaseControllerTest;
import org.folio.innreach.domain.entity.UserCustomFieldMapping;
import org.folio.innreach.dto.Error;
import org.folio.innreach.dto.UserCustomFieldMappingDTO;
import org.folio.innreach.mapper.UserCustomFieldMappingMapper;
import org.folio.innreach.repository.UserCustomFieldMappingRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlMergeMode;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;

import static java.util.UUID.randomUUID;
import static org.folio.innreach.fixture.TestUtil.deserializeFromJsonFile;
import static org.folio.innreach.fixture.TestUtil.randomFiveCharacterCode;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.AFTER_TEST_METHOD;
import static org.springframework.test.context.jdbc.SqlMergeMode.MergeMode.MERGE;

@Sql(
  scripts = {
    "classpath:db/user-custom-field-mapping/clear-user-custom-field-mapping.sql",
    "classpath:db/central-server/clear-central-server-tables.sql"},
  executionPhase = AFTER_TEST_METHOD
)
@SqlMergeMode(MERGE)
class UserCustomFieldMappingControllerTest extends BaseControllerTest {

  private static final String PRE_POPULATED_CENTRAL_SERVER_ID = "edab6baf-c696-42b1-89bb-1bbb8759b0d2";
  private static final String PRE_POPULATED_CUSTOM_FIELD_ID = "43a175e3-d876-4235-8a51-56de9fce3247";

  @Autowired
  private TestRestTemplate testRestTemplate;
  @Autowired
  private UserCustomFieldMappingRepository repository;
  @Autowired
  private UserCustomFieldMappingMapper mapper;

  @Test
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/central-server/pre-populate-another-central-server.sql",
    "classpath:db/user-custom-field-mapping/pre-populate-user-custom-field-mapping.sql",
    "classpath:db/user-custom-field-mapping/pre-populate-another-user-custom-field-mapping.sql"
  })
  void shouldGetAllExistingMappingsForOneCustomField() {
    var responseEntity = testRestTemplate.getForEntity(
      "/inn-reach/central-servers/{centralServerId}/user-custom-field-mappings",
      UserCustomFieldMappingDTO.class, PRE_POPULATED_CENTRAL_SERVER_ID);

    assertTrue(responseEntity.getStatusCode().is2xxSuccessful());
    assertTrue(responseEntity.hasBody());

    var response = responseEntity.getBody();
    assertNotNull(response);

    Optional<UserCustomFieldMapping> fromDb = repository.findOneByCentralServerId(UUID.fromString(PRE_POPULATED_CENTRAL_SERVER_ID));

    assertEquals(fromDb.get().getCustomFieldId(), response.getCustomFieldId());
    assertEquals(fromDb.get().getConfiguredOptions(), response.getConfiguredOptions());
  }

  @Test
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql"
  })
  void return200HttpCode_and_createdUserCustomFieldMapping_when_createUserCustomFieldMapping() {
    var mappingDTO = deserializeFromJsonFile(
      "/user-custom-field-mapping/create-user-custom-field-mappings-request.json", UserCustomFieldMappingDTO.class);

    var responseEntity = testRestTemplate.postForEntity(
      "/inn-reach/central-servers/{centralServerId}/user-custom-field-mappings",
      mappingDTO, UserCustomFieldMappingDTO.class, PRE_POPULATED_CENTRAL_SERVER_ID);

    assertEquals(HttpStatus.CREATED, responseEntity.getStatusCode());
    assertTrue(responseEntity.hasBody());

    var created = repository.findOneByCentralServerId(UUID.fromString(PRE_POPULATED_CENTRAL_SERVER_ID));

    assertTrue(created.isPresent());
    var createdMapping = created.get();

    assertNotNull(createdMapping.getId());
    assertEquals(mappingDTO.getCustomFieldId(), createdMapping.getCustomFieldId());
    assertEquals(mappingDTO.getConfiguredOptions(), createdMapping.getConfiguredOptions());
  }

  @Test
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/user-custom-field-mapping/pre-populate-user-custom-field-mapping.sql"
  })
  void shouldUpdateExistingMappingsForOneCentralServer() {
    var existing = mapper.toDTO(repository.findOneByCentralServerId(UUID.fromString(PRE_POPULATED_CENTRAL_SERVER_ID)).get());
    existing.getConfiguredOptions().values().forEach(m -> randomFiveCharacterCode());
    existing.setCustomFieldId(randomUUID());

    var responseEntity = testRestTemplate.exchange(
      "/inn-reach/central-servers/{centralServerId}/user-custom-field-mappings",
      HttpMethod.PUT, new HttpEntity<>(existing), Void.class, PRE_POPULATED_CENTRAL_SERVER_ID);

    assertEquals(HttpStatus.NO_CONTENT, responseEntity.getStatusCode());
    assertFalse(responseEntity.hasBody());

    var updated = mapper.toDTO(repository.findOneByCentralServerId(
      UUID.fromString(PRE_POPULATED_CENTRAL_SERVER_ID)).get());

    assertEquals(existing.getCustomFieldId(), updated.getCustomFieldId());
    assertEquals(existing.getConfiguredOptions(), updated.getConfiguredOptions());
  }

  @Test
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/user-custom-field-mapping/pre-populate-user-custom-field-mapping.sql"
  })
  void return409WhenCreatingMappingAndCustomFieldIdAlreadyMapped() {
    var newMapping = deserializeFromJsonFile("/user-custom-field-mapping/create-user-custom-field-mappings-request.json",
      UserCustomFieldMappingDTO.class);
    newMapping.setCustomFieldId(UUID.fromString(PRE_POPULATED_CUSTOM_FIELD_ID));

    var responseEntity = testRestTemplate.postForEntity(
      "/inn-reach/central-servers/{centralServerId}/user-custom-field-mappings", newMapping,
      Error.class, PRE_POPULATED_CENTRAL_SERVER_ID);

    assertEquals(CONFLICT, responseEntity.getStatusCode());
    assertNotNull(responseEntity.getBody());
    assertThat(responseEntity.getBody().getMessage(), containsString("constraint [unq_central_server]"));
  }

  @Test
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql"
  })
  void return409WhenCreatingMappingWithInvalidAgencyCode() {
    var newMapping = deserializeFromJsonFile("/user-custom-field-mapping/create-user-custom-field-mappings-invalid-request.json",
      UserCustomFieldMappingDTO.class);

    var responseEntity = testRestTemplate.postForEntity(
      "/inn-reach/central-servers/{centralServerId}/user-custom-field-mappings", newMapping,
      Error.class, PRE_POPULATED_CENTRAL_SERVER_ID);

    assertEquals(CONFLICT, responseEntity.getStatusCode());
  }
}
