package org.folio.innreach.controller;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.AFTER_TEST_METHOD;
import static org.springframework.test.context.jdbc.SqlMergeMode.MergeMode.MERGE;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlMergeMode;

import org.folio.innreach.controller.base.BaseControllerTest;
import org.folio.innreach.dto.CentralServerAgenciesDTO;
import org.folio.innreach.fixture.CentralServerCredentialsFixture;
import org.folio.innreach.fixture.CentralServerFixture;
import org.folio.innreach.fixture.LocalServerCredentialsFixture;
import org.folio.innreach.repository.CentralServerRepository;

@Sql(
    scripts = {
        "classpath:db/central-server/clear-central-server-tables.sql"},
    executionPhase = AFTER_TEST_METHOD
)
@SqlMergeMode(MERGE)
class CentralServerAgencyControllerTest extends BaseControllerTest {

  @Autowired
  private TestRestTemplate testRestTemplate;
  @Autowired
  private CentralServerRepository repository;


  @Test
  void returnAllAgencyCodesPerCentralServer() {
    var centralServer = CentralServerFixture.createCentralServer();

    centralServer.setCentralServerAddress(wm.getRuntimeInfo().getHttpBaseUrl());
    centralServer.setCentralServerCredentials(CentralServerCredentialsFixture.createCentralServerCredentials());
    centralServer.setLocalServerCredentials(LocalServerCredentialsFixture.createLocalServerCredentials());

    repository.save(centralServer);

    stubFor(post(urlEqualTo("/auth/v1/oauth2/token?grant_type=client_credentials&scope=innreach_tp"))
      .willReturn(aResponse()
          .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
          .withBodyFile("oauth/d2ir-oauth-token-response.json")));

    stubFor(get(urlEqualTo("/innreach/v2/contribution/localservers"))
      .willReturn(aResponse()
          .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
          .withBodyFile("agency-codes/d2ir-local-servers-response.json")));


    var responseEntity = testRestTemplate.getForEntity(
        "/inn-reach/central-servers/agencies", CentralServerAgenciesDTO.class);

    assertTrue(responseEntity.getStatusCode().is2xxSuccessful());
    assertTrue(responseEntity.hasBody());
  }

}
