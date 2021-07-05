package org.folio.innreach.controller;

import org.folio.innreach.controller.base.BaseControllerTest;
import org.folio.innreach.dto.ContributionCriteriaDTO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.folio.innreach.fixture.TestUtil.deserializeFromJsonFile;

class ContributionCriteriaControllerTest extends BaseControllerTest {

  private static final String PRE_POPULATED_CENTRAL_SERVER_ID = "f8723a94-25d5-4f19-9043-cc3c306d54a1";

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
  void return200HttpCode_on_createCriteriaConfiguration() {
    var contributionCriteriaDTO = deserializeFromJsonFile("/contribution-criteria/create-contribution-configuration-request.json", ContributionCriteriaDTO.class);
    var responseEntity = testRestTemplate.postForEntity("/inn-reach/central-servers/contribution-criteria/", contributionCriteriaDTO, ContributionCriteriaDTO.class);
    assertEquals(HttpStatus.CREATED, responseEntity.getStatusCode());
    assertTrue(responseEntity.hasBody());

    removeContributionCriteriaConfiguration();
  }

  @Test
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
  void return204HttpCode_when_deletableCriteriaConfiguration() {
    createContributionCriteriaConfigurationForTest();
    var responseEntity
      = testRestTemplate.exchange("/inn-reach/central-servers/{centralServerId}/contribution-criteria",
      HttpMethod.DELETE, HttpEntity.EMPTY, ContributionCriteriaDTO.class, PRE_POPULATED_CENTRAL_SERVER_ID);

    assertEquals(HttpStatus.NO_CONTENT, responseEntity.getStatusCode());

    removeContributionCriteriaConfiguration();
  }

  @Test
  void return200HttpCode_on_updateContributionCriteriaConfiguration() {
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
}
