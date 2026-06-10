package org.folio.innreach.controller;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.AFTER_TEST_METHOD;
import static org.springframework.test.context.jdbc.SqlMergeMode.MergeMode.MERGE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;
import java.util.UUID;

import org.folio.innreach.batch.contribution.IterationEventReaderFactory;
import org.folio.innreach.batch.contribution.service.ContributionJobRunner;
import org.folio.innreach.client.InstanceStorageClient;
import org.folio.innreach.controller.base.BaseApiControllerTest;
import org.folio.innreach.dto.ContributionDTO;
import org.folio.innreach.dto.MappingValidationStatusDTO;
import org.folio.innreach.repository.ContributionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlMergeMode;

@Sql(
  scripts = {
    "classpath:db/contribution/clear-contribution-tables.sql",
    "classpath:db/mtype-mapping/clear-material-type-mapping-table.sql",
    "classpath:db/inn-reach-location/clear-inn-reach-location-tables.sql",
    "classpath:db/lib-mapping/clear-library-mapping-table.sql",
    "classpath:db/central-server/clear-central-server-tables.sql"
  },
  executionPhase = AFTER_TEST_METHOD
)
@SqlMergeMode(MERGE)
@ExtendWith(MockitoExtension.class)
class ContributionControllerIT extends BaseApiControllerTest {

  private static final String CURRENT_CONTRIBUTION_URL =
    "/inn-reach/central-servers/{centralServerId}/contributions/current";

  private static final UUID PRE_POPULATED_CENTRAL_SERVER_ID =
    UUID.fromString("edab6baf-c696-42b1-89bb-1bbb8759b0d2");

  private static final String INNREACH_LOCATIONS_URL = "/innreach/v2/contribution/locations";
  private static final String MATERIAL_TYPES_URL = "/material-types";

  @MockitoBean
  private InstanceStorageClient instanceStorageClient;

  @MockitoBean
  private ContributionJobRunner contributionJobRunner;

  @MockitoBean
  private IterationEventReaderFactory iterationEventReaderFactory;

  @Autowired
  private ContributionRepository contributionRepository;

  @Autowired
  private JdbcTemplate jdbcTemplate;

  @BeforeEach
  void setup() {
    wm.resetAll();
    jdbcTemplate.update(
      "UPDATE central_server SET central_server_address = ? WHERE id = ?",
      wm.baseUrl(), PRE_POPULATED_CENTRAL_SERVER_ID
    );
  }

  @Test
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/mtype-mapping/pre-populate-material-type-mapping.sql",
    "classpath:db/inn-reach-location/pre-populate-inn-reach-location-code.sql",
    "classpath:db/lib-mapping/pre-populate-another-library-mapping.sql",
    "classpath:db/contribution/pre-populate-contribution.sql"
  })
  void getCurrentContribution_returnsInProgressContributionWithValidMappingStatuses() throws Exception {
    stubInnReachLocations();
    stubGet(MATERIAL_TYPES_URL, "inventory-storage/material-types-response.json",
      Map.of("query", "cql.allRecords=1", "limit", "2000"));

    var result = mockMvc.perform(get(CURRENT_CONTRIBUTION_URL, PRE_POPULATED_CENTRAL_SERVER_ID)
        .headers(getOkapiHeaders()))
      .andExpect(status().isOk())
      .andReturn();

    var body = result.getResponse().getContentAsString();
    assertNotNull(body);

    var existing = contributionRepository.fetchCurrentByCentralServerId(PRE_POPULATED_CENTRAL_SERVER_ID);
    assertTrue(existing.isPresent());

    mockMvc.perform(get(CURRENT_CONTRIBUTION_URL, PRE_POPULATED_CENTRAL_SERVER_ID)
        .headers(getOkapiHeaders()))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.locationsMappingStatus").value(MappingValidationStatusDTO.VALID.getValue()))
      .andExpect(jsonPath("$.itemTypeMappingStatus").value(MappingValidationStatusDTO.VALID.getValue()))
      .andExpect(jsonPath("$.status").value(ContributionDTO.StatusEnum.IN_PROGRESS.getValue()));
  }

  @Test
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/mtype-mapping/pre-populate-material-type-mapping.sql",
    "classpath:db/inn-reach-location/pre-populate-inn-reach-location-code.sql",
    "classpath:db/lib-mapping/pre-populate-another-library-mapping.sql"
  })
  void getCurrentContribution_returnsNotStartedStatusWhenNoContributionExists() throws Exception {
    stubInnReachLocations();
    stubGet(MATERIAL_TYPES_URL, "inventory-storage/material-types-response.json",
      Map.of("query", "cql.allRecords=1", "limit", "2000"));

    mockMvc.perform(get(CURRENT_CONTRIBUTION_URL, PRE_POPULATED_CENTRAL_SERVER_ID)
        .headers(getOkapiHeaders()))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.status").value(ContributionDTO.StatusEnum.NOT_STARTED.getValue()))
      .andExpect(jsonPath("$.locationsMappingStatus").value(MappingValidationStatusDTO.VALID.getValue()))
      .andExpect(jsonPath("$.itemTypeMappingStatus").value(MappingValidationStatusDTO.VALID.getValue()));
  }

  @Test
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/inn-reach-location/pre-populate-inn-reach-location-code.sql",
    "classpath:db/lib-mapping/pre-populate-another-library-mapping.sql"
  })
  void getCurrentContribution_returnsInvalidItemTypeMappingStatus_whenMaterialTypeMappingsAreMissing() throws Exception {
    stubInnReachLocations();
    stubGet(MATERIAL_TYPES_URL, "inventory-storage/material-types-response.json",
      Map.of("query", "cql.allRecords=1", "limit", "2000"));

    mockMvc.perform(get(CURRENT_CONTRIBUTION_URL, PRE_POPULATED_CENTRAL_SERVER_ID)
        .headers(getOkapiHeaders()))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.itemTypeMappingStatus").value(MappingValidationStatusDTO.INVALID.getValue()))
      .andExpect(jsonPath("$.locationsMappingStatus").value(MappingValidationStatusDTO.VALID.getValue()));
  }

  @Test
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/mtype-mapping/pre-populate-material-type-mapping.sql",
    "classpath:db/inn-reach-location/pre-populate-inn-reach-location-code.sql"
  })
  void getCurrentContribution_returnsInvalidLocationMappingStatus_whenLibraryMappingsAreMissing() throws Exception {
    stubInnReachLocations();
    stubGet(MATERIAL_TYPES_URL, "inventory-storage/material-types-response.json",
      Map.of("query", "cql.allRecords=1", "limit", "2000"));

    mockMvc.perform(get(CURRENT_CONTRIBUTION_URL, PRE_POPULATED_CENTRAL_SERVER_ID)
        .headers(getOkapiHeaders()))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.itemTypeMappingStatus").value(MappingValidationStatusDTO.VALID.getValue()))
      .andExpect(jsonPath("$.locationsMappingStatus").value(MappingValidationStatusDTO.INVALID.getValue()));
  }

  @Test
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/mtype-mapping/pre-populate-material-type-mapping.sql",
    "classpath:db/inn-reach-location/pre-populate-inn-reach-location-code.sql",
    "classpath:db/lib-mapping/pre-populate-another-library-mapping.sql"
  })
  void getCurrentContribution_returnsBothMappingStatusesInvalid_whenInnReachLocationCallFails() throws Exception {
    // Location endpoint not stubbed — InnReach location fetch will fail with a connection error.
    // ContributionServiceImpl.getCurrent catches the exception and sets both statuses to INVALID.
    stubGet(MATERIAL_TYPES_URL, "inventory-storage/material-types-response.json",
      Map.of("query", "cql.allRecords=1", "limit", "2000"));

    mockMvc.perform(get(CURRENT_CONTRIBUTION_URL, PRE_POPULATED_CENTRAL_SERVER_ID)
        .headers(getOkapiHeaders()))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.locationsMappingStatus").value(MappingValidationStatusDTO.INVALID.getValue()))
      .andExpect(jsonPath("$.itemTypeMappingStatus").value(MappingValidationStatusDTO.INVALID.getValue()));
  }


  private static void stubInnReachLocations() {
    stubGet(INNREACH_LOCATIONS_URL, "innreach/locations-response.json",
      resp -> resp.withHeader("Content-Type", "text/json;charset=utf-8"));
  }
}

