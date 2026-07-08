package org.folio.innreach.controller;

import static java.util.UUID.fromString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.samePropertyValuesAs;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
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

import jakarta.transaction.Transactional;
import java.util.UUID;
import org.folio.innreach.dto.ContributionCriteriaDTO;
import org.folio.innreach.dto.Error;
import org.folio.innreach.external.client.InnReachAuthClient;
import org.folio.innreach.external.dto.AccessTokenDTO;
import org.folio.innreach.it.base.BaseTenantIT;
import org.folio.innreach.mapper.ContributionCriteriaConfigurationMapper;
import org.folio.innreach.repository.ContributionCriteriaConfigurationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlConfig;
import org.springframework.test.context.jdbc.SqlMergeMode;

@Sql(
    scripts = {
        "classpath:db/contribution-criteria/clear-contribution-criteria-tables.sql",
        "classpath:db/central-server/clear-central-server-tables.sql"},
    executionPhase = AFTER_TEST_METHOD
)
@SqlConfig(transactionMode = SqlConfig.TransactionMode.ISOLATED)
@SqlMergeMode(MERGE)
class ContributionCriteriaControllerIT extends BaseTenantIT {

  private static final String PRE_POPULATED_CENTRAL_SERVER_ID = "edab6baf-c696-42b1-89bb-1bbb8759b0d2";
  private static final UUID PRE_POPULATED_CRITERIA_ID = fromString("71bd0beb-28cb-40bb-9f40-87463d61a553");

  @MockitoBean
  private InnReachAuthClient innReachAuthClient;

  @Autowired
  private ContributionCriteriaConfigurationRepository repository;
  @Autowired
  private ContributionCriteriaConfigurationMapper mapper;

  @BeforeEach
  void init() {
    when(innReachAuthClient.getAccessToken(any(), any())).thenReturn(ResponseEntity.ok(new AccessTokenDTO()));
  }

  @Test
  @Transactional
  @Sql(scripts = {
      "classpath:db/central-server/pre-populate-central-server.sql",
      "classpath:db/contribution-criteria/pre-populate-contribution-criteria.sql"
  })
  void shouldGetExistingCriteria() throws Exception {
    var result = mockMvc.perform(get(baseMappingURL())
        .headers(defaultHeaders()))
      .andExpect(status().is2xxSuccessful())
      .andReturn();

    var response = fromJson(result, ContributionCriteriaDTO.class);
    assertNotNull(response);

    var dbCriteria = findCriteria();

    assertEquals(dbCriteria, response);
  }

  @Test
  void return404WhenCriteriaIsNotFoundByServerId() throws Exception {
    mockMvc.perform(get(baseMappingURL())
        .headers(defaultHeaders()))
      .andExpect(status().isNotFound());
  }

  @Test
  @Sql(scripts = {
      "classpath:db/central-server/pre-populate-central-server.sql"
  })
  void shouldCreateNewCriteria() throws Exception {
    var newCriteria = deserializeFromJsonFile("/contribution-criteria/create-contribution-configuration-request.json",
        ContributionCriteriaDTO.class);

    var result = mockMvc.perform(post(baseMappingURL())
        .content(asJsonString(newCriteria))
        .headers(defaultHeaders())
        .contentType(APPLICATION_JSON))
      .andExpect(status().is2xxSuccessful())
      .andReturn();

    var created = fromJson(result, ContributionCriteriaDTO.class);

    assertThat(created, samePropertyValuesAs(newCriteria, "id", "metadata"));
  }

  @Test
  @Sql(scripts = {
      "classpath:db/central-server/pre-populate-central-server.sql"
  })
  void shouldCreateNewCriteriaWithoutExcludedLocations() throws Exception {
    var newCriteria = deserializeFromJsonFile(
        "/contribution-criteria/create-contribution-configuration-request-without-locations.json",
        ContributionCriteriaDTO.class);

    var result = mockMvc.perform(post(baseMappingURL())
        .content(asJsonString(newCriteria))
        .headers(defaultHeaders())
        .contentType(APPLICATION_JSON))
      .andExpect(status().is2xxSuccessful())
      .andReturn();

    var created = fromJson(result, ContributionCriteriaDTO.class);

    assertThat(created, samePropertyValuesAs(newCriteria, "id", "metadata"));
  }

  @Test
  @Sql(scripts = {
      "classpath:db/central-server/pre-populate-central-server.sql",
      "classpath:db/contribution-criteria/pre-populate-contribution-criteria.sql"
  })
  void return409WhenCriteriaAlreadyExists() throws Exception {
    var newCriteria = deserializeFromJsonFile("/contribution-criteria/create-contribution-configuration-request.json",
        ContributionCriteriaDTO.class);

    var result = mockMvc.perform(post(baseMappingURL())
        .content(asJsonString(newCriteria))
        .headers(defaultHeaders())
        .contentType(APPLICATION_JSON))
      .andExpect(status().isConflict())
      .andReturn();

    var error = fromJson(result, Error.class);
    assertNotNull(error);
    assertThat(error.getMessage(), containsString("constraint [unq_contribution_criteria_server]"));
  }

  @Test
  @Transactional
  @Sql(scripts = {
      "classpath:db/central-server/pre-populate-central-server.sql",
      "classpath:db/contribution-criteria/pre-populate-contribution-criteria.sql"
  })
  void shouldUpdateExistingCriteria() throws Exception {
    var criteria = deserializeFromJsonFile("/contribution-criteria/update-contribution-configuration-request.json",
        ContributionCriteriaDTO.class);

    mockMvc.perform(put(baseMappingURL())
        .content(asJsonString(criteria))
        .headers(defaultHeaders())
        .contentType(APPLICATION_JSON))
      .andExpect(status().is2xxSuccessful());

    var dbCriteria = findCriteria();
    assertThat(dbCriteria, samePropertyValuesAs(criteria, "locationIds", "metadata"));
    // compare separately due to different order of items
    assertThat(dbCriteria.getLocationIds(), containsInAnyOrder(criteria.getLocationIds().toArray()));
  }

  @Test
  @Transactional
  @Sql(scripts = {
      "classpath:db/central-server/pre-populate-central-server.sql",
      "classpath:db/contribution-criteria/pre-populate-contribution-criteria.sql"
  })
  void shouldRemoveAllLocationIdsWhenUpdatingExistingCriteria() throws Exception {
    var criteria = deserializeFromJsonFile("/contribution-criteria/update-contribution-configuration-request.json",
        ContributionCriteriaDTO.class);
    criteria.setLocationIds(null);

    mockMvc.perform(put(baseMappingURL())
        .content(asJsonString(criteria))
        .headers(defaultHeaders())
        .contentType(APPLICATION_JSON))
      .andExpect(status().is2xxSuccessful());

    var dbCriteria = findCriteria();

    assertThat(dbCriteria.getLocationIds(), empty());
  }

  @Test
  @Sql(scripts = {
      "classpath:db/central-server/pre-populate-central-server.sql"
  })
  void return404IfCriteriaNotFoundWhenUpdating() throws Exception {
    var criteria = deserializeFromJsonFile("/contribution-criteria/update-contribution-configuration-request.json",
        ContributionCriteriaDTO.class);

    mockMvc.perform(put(baseMappingURL())
        .content(asJsonString(criteria))
        .headers(defaultHeaders())
        .contentType(APPLICATION_JSON))
      .andExpect(status().isNotFound());
  }

  @Test
  @Sql(scripts = {
      "classpath:db/central-server/pre-populate-central-server.sql",
      "classpath:db/contribution-criteria/pre-populate-contribution-criteria.sql"
  })
  void shouldDeleteExistingMapping() throws Exception {
    mockMvc.perform(delete(baseMappingURL())
        .headers(defaultHeaders()))
      .andExpect(status().isNoContent());

    var deleted = repository.findById(PRE_POPULATED_CRITERIA_ID);
    assertTrue(deleted.isEmpty());
  }

  @Test
  @Sql(scripts = {
      "classpath:db/central-server/pre-populate-central-server.sql"
  })
  void return404IfCriteriaNotFoundWhenDeleting() throws Exception {
    mockMvc.perform(delete(baseMappingURL())
        .headers(defaultHeaders()))
      .andExpect(status().isNotFound());
  }

  private ContributionCriteriaDTO findCriteria() {
    return mapper.toDTO(repository.findById(PRE_POPULATED_CRITERIA_ID).get());
  }

  private static String baseMappingURL() {
    return baseMappingURL(PRE_POPULATED_CENTRAL_SERVER_ID);
  }

  private static String baseMappingURL(String serverId) {
    return "/inn-reach/central-servers/" + serverId + "/contribution-criteria";
  }

}
