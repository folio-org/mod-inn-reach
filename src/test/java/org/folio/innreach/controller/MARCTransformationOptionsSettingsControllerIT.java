package org.folio.innreach.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.AFTER_TEST_METHOD;
import static org.springframework.test.context.jdbc.SqlMergeMode.MergeMode.MERGE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.folio.innreach.fixture.TestUtil.deserializeFromJsonFile;

import java.util.UUID;
import org.folio.innreach.dto.MARCTransformationOptionsSettingsDTO;
import org.folio.innreach.dto.MARCTransformationOptionsSettingsListDTO;
import org.folio.innreach.external.client.InnReachAuthClient;
import org.folio.innreach.external.dto.AccessTokenDTO;
import org.folio.innreach.it.base.BaseTenantIT;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlMergeMode;

@Sql(
  scripts = {
    "classpath:db/marc-transform-opt-set/clear-marc-transform-opt-set-tables.sql",
    "classpath:db/central-server/clear-central-server-tables.sql"
  },
  executionPhase = AFTER_TEST_METHOD
)
@SqlMergeMode(MERGE)
class MARCTransformationOptionsSettingsControllerIT extends BaseTenantIT {
  private static final String PRE_POPULATED_MARC_TRANSFORM_OPT_SET_ID = "51768f15-41e8-494d-bc4d-a308568e7052";
  private static final String PRE_POPULATED_CENTRAL_SERVER_ID = "edab6baf-c696-42b1-89bb-1bbb8759b0d2";

  @MockitoBean
  private InnReachAuthClient innReachAuthClient;

  @BeforeEach
  void init() {
    when(innReachAuthClient.getAccessToken(any(), any())).thenReturn(ResponseEntity.ok(new AccessTokenDTO()));
  }

  @Test
  @Sql(scripts = {"classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/marc-transform-opt-set/pre-populate-marc-transform-opt-set.sql"
  })
  void return200HttpCode_and_marcTransformOptSet_when_getForOneMARCTransformOptSet() throws Exception {
    var result = mockMvc.perform(get("/inn-reach/central-servers/{centralServerId}/marc-transformation-options",
        PRE_POPULATED_CENTRAL_SERVER_ID)
        .headers(defaultHeaders()))
      .andExpect(status().isOk())
      .andReturn();

    var marcTransformOptSetDTO = fromJson(result, MARCTransformationOptionsSettingsDTO.class);

    assertEquals(UUID.fromString(PRE_POPULATED_MARC_TRANSFORM_OPT_SET_ID), marcTransformOptSetDTO.getId());
    assertNotNull(marcTransformOptSetDTO.getExcludedMARCFields());
    assertNotNull(marcTransformOptSetDTO.getConfigIsActive());
    assertNotNull(marcTransformOptSetDTO.getModifiedFieldsForContributedRecords());
  }

  @Test
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/marc-transform-opt-set/pre-populate-marc-transform-opt-set.sql"
  })
  void return200HttpCode_and_allMARCTransformOptSetEntities_when_getForAllMARCTransformOptSet() throws Exception {
    var result = mockMvc.perform(get("/inn-reach/central-servers/marc-transformation-options")
        .headers(defaultHeaders()))
      .andExpect(status().is2xxSuccessful())
      .andReturn();

    var marcTransformOptSetList = fromJson(result, MARCTransformationOptionsSettingsListDTO.class);

    assertNotNull(marcTransformOptSetList);
    assertNotNull(marcTransformOptSetList.getMaRCTransformOptSetList());
    assertEquals(1, marcTransformOptSetList.getMaRCTransformOptSetList().size());
    assertEquals(1, marcTransformOptSetList.getTotalRecords());
  }

  @Test
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql"
  })
  void return200HttpCode_and_createdMARCTransformOptSetEntity_when_createMARCTransformOptSet() throws Exception {
    var marcTransformOptSetDTO = deserializeFromJsonFile(
      "/marc-transform-opt-set/create-marc-transform-opt-set-request.json", MARCTransformationOptionsSettingsDTO.class);

    var result = mockMvc.perform(post("/inn-reach/central-servers/{centralServerId}/marc-transformation-options",
        PRE_POPULATED_CENTRAL_SERVER_ID)
        .content(asJsonString(marcTransformOptSetDTO))
        .headers(defaultHeaders())
        .contentType(APPLICATION_JSON))
      .andExpect(status().isCreated())
      .andReturn();

    var created = fromJson(result, MARCTransformationOptionsSettingsDTO.class);

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
  void return204HttpCode_when_updateMARCTransformOptSet() throws Exception {
    var marcTransformOptSetDTO = deserializeFromJsonFile(
      "/marc-transform-opt-set/update-marc-transform-opt-set-request.json", MARCTransformationOptionsSettingsDTO.class);

    mockMvc.perform(put("/inn-reach/central-servers/{centralServerId}/marc-transformation-options",
        PRE_POPULATED_CENTRAL_SERVER_ID)
        .content(asJsonString(marcTransformOptSetDTO))
        .headers(defaultHeaders())
        .contentType(APPLICATION_JSON))
      .andExpect(status().isNoContent());
  }

  @Test
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql"
  })
  void return404HttpCode_when_marcTransformOptSetByIdNotFound() throws Exception {
    mockMvc.perform(get("/inn-reach/central-servers/{centralServerId}/marc-transformation-options",
        PRE_POPULATED_CENTRAL_SERVER_ID)
        .headers(defaultHeaders()))
      .andExpect(status().isNotFound());
  }

  @Test
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/marc-transform-opt-set/pre-populate-marc-transform-opt-set.sql"
  })
  void return204HttpCode_when_deleteMARCTransformOptSet() throws Exception {
    mockMvc.perform(delete("/inn-reach/central-servers/{centralServerId}/marc-transformation-options",
        PRE_POPULATED_CENTRAL_SERVER_ID)
        .headers(defaultHeaders()))
      .andExpect(status().isNoContent());
  }
}
