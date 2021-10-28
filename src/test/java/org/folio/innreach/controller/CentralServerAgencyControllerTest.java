package org.folio.innreach.controller;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.AFTER_TEST_METHOD;
import static org.springframework.test.context.jdbc.SqlMergeMode.MergeMode.MERGE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import static org.folio.innreach.external.InnReachHeaders.X_TO_CODE;
import static org.folio.innreach.fixture.TestUtil.readFile;

import java.util.Map;

import com.github.tomakehurst.wiremock.client.WireMock;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlMergeMode;

import org.folio.innreach.controller.base.BaseApiControllerTest;
import org.folio.innreach.domain.entity.CentralServer;
import org.folio.innreach.fixture.CentralServerFixture;
import org.folio.innreach.repository.CentralServerRepository;

@Sql(
  scripts = {
    "classpath:db/central-server/clear-central-server-tables.sql"},
  executionPhase = AFTER_TEST_METHOD
)
@SqlMergeMode(MERGE)
public
class CentralServerAgencyControllerTest extends BaseApiControllerTest {

  private static final String CS_TEST_CODE1 = "test1";
  private static final String CS_TEST_CODE2 = "test2";
  public static final String INNREACH_LOCALSERVERS_URL = "/innreach/v2/contribution/localservers";
  private static final String AGENCIES_URL = "/inn-reach/central-servers/agencies";

  @Autowired
  private CentralServerRepository repository;


  @Test
  void returnAgencyCodesForSingleCentralServer() throws Exception {
    var cs = createCentralServer(CS_TEST_CODE1);

    stubGet(INNREACH_LOCALSERVERS_URL, "agency-codes/d2ir-local-servers-response-01.json");

    mockMvc.perform(get(AGENCIES_URL))
      .andExpect(status().isOk())
      .andExpect(content()
        .json(readTemplate("cs-agencies-single-server-response.json", cs.getId(), CS_TEST_CODE1)));
  }

  @Test
  void returnAgencyCodesForSeveralCentralServers() throws Exception {
    var cs1 = createCentralServer(CS_TEST_CODE1);
    var cs2 = createCentralServer(CS_TEST_CODE2);

    stubGet(INNREACH_LOCALSERVERS_URL, xToCodeHeader(CS_TEST_CODE1),
      "agency-codes/d2ir-local-servers-response-01.json");
    stubGet(INNREACH_LOCALSERVERS_URL, xToCodeHeader(CS_TEST_CODE2),
      "agency-codes/d2ir-local-servers-response-02.json");

    mockMvc.perform(get(AGENCIES_URL))
      .andExpect(status().isOk())
      .andExpect(content()
        .json(readTemplate("cs-agencies-two-server-response.json", cs1.getId(), CS_TEST_CODE1,
          cs2.getId(), CS_TEST_CODE2)));
  }

  @Test
  void returnEmptyAgencyCodeListIfNoCentralServers() throws Exception {
    mockMvc.perform(get(AGENCIES_URL))
      .andExpect(status().isOk())
      .andExpect(content().json(readTemplate("cs-agencies-empty-response.json")));
  }

  @Test
  void returnAgencyCodeListForOneServer_and_ignoreInvalidResponseForAnotherServers() throws Exception {
    var cs1 = createCentralServer(CS_TEST_CODE1);
    createCentralServer(CS_TEST_CODE2);

    stubGet(INNREACH_LOCALSERVERS_URL, xToCodeHeader(CS_TEST_CODE1),
      "agency-codes/d2ir-local-servers-response-01.json");
    stubGet(INNREACH_LOCALSERVERS_URL, xToCodeHeader(CS_TEST_CODE2),
      "agency-codes/d2ir-local-servers-broken-response.json");

    mockMvc.perform(get(AGENCIES_URL))
      .andExpect(status().isOk())
      .andExpect(content()
        .json(readTemplate("cs-agencies-single-server-response.json", cs1.getId(), CS_TEST_CODE1)));
  }

  @Test
  void returnEmptyAgencyCodeList_when_requestIsNotAuthorized() throws Exception {
    createCentralServer(CS_TEST_CODE1);

    stubFor(WireMock.get(urlEqualTo(INNREACH_LOCALSERVERS_URL))
      .willReturn(aResponse()
        .withStatus(HttpStatus.UNAUTHORIZED.value())));

    mockMvc.perform(get(AGENCIES_URL))
      .andExpect(status().isOk())
      .andExpect(content().json(readTemplate("cs-agencies-empty-response.json")));
  }

  @Test
  void returnEmptyAgencyCodeList_when_responseContainsErrors() throws Exception {
    createCentralServer(CS_TEST_CODE1);

    stubGet(INNREACH_LOCALSERVERS_URL, "agency-codes/d2ir-local-servers-error-response.json");

    mockMvc.perform(get(AGENCIES_URL))
      .andExpect(status().isOk())
      .andExpect(content().json(readTemplate("cs-agencies-empty-response.json")));
  }

  private CentralServer createCentralServer(String csCode) {
    var centralServer = CentralServerFixture.createCentralServer();

    centralServer.setCentralServerCode(csCode);
    centralServer.setCentralServerAddress(wm.baseUrl());

    return repository.save(centralServer);
  }

  private static Map<String, String> xToCodeHeader(String code) {
    return Map.of(X_TO_CODE, code);
  }

  private static String readTemplate(String templateFile, Object... params) {
    String path = "json/central-server-agencies/" + templateFile;

    return String.format(readFile(path), params);
  }

}
