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
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.AFTER_TEST_METHOD;
import static org.springframework.test.context.jdbc.SqlMergeMode.MergeMode.MERGE;

import static org.folio.innreach.fixture.TestUtil.deserializeFromJsonFile;

import jakarta.transaction.Transactional;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlConfig;
import org.springframework.test.context.jdbc.SqlMergeMode;

import org.folio.innreach.controller.base.BaseControllerTest;
import org.folio.innreach.dto.ContributionCriteriaDTO;
import org.folio.innreach.dto.Error;
import org.folio.innreach.mapper.ContributionCriteriaConfigurationMapper;
import org.folio.innreach.repository.ContributionCriteriaConfigurationRepository;

@Sql(
    scripts = {
        "classpath:db/contribution-criteria/clear-contribution-criteria-tables.sql",
        "classpath:db/central-server/clear-central-server-tables.sql"},
    executionPhase = AFTER_TEST_METHOD
)
@SqlConfig(transactionMode = SqlConfig.TransactionMode.ISOLATED)
@SqlMergeMode(MERGE)
@Transactional
class ContributionCriteriaControllerTest extends BaseControllerTest {

  private static final String PRE_POPULATED_CENTRAL_SERVER_ID = "edab6baf-c696-42b1-89bb-1bbb8759b0d2";
  private static final UUID PRE_POPULATED_CRITERIA_ID = fromString("71bd0beb-28cb-40bb-9f40-87463d61a553");

  @Autowired
  private TestRestTemplate testRestTemplate;
  @Autowired
  private ContributionCriteriaConfigurationRepository repository;
  @Autowired
  private ContributionCriteriaConfigurationMapper mapper;


  @Test
  @Sql(scripts = {
      "classpath:db/central-server/pre-populate-central-server.sql",
      "classpath:db/contribution-criteria/pre-populate-contribution-criteria.sql"
  })
  void shouldGetExistingCriteria() {
    var responseEntity = testRestTemplate.getForEntity(baseMappingURL(), ContributionCriteriaDTO.class);

    assertTrue(responseEntity.getStatusCode().is2xxSuccessful());
    assertTrue(responseEntity.hasBody());

    var response = responseEntity.getBody();
    assertNotNull(response);

    var dbCriteria = findCriteria();

    assertEquals(dbCriteria, response);
  }

  @Test
  void return404WhenCriteriaIsNotFoundByServerId() {
    var responseEntity = testRestTemplate.getForEntity(baseMappingURL(), ContributionCriteriaDTO.class);

    assertEquals(HttpStatus.NOT_FOUND, responseEntity.getStatusCode());
  }

  @Test
  @Sql(scripts = {
      "classpath:db/central-server/pre-populate-central-server.sql"
  })
  void shouldCreateNewCriteria() {
    var newCriteria = deserializeFromJsonFile("/contribution-criteria/create-contribution-configuration-request.json",
        ContributionCriteriaDTO.class);

    var responseEntity = testRestTemplate.postForEntity(baseMappingURL(), newCriteria, ContributionCriteriaDTO.class);

    assertTrue(responseEntity.getStatusCode().is2xxSuccessful());
    assertTrue(responseEntity.hasBody());

    var created = responseEntity.getBody();

    assertThat(created, samePropertyValuesAs(newCriteria, "id", "metadata"));
  }

  @Test
  @Sql(scripts = {
      "classpath:db/central-server/pre-populate-central-server.sql"
  })
  void shouldCreateNewCriteriaWithoutExcludedLocations() {
    var newCriteria = deserializeFromJsonFile(
        "/contribution-criteria/create-contribution-configuration-request-without-locations.json",
        ContributionCriteriaDTO.class);

    var responseEntity = testRestTemplate.postForEntity(baseMappingURL(), newCriteria, ContributionCriteriaDTO.class);

    assertTrue(responseEntity.getStatusCode().is2xxSuccessful());
    assertTrue(responseEntity.hasBody());

    var created = responseEntity.getBody();

    assertThat(created, samePropertyValuesAs(newCriteria, "id", "metadata"));
  }

  @Test
  @Sql(scripts = {
      "classpath:db/central-server/pre-populate-central-server.sql",
      "classpath:db/contribution-criteria/pre-populate-contribution-criteria.sql"
  })
  void return409WhenCriteriaAlreadyExists() {
    var newCriteria = deserializeFromJsonFile("/contribution-criteria/create-contribution-configuration-request.json",
        ContributionCriteriaDTO.class);

    var responseEntity = testRestTemplate.postForEntity(baseMappingURL(), newCriteria, Error.class);

    assertEquals(CONFLICT, responseEntity.getStatusCode());
    assertNotNull(responseEntity.getBody());
    assertThat(responseEntity.getBody().getMessage(), containsString("constraint [unq_contribution_criteria_server]"));
  }

  @Test
  @Sql(scripts = {
      "classpath:db/central-server/pre-populate-central-server.sql",
      "classpath:db/contribution-criteria/pre-populate-contribution-criteria.sql"
  })
  void shouldUpdateExistingCriteria() {
    var criteria = deserializeFromJsonFile("/contribution-criteria/update-contribution-configuration-request.json",
        ContributionCriteriaDTO.class);

    var responseEntity = testRestTemplate.exchange(baseMappingURL(), HttpMethod.PUT,
        new HttpEntity<>(criteria), ContributionCriteriaDTO.class);

    assertTrue(responseEntity.getStatusCode().is2xxSuccessful());

    var dbCriteria = findCriteria();
    assertThat(dbCriteria, samePropertyValuesAs(criteria, "locationIds", "metadata"));
    // compare separately due to different order of items
    assertThat(dbCriteria.getLocationIds(), containsInAnyOrder(criteria.getLocationIds().toArray()));
  }

  @Test
  @Sql(scripts = {
      "classpath:db/central-server/pre-populate-central-server.sql",
      "classpath:db/contribution-criteria/pre-populate-contribution-criteria.sql"
  })
  void shouldRemoveAllLocationIdsWhenUpdatingExistingCriteria() {
    var criteria = deserializeFromJsonFile("/contribution-criteria/update-contribution-configuration-request.json",
        ContributionCriteriaDTO.class);
    criteria.setLocationIds(null);

    var responseEntity = testRestTemplate.exchange(baseMappingURL(), HttpMethod.PUT,
        new HttpEntity<>(criteria), ContributionCriteriaDTO.class);

    assertTrue(responseEntity.getStatusCode().is2xxSuccessful());

    var dbCriteria = findCriteria();

    assertThat(dbCriteria.getLocationIds(), empty());
  }

  @Test
  @Sql(scripts = {
      "classpath:db/central-server/pre-populate-central-server.sql"
  })
  void return404IfCriteriaNotFoundWhenUpdating() {
    var criteria = deserializeFromJsonFile("/contribution-criteria/update-contribution-configuration-request.json",
        ContributionCriteriaDTO.class);

    var responseEntity = testRestTemplate.exchange(baseMappingURL(), HttpMethod.PUT,
        new HttpEntity<>(criteria), ContributionCriteriaDTO.class);

    assertEquals(HttpStatus.NOT_FOUND, responseEntity.getStatusCode());
  }

  @Test
  @Sql(scripts = {
      "classpath:db/central-server/pre-populate-central-server.sql",
      "classpath:db/contribution-criteria/pre-populate-contribution-criteria.sql"
  })
  void shouldDeleteExistingMapping() {
    var responseEntity = testRestTemplate.exchange(baseMappingURL(), HttpMethod.DELETE,
        HttpEntity.EMPTY, ContributionCriteriaDTO.class);

    assertEquals(HttpStatus.NO_CONTENT, responseEntity.getStatusCode());

    var deleted = repository.findById(PRE_POPULATED_CRITERIA_ID);
    assertTrue(deleted.isEmpty());
  }

  @Test
  @Sql(scripts = {
      "classpath:db/central-server/pre-populate-central-server.sql"
  })
  void return404IfCriteriaNotFoundWhenDeleting() {
    var responseEntity = testRestTemplate.exchange(baseMappingURL(), HttpMethod.DELETE,
        HttpEntity.EMPTY, ContributionCriteriaDTO.class);

    assertEquals(HttpStatus.NOT_FOUND, responseEntity.getStatusCode());
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
