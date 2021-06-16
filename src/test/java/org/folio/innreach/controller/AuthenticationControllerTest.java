package org.folio.innreach.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.AFTER_TEST_METHOD;

import static org.folio.innreach.fixture.TestUtil.deserializeFromJsonFile;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.jdbc.Sql;

import org.folio.innreach.controller.base.BaseControllerTest;
import org.folio.innreach.dto.AuthenticationRequest;

@Sql("classpath:db/central-server/pre-populate-central-server.sql")
@Sql(value = "classpath:db/central-server/clear-central-server-tables.sql",
    executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
class AuthenticationControllerTest extends BaseControllerTest {

  @Autowired
  private TestRestTemplate testRestTemplate;

  @Test
  void return200HttpCode_when_localServerCredentialsAreSuccessfullyAuthenticated() {
    var authenticationRequest = deserializeFromJsonFile("/authentication/authentication-request.json",
        AuthenticationRequest.class);

    var responseEntity = testRestTemplate.postForEntity("/inn-reach/authentication", authenticationRequest, Void.class);

    assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
  }

  @Test
  void return401HttpCode_when_localServerCredentialsAreNotAuthenticated() {
    var authenticationRequest = deserializeFromJsonFile("/authentication/bad-credentials-authentication-request.json",
        AuthenticationRequest.class);

    var responseEntity = testRestTemplate.postForEntity("/inn-reach/authentication", authenticationRequest, Void.class);

    assertEquals(HttpStatus.UNAUTHORIZED, responseEntity.getStatusCode());
  }
}
