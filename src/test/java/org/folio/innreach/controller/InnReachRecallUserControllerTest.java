package org.folio.innreach.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlMergeMode;

import org.folio.innreach.controller.base.BaseControllerTest;
import org.folio.innreach.dto.InnReachRecallUserDTO;
import org.folio.innreach.repository.CentralServerRepository;

@Sql(scripts = {
  "classpath:db/central-server/clear-central-server-tables.sql",
  "classpath:db/inn-reach-recall-user/clear-inn-reach-recall-user.sql"
}, executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
@SqlMergeMode(SqlMergeMode.MergeMode.MERGE)
class InnReachRecallUserControllerTest extends BaseControllerTest {

  @Autowired
  private CentralServerRepository centralServerRepository;

  @Autowired
  private TestRestTemplate testRestTemplate;

  @Test
  @Sql(scripts = {
    "classpath:db/inn-reach-recall-user/pre-populate-inn-reach-recall-user.sql",
    "classpath:db/central-server/pre-populate-central-server-with-recall-user.sql"
  })
  void returnRecallUser_when_centralUserRecallUserExists() {
    var responseEntity = testRestTemplate.getForEntity("/inn-reach/central-servers/{id}/inn-reach-recall-user",
        InnReachRecallUserDTO.class, UUID.fromString("edab6baf-c696-42b1-89bb-1bbb8759b0d2"));

    assertEquals(HttpStatus.OK, responseEntity.getStatusCode());

    var body = responseEntity.getBody();

    assertNotNull(body);
  }

  @Test
  @Sql(scripts = {
    "classpath:db/inn-reach-recall-user/pre-populate-inn-reach-recall-user.sql",
    "classpath:db/central-server/pre-populate-central-server-with-recall-user.sql"
  })
  void returnErrorMessage_when_centralServerRecallUserDoesNotExist() {
    var responseEntity = testRestTemplate.getForEntity("/inn-reach/central-servers/{id}/inn-reach-recall-user",
        InnReachRecallUserDTO.class, UUID.fromString("edab6baf-c697-42b2-89bb-1bbb8759b0d3"));

    assertEquals(HttpStatus.NOT_FOUND, responseEntity.getStatusCode());

    var body = responseEntity.getBody();

    assertNotNull(body);
  }

  @Test
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql"
  })
  void saveRecallUser() {
    var innReachRecallUserDTO = new InnReachRecallUserDTO();
    innReachRecallUserDTO.setUserId(UUID.randomUUID());

    var httpEntity = new HttpEntity<>(innReachRecallUserDTO);

    var responseEntity = testRestTemplate.postForEntity("/inn-reach/central-servers/{id}/inn-reach-recall-user",
        httpEntity, InnReachRecallUserDTO.class, UUID.fromString("edab6baf-c696-42b1-89bb-1bbb8759b0d2"));

    assertEquals(HttpStatus.OK, responseEntity.getStatusCode());

    var body = responseEntity.getBody();

    assertNotNull(body);

    var centralServer = centralServerRepository.fetchRecallUser(UUID.fromString("edab6baf-c696-42b1-89bb-1bbb8759b0d2")).orElseThrow();
    var innReachRecallUser = centralServer.getInnReachRecallUser();

    assertNotNull(innReachRecallUser);
    assertEquals(body.getUserId(), innReachRecallUser.getUserId());
  }

  @Test
  @Sql(scripts = {
    "classpath:db/inn-reach-recall-user/pre-populate-inn-reach-recall-user.sql",
    "classpath:db/central-server/pre-populate-central-server-with-recall-user.sql"
  })
  void updateRecallUser() {
    var innReachRecallUserDTO = new InnReachRecallUserDTO();
    innReachRecallUserDTO.setUserId(UUID.randomUUID());

    var httpEntity = new HttpEntity<>(innReachRecallUserDTO);

    var responseEntity = testRestTemplate.exchange("/inn-reach/central-servers/{id}/inn-reach-recall-user",
      HttpMethod.PUT, httpEntity, Void.class, UUID.fromString("edab6baf-c696-42b1-89bb-1bbb8759b0d2"));

    assertEquals(HttpStatus.NO_CONTENT, responseEntity.getStatusCode());

    var centralServer = centralServerRepository.fetchRecallUser(UUID.fromString("edab6baf-c696-42b1-89bb-1bbb8759b0d2")).orElseThrow();
    var innReachRecallUser = centralServer.getInnReachRecallUser();

    assertNotNull(innReachRecallUser);
    assertEquals(innReachRecallUserDTO.getUserId(), innReachRecallUser.getUserId());
  }




}
