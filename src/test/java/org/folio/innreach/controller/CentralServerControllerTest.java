package org.folio.innreach.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.AFTER_TEST_METHOD;
import static org.springframework.test.context.jdbc.SqlMergeMode.MergeMode.MERGE;

import static org.folio.innreach.fixture.TestUtil.deserializeFromJsonFile;
import static org.folio.innreach.fixture.TestUtil.randomUUIDString;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlMergeMode;

import org.folio.innreach.controller.base.BaseControllerTest;
import org.folio.innreach.dto.CentralServerDTO;
import org.folio.innreach.dto.CentralServersDTO;

@Sql(
  scripts = {
    "classpath:db/central-server/clear-central-server-tables.sql"},
  executionPhase = AFTER_TEST_METHOD
)
@SqlMergeMode(MERGE)
class CentralServerControllerTest extends BaseControllerTest {

  private static final String PRE_POPULATED_CENTRAL_SERVER_ID = "edab6baf-c696-42b1-89bb-1bbb8759b0d2";

  @Autowired
  private TestRestTemplate testRestTemplate;

  @Test
  void return200HttpCode_and_createdCentralServerEntity_when_createCentralServer() {
    var centralServerRequestDTO = deserializeFromJsonFile(
      "/central-server/create-central-server-request.json", CentralServerDTO.class);

    var responseEntity = testRestTemplate.postForEntity(
      "/inn-reach/central-servers", centralServerRequestDTO, CentralServerDTO.class);

    assertTrue(responseEntity.getStatusCode().is2xxSuccessful());
    assertTrue(responseEntity.hasBody());

    var createdCentralServer = responseEntity.getBody();

    assertNotNull(createdCentralServer);
  }

  @Test
  void return200HttpCode_and_createdCentralServerEntity_when_createCentralServerWithoutLocalServerCredentials() {
    var centralServerRequestDTO = deserializeFromJsonFile(
      "/central-server/create-central-server-without-local-server-credentials-request.json", CentralServerDTO.class);

    var responseEntity = testRestTemplate.postForEntity(
      "/inn-reach/central-servers", centralServerRequestDTO, CentralServerDTO.class);

    assertTrue(responseEntity.getStatusCode().is2xxSuccessful());
    assertTrue(responseEntity.hasBody());

    var createdCentralServer = responseEntity.getBody();

    assertNotNull(createdCentralServer);
    assertNull(createdCentralServer.getLocalServerKey());
    assertNull(createdCentralServer.getLocalServerSecret());
  }

  @Test
  void return400HttpCode_when_requestDataIsInvalid() {
    var centralServerRequestDTO = deserializeFromJsonFile(
      "/central-server/create-central-server-invalid-request.json", CentralServerDTO.class);

    var responseEntity = testRestTemplate.postForEntity(
      "/inn-reach/central-servers", centralServerRequestDTO, CentralServerDTO.class);

    assertTrue(responseEntity.getStatusCode().is4xxClientError());
    assertTrue(responseEntity.hasBody());
  }

  @Test
  @Sql(scripts = "classpath:db/central-server/pre-populate-central-server.sql")
  void return200HttpCode_and_allCentralServerEntities_when_getForAllCentralServers() {
    var responseEntity = testRestTemplate.getForEntity(
      "/inn-reach/central-servers", CentralServersDTO.class);

    assertTrue(responseEntity.getStatusCode().is2xxSuccessful());
    assertTrue(responseEntity.hasBody());

    var centralServers = responseEntity.getBody();

    assertNotNull(centralServers);
    assertNotNull(centralServers.getCentralServers());
    assertEquals(1, centralServers.getCentralServers().size());
    assertEquals(1, centralServers.getTotalRecords());
  }

  @Test
  @Sql(scripts = "classpath:db/central-server/pre-populate-central-server.sql")
  void return200HttpCode_and_centralServerEntityById_when_getForOneCentralServer() {
    var responseEntity = testRestTemplate.getForEntity(
      "/inn-reach/central-servers/{centralServerId}", CentralServerDTO.class, PRE_POPULATED_CENTRAL_SERVER_ID);

    assertTrue(responseEntity.getStatusCode().is2xxSuccessful());
    assertTrue(responseEntity.hasBody());

    var centralServer = responseEntity.getBody();

    assertNotNull(centralServer);
  }

  @Test
  void return404HttpCode_when_centralServerByIdNotFound() {
    var responseEntity = testRestTemplate.getForEntity(
      "/inn-reach/central-servers/{centralServerId}", CentralServerDTO.class, randomUUIDString());

    assertEquals(HttpStatus.NOT_FOUND, responseEntity.getStatusCode());
  }

  @Test
  @Sql(scripts = "classpath:db/central-server/pre-populate-central-server.sql")
  void return200HttpCode_when_updateCentralServer() {
    var centralServerRequestDTO = deserializeFromJsonFile(
      "/central-server/update-central-server-request.json", CentralServerDTO.class);

    var responseEntity = testRestTemplate.exchange(
      "/inn-reach/central-servers/{centralServerId}", HttpMethod.PUT, new HttpEntity<>(centralServerRequestDTO),
      CentralServerDTO.class, PRE_POPULATED_CENTRAL_SERVER_ID);

    assertTrue(responseEntity.getStatusCode().is2xxSuccessful());
  }

  @Test
  void return404HttpCode_when_updatableCentralServerNotFound() {
    var centralServerRequestDTO = deserializeFromJsonFile(
      "/central-server/update-central-server-request.json", CentralServerDTO.class);

    var responseEntity = testRestTemplate.exchange(
      "/inn-reach/central-servers/{centralServerId}", HttpMethod.PUT, new HttpEntity<>(centralServerRequestDTO),
      CentralServerDTO.class, randomUUIDString());

    assertEquals(HttpStatus.NOT_FOUND, responseEntity.getStatusCode());
  }

  @Test
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql"
    })
  void return204HttpCode_when_deleteCentralServer() {
    var responseEntity = testRestTemplate.exchange(
      "/inn-reach/central-servers/{centralServerId}", HttpMethod.DELETE, HttpEntity.EMPTY,
      CentralServerDTO.class, PRE_POPULATED_CENTRAL_SERVER_ID);

    assertEquals(HttpStatus.NO_CONTENT, responseEntity.getStatusCode());
  }

  @Test
  void return404HttpCode_when_deletableCentralServerNotFound() {
    var responseEntity = testRestTemplate.exchange(
      "/inn-reach/central-servers/{centralServerId}", HttpMethod.DELETE, HttpEntity.EMPTY,
      CentralServerDTO.class, randomUUIDString());

    assertEquals(HttpStatus.NOT_FOUND, responseEntity.getStatusCode());
  }

  @Test
  @Sql(scripts = "classpath:db/central-server/pre-populate-central-server.sql")
  void return409HttpCode_when_createCentralServerWithUniqueViolation() {
    var centralServerRequestDTO = deserializeFromJsonFile(
      "/central-server/create-central-server-request.json", CentralServerDTO.class);

    var responseEntity = testRestTemplate.postForEntity(
      "/inn-reach/central-servers", centralServerRequestDTO, CentralServerDTO.class);

    assertEquals(HttpStatus.CONFLICT, responseEntity.getStatusCode());
  }

}
