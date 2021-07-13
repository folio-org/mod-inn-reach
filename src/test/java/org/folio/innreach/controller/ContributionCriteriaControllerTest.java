package org.folio.innreach.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import static org.folio.innreach.fixture.TestUtil.deserializeFromJsonFile;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.AFTER_TEST_METHOD;
import static org.springframework.test.context.jdbc.SqlMergeMode.MergeMode.MERGE;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;

import org.folio.innreach.controller.base.BaseControllerTest;
import org.folio.innreach.dto.ContributionCriteriaDTO;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlMergeMode;

@Sql(
  scripts = {
    "classpath:db/central-server/clear-central-server-tables.sql"},
  executionPhase = AFTER_TEST_METHOD
)
@SqlMergeMode(MERGE)
class ContributionCriteriaControllerTest extends BaseControllerTest {

  private static final String PRE_POPULATED_CENTRAL_SERVER_ID = "edab6baf-c696-42b1-89bb-1bbb8759b0d2";

  @Autowired
  private TestRestTemplate testRestTemplate;

  void createContributionCriteriaConfigurationForTest() {
    var contributionCriteriaDTO = deserializeFromJsonFile("/contribution-criteria/create-contribution-configuration-request.json", ContributionCriteriaDTO.class);
    var responseEntity = testRestTemplate.postForEntity("/inn-reach/central-servers/contribution-criteria/", contributionCriteriaDTO, ContributionCriteriaDTO.class);
    assertEquals(HttpStatus.CREATED, responseEntity.getStatusCode());
  }

  void removeContributionCriteriaConfiguration() {
    var responseEntity = testRestTemplate.exchange("/inn-reach/central-servers/{centralServerId}/contribution-criteria",
      HttpMethod.DELETE, HttpEntity.EMPTY, ContributionCriteriaDTO.class, PRE_POPULATED_CENTRAL_SERVER_ID);
  }

  @Test
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql"
  })
  void return201HttpCode_on_createCriteriaConfiguration() {
    var contributionCriteriaDTO = deserializeFromJsonFile("/contribution-criteria/create-contribution-configuration-request.json", ContributionCriteriaDTO.class);
    var responseEntity = testRestTemplate.postForEntity("/inn-reach/central-servers/contribution-criteria/", contributionCriteriaDTO, ContributionCriteriaDTO.class);
    assertEquals(HttpStatus.CREATED, responseEntity.getStatusCode());
    assertTrue(responseEntity.hasBody());

    removeContributionCriteriaConfiguration();
  }

  @Test
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql"
  })
  void return201HttpCode_on_createCriteriaConfigurationWithoutExcludedLocations() {
    var contributionCriteriaDTO = deserializeFromJsonFile("/contribution-criteria/create-contribution-configuration-request-without-locations.json", ContributionCriteriaDTO.class);
    var responseEntity = testRestTemplate.postForEntity("/inn-reach/central-servers/contribution-criteria/", contributionCriteriaDTO, ContributionCriteriaDTO.class);
    assertEquals(HttpStatus.CREATED, responseEntity.getStatusCode());
    assertTrue(responseEntity.hasBody());
    assertNull(responseEntity.getBody().getLocationIds());

    removeContributionCriteriaConfiguration();
  }

  @Test
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql"
  })
  void return500HttpCode_on_createCriteriaConfiguration_when_CriteriaConfigurationAlreadyExists() {
    createContributionCriteriaConfigurationForTest();

    var contributionCriteriaDTO
      = deserializeFromJsonFile("/contribution-criteria/create-contribution-configuration-request.json",
      ContributionCriteriaDTO.class);
    var responseEntity
      = testRestTemplate.postForEntity("/inn-reach/central-servers/contribution-criteria/",
      contributionCriteriaDTO, ContributionCriteriaDTO.class);

    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, responseEntity.getStatusCode());

    removeContributionCriteriaConfiguration();
  }

  @Test
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql"
  })
  void return500HttpCode_on_createCriteriaConfiguration_when_requestDataIsInvalid() {
    createContributionCriteriaConfigurationForTest();

    var contributionCriteriaDTO
      = deserializeFromJsonFile("/contribution-criteria/create-contribution-configuration-not-valid-request.json",
      ContributionCriteriaDTO.class);
    var responseEntity
      = testRestTemplate.postForEntity("/inn-reach/central-servers/contribution-criteria/",
      contributionCriteriaDTO, ContributionCriteriaDTO.class);

    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, responseEntity.getStatusCode());

    removeContributionCriteriaConfiguration();
  }

  @Test
  void return404HttpCode_when_deletableCriteriaConfiguration_DoesNotExist() {
    var responseEntity
      = testRestTemplate.exchange("/inn-reach/central-servers/{centralServerId}/contribution-criteria",
      HttpMethod.DELETE, HttpEntity.EMPTY, ContributionCriteriaDTO.class, PRE_POPULATED_CENTRAL_SERVER_ID);

    assertEquals(HttpStatus.NOT_FOUND, responseEntity.getStatusCode());
  }

  @Test
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql"
  })
  void return204HttpCode_when_deletableCriteriaConfiguration() {
    createContributionCriteriaConfigurationForTest();
    var responseEntity
      = testRestTemplate.exchange("/inn-reach/central-servers/{centralServerId}/contribution-criteria",
      HttpMethod.DELETE, HttpEntity.EMPTY, ContributionCriteriaDTO.class, PRE_POPULATED_CENTRAL_SERVER_ID);

    assertEquals(HttpStatus.NO_CONTENT, responseEntity.getStatusCode());

    removeContributionCriteriaConfiguration();
  }

  @Test
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql"
  })
  void return204HttpCode_on_updateContributionCriteriaConfiguration() {
    createContributionCriteriaConfigurationForTest();
    var contributionCriteriaDTO
      = deserializeFromJsonFile("/contribution-criteria/update-contribution-configuration-request.json",
      ContributionCriteriaDTO.class);
    var responseEntity
      = testRestTemplate.exchange("/inn-reach/central-servers/{centralServerId}/contribution-criteria",
      HttpMethod.PUT,new HttpEntity<>(contributionCriteriaDTO),ContributionCriteriaDTO.class ,PRE_POPULATED_CENTRAL_SERVER_ID);

    assertEquals(HttpStatus.NO_CONTENT,responseEntity.getStatusCode());
    removeContributionCriteriaConfiguration();
  }

  @Test
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql"
  })
  void return204HttpCode_on_updateContributionCriteriaConfigurationWithoutStatisticalCodeBehavior() {
    createContributionCriteriaConfigurationForTest();

    var contributionCriteriaDTO
      = deserializeFromJsonFile("/contribution-criteria/update-contribution-configuration-request-locations-only.json",
      ContributionCriteriaDTO.class);
    var responseEntity
      = testRestTemplate.exchange("/inn-reach/central-servers/{centralServerId}/contribution-criteria",
      HttpMethod.PUT,new HttpEntity<>(contributionCriteriaDTO),ContributionCriteriaDTO.class ,PRE_POPULATED_CENTRAL_SERVER_ID);

    assertEquals(HttpStatus.NO_CONTENT,responseEntity.getStatusCode());

    contributionCriteriaDTO
      = deserializeFromJsonFile("/contribution-criteria/update-contribution-configuration-request.json",
      ContributionCriteriaDTO.class);
    var responseEntity1
      = testRestTemplate.exchange("/inn-reach/central-servers/{centralServerId}/contribution-criteria",
      HttpMethod.PUT,new HttpEntity<>(contributionCriteriaDTO),ContributionCriteriaDTO.class ,PRE_POPULATED_CENTRAL_SERVER_ID);

    assertEquals(HttpStatus.NO_CONTENT,responseEntity1.getStatusCode());

    removeContributionCriteriaConfiguration();
  }
}
