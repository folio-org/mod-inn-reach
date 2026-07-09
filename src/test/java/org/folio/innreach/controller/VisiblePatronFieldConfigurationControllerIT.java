package org.folio.innreach.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.AFTER_TEST_METHOD;
import static org.springframework.test.context.jdbc.SqlMergeMode.MergeMode.MERGE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.folio.innreach.fixture.TestUtil.deserializeFromJsonFile;

import java.util.UUID;
import org.folio.innreach.dto.VisiblePatronFieldConfigurationDTO;
import org.folio.innreach.it.base.BaseTenantIT;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlMergeMode;

@Sql(
  scripts = {
    "classpath:db/visible-fields/clear-visible-patron-field-configuration-tables.sql",
    "classpath:db/central-server/clear-central-server-tables.sql"
  },
  executionPhase = AFTER_TEST_METHOD
)
@SqlMergeMode(MERGE)
class VisiblePatronFieldConfigurationControllerIT extends BaseTenantIT {
  private static final String PRE_POPULATED_VISIBLE_PATRON_FIELD_CONFIG_ID = "58173d4f-5dce-407a-8f63-80d1a0df3218";
  private static final String PRE_POPULATED_CENTRAL_SERVER_ID = "edab6baf-c696-42b1-89bb-1bbb8759b0d2";

  @Test
  @Sql(scripts = {"classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/visible-fields/pre-populate-visible-patron-field-configuration.sql"
  })
  void return200HttpCode_and_visiblePatronFieldConfig_when_getForOneVisiblePatronFieldConfig() throws Exception {
    var result = mockMvc.perform(get("/inn-reach/central-servers/{centralServerId}/visible-patron-field-configuration",
        PRE_POPULATED_CENTRAL_SERVER_ID)
        .headers(defaultHeaders()))
      .andExpect(status().isOk())
      .andReturn();

    var fieldConfigDTO = fromJson(result, VisiblePatronFieldConfigurationDTO.class);

    assertEquals(UUID.fromString(PRE_POPULATED_VISIBLE_PATRON_FIELD_CONFIG_ID), fieldConfigDTO.getId());
    assertNotNull(fieldConfigDTO.getFields());
    assertNotNull(fieldConfigDTO.getUserCustomFields());
  }

  @Test
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql"
  })
  void return200HttpCode_and_createdVisiblePatronFieldConfig_when_createVisiblePatronFieldConfig() throws Exception {
    var fieldConfigDTO = deserializeFromJsonFile(
      "/visible-fields/create-visible-patron-field-config-request.json", VisiblePatronFieldConfigurationDTO.class);

    var result = mockMvc.perform(post("/inn-reach/central-servers/{centralServerId}/visible-patron-field-configuration",
        PRE_POPULATED_CENTRAL_SERVER_ID)
        .content(asJsonString(fieldConfigDTO))
        .headers(defaultHeaders())
        .contentType(APPLICATION_JSON))
      .andExpect(status().isCreated())
      .andReturn();

    var created = fromJson(result, VisiblePatronFieldConfigurationDTO.class);

    assertNotNull(created);
    assertNotNull(created.getId());
    assertTrue(created.getFields().containsAll(fieldConfigDTO.getFields()));
    assertEquals(fieldConfigDTO.getUserCustomFields(), created.getUserCustomFields());
  }

  @Test
  @Sql(scripts = {"classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/visible-fields/pre-populate-visible-patron-field-configuration.sql"
  })
  void return204HttpCode_when_updateVisiblePatronFieldConfig() throws Exception {
    var fieldsConfigDTO = deserializeFromJsonFile(
      "/visible-fields/update-visible-patron-field-config-request.json", VisiblePatronFieldConfigurationDTO.class);

    mockMvc.perform(put("/inn-reach/central-servers/{centralServerId}/visible-patron-field-configuration",
        PRE_POPULATED_CENTRAL_SERVER_ID)
        .content(asJsonString(fieldsConfigDTO))
        .headers(defaultHeaders())
        .contentType(APPLICATION_JSON))
      .andExpect(status().isNoContent());
  }

  @Test
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql"
  })
  void return404HttpCode_when_visiblePatronFieldConfigNotFound() throws Exception {
    mockMvc.perform(get("/inn-reach/central-servers/{centralServerId}/visible-patron-field-configuration",
        PRE_POPULATED_CENTRAL_SERVER_ID)
        .headers(defaultHeaders()))
      .andExpect(status().isNotFound());
  }
}
