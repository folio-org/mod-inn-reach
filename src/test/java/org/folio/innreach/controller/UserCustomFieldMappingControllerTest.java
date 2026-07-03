package org.folio.innreach.controller;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.AFTER_TEST_METHOD;
import static org.springframework.test.context.jdbc.SqlMergeMode.MergeMode.MERGE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import static org.folio.innreach.fixture.TestUtil.deserializeFromJsonFile;
import static org.folio.innreach.fixture.TestUtil.randomFiveCharacterCode;

import java.util.Optional;
import java.util.UUID;

import org.folio.innreach.domain.entity.UserCustomFieldMapping;
import org.folio.innreach.domain.listener.KafkaCirculationEventListener;
import org.folio.innreach.domain.listener.KafkaInitialContributionEventListener;
import org.folio.innreach.domain.listener.KafkaInventoryEventListener;
import org.folio.innreach.dto.Error;
import org.folio.innreach.dto.UserCustomFieldMappingDTO;
import org.folio.innreach.external.client.InnReachAuthClient;
import org.folio.innreach.external.dto.AccessTokenDTO;
import org.folio.innreach.it.base.BaseTenantIntegrationTest;
import org.folio.innreach.mapper.UserCustomFieldMappingMapper;
import org.folio.innreach.repository.UserCustomFieldMappingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlMergeMode;

@Sql(
  scripts = {
    "classpath:db/user-custom-field-mapping/clear-user-custom-field-mapping.sql",
    "classpath:db/central-server/clear-central-server-tables.sql"},
  executionPhase = AFTER_TEST_METHOD
)
@SqlMergeMode(MERGE)
class UserCustomFieldMappingControllerTest extends BaseTenantIntegrationTest {

  private static final String PRE_POPULATED_CENTRAL_SERVER_ID = "edab6baf-c696-42b1-89bb-1bbb8759b0d2";
  private static final String PRE_POPULATED_CUSTOM_FIELD_ID = "homeLibrary";

  @MockitoBean
  private KafkaCirculationEventListener kafkaCirculationEventListener;
  @MockitoBean
  private KafkaInventoryEventListener kafkaInventoryEventListener;
  @MockitoBean
  private KafkaInitialContributionEventListener kafkaInitialContributionEventListener;
  @MockitoBean
  private InnReachAuthClient innReachAuthClient;

  @Autowired
  private UserCustomFieldMappingRepository repository;
  @Autowired
  private UserCustomFieldMappingMapper mapper;

  @BeforeEach
  void init() {
    when(innReachAuthClient.getAccessToken(any(), any())).thenReturn(ResponseEntity.ok(new AccessTokenDTO()));
  }

  @Test
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/central-server/pre-populate-another-central-server.sql",
    "classpath:db/user-custom-field-mapping/pre-populate-user-custom-field-mapping.sql",
    "classpath:db/user-custom-field-mapping/pre-populate-another-user-custom-field-mapping.sql"
  })
  void shouldGetAllExistingMappingsForOneCustomField() throws Exception {
    var result = mockMvc.perform(get("/inn-reach/central-servers/{centralServerId}/user-custom-field-mappings",
        PRE_POPULATED_CENTRAL_SERVER_ID)
        .headers(defaultHeaders()))
      .andExpect(status().is2xxSuccessful())
      .andReturn();

    var response = fromJson(result, UserCustomFieldMappingDTO.class);
    assertNotNull(response);

    Optional<UserCustomFieldMapping> fromDb = repository.findOneByCentralServerId(UUID.fromString(PRE_POPULATED_CENTRAL_SERVER_ID));

    assertEquals(fromDb.get().getCustomFieldId(), response.getCustomFieldId());
    assertEquals(fromDb.get().getConfiguredOptions(), response.getConfiguredOptions());
  }

  @Test
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql"
  })
  void return200HttpCode_and_createdUserCustomFieldMapping_when_createUserCustomFieldMapping() throws Exception {
    var mappingDTO = deserializeFromJsonFile(
      "/user-custom-field-mapping/create-user-custom-field-mappings-request.json", UserCustomFieldMappingDTO.class);

    mockMvc.perform(post("/inn-reach/central-servers/{centralServerId}/user-custom-field-mappings",
        PRE_POPULATED_CENTRAL_SERVER_ID)
        .content(asJsonString(mappingDTO))
        .headers(defaultHeaders())
        .contentType(APPLICATION_JSON))
      .andExpect(status().isCreated());

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
  void shouldUpdateExistingMappingsForOneCentralServer() throws Exception {
    var existing = mapper.toDTO(repository.findOneByCentralServerId(UUID.fromString(PRE_POPULATED_CENTRAL_SERVER_ID)).get());
    existing.getConfiguredOptions().values().forEach(m -> randomFiveCharacterCode());
    existing.setCustomFieldId("testLib");

    mockMvc.perform(put("/inn-reach/central-servers/{centralServerId}/user-custom-field-mappings",
        PRE_POPULATED_CENTRAL_SERVER_ID)
        .content(asJsonString(existing))
        .headers(defaultHeaders())
        .contentType(APPLICATION_JSON))
      .andExpect(status().isNoContent());

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
  void return409WhenCreatingMappingAndCustomFieldIdAlreadyMapped() throws Exception {
    var newMapping = deserializeFromJsonFile("/user-custom-field-mapping/create-user-custom-field-mappings-request.json",
      UserCustomFieldMappingDTO.class);
    newMapping.setCustomFieldId(PRE_POPULATED_CUSTOM_FIELD_ID);

    var result = mockMvc.perform(post("/inn-reach/central-servers/{centralServerId}/user-custom-field-mappings",
        PRE_POPULATED_CENTRAL_SERVER_ID)
        .content(asJsonString(newMapping))
        .headers(defaultHeaders())
        .contentType(APPLICATION_JSON))
      .andExpect(status().isConflict())
      .andReturn();

    var error = fromJson(result, Error.class);
    assertNotNull(error);
    assertThat(error.getMessage(), containsString("constraint [unq_central_server]"));
  }

  @Test
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql"
  })
  void return409WhenCreatingMappingWithInvalidAgencyCode() throws Exception {
    var newMapping = deserializeFromJsonFile("/user-custom-field-mapping/create-user-custom-field-mappings-invalid-request.json",
      UserCustomFieldMappingDTO.class);

    mockMvc.perform(post("/inn-reach/central-servers/{centralServerId}/user-custom-field-mappings",
        PRE_POPULATED_CENTRAL_SERVER_ID)
        .content(asJsonString(newMapping))
        .headers(defaultHeaders())
        .contentType(APPLICATION_JSON))
      .andExpect(status().isConflict());
  }

  @Test
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql"
  })
  void return404HttpCodeWhenUserCustomFieldMappingNotFound() throws Exception {
    mockMvc.perform(get("/inn-reach/central-servers/{centralServerId}/user-custom-field-mappings",
        PRE_POPULATED_CENTRAL_SERVER_ID)
        .headers(defaultHeaders()))
      .andExpect(status().isNotFound());
  }
}
