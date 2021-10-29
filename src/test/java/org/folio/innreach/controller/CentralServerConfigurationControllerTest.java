package org.folio.innreach.controller;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.AFTER_TEST_METHOD;
import static org.springframework.test.context.jdbc.SqlMergeMode.MergeMode.MERGE;

import static org.folio.innreach.external.InnReachHeaders.X_TO_CODE;

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
class CentralServerConfigurationControllerTest extends BaseApiControllerTest {

  private static final String CS_TEST_CODE1 = "test1";
  private static final String CS_TEST_CODE2 = "test2";
  private static final String INNREACH_LOCALSERVERS_URL = "/innreach/v2/contribution/localservers";
  private static final String AGENCIES_URL = "/inn-reach/central-servers/agencies";
  private static final String INNREACH_ITEMTYPES_URL = "/innreach/v2/contribution/itemtypes";
  private static final String ITEMTYPES_URL = "/inn-reach/central-servers/item-types";
  private static final String INNREACH_PATRONTYPES_URL = "/innreach/v2/circ/patrontypes";
  private static final String PATRONTYPES_URL = "/inn-reach/central-servers/patron-types";

  @Autowired
  private CentralServerRepository repository;


  @Test
  void returnAgencyCodesForSingleCentralServer() throws Exception {
    var cs = createCentralServer(CS_TEST_CODE1);

    stubGet(INNREACH_LOCALSERVERS_URL, "agency-codes/d2ir-local-servers-response-01.json");

    getAndExpect(AGENCIES_URL,
        Template.of("central-server-agencies/cs-agencies-single-server-response.json", cs.getId(), CS_TEST_CODE1));
  }

  @Test
  void returnAgencyCodesForSeveralCentralServers() throws Exception {
    var cs1 = createCentralServer(CS_TEST_CODE1);
    var cs2 = createCentralServer(CS_TEST_CODE2);

    stubGet(INNREACH_LOCALSERVERS_URL, xToCodeHeader(CS_TEST_CODE1),
        "agency-codes/d2ir-local-servers-response-01.json");
    stubGet(INNREACH_LOCALSERVERS_URL, xToCodeHeader(CS_TEST_CODE2),
        "agency-codes/d2ir-local-servers-response-02.json");

    getAndExpect(AGENCIES_URL,
        Template.of("central-server-agencies/cs-agencies-two-server-response.json", cs1.getId(), CS_TEST_CODE1,
            cs2.getId(), CS_TEST_CODE2));
  }

  @Test
  void returnEmptyAgencyCodeListIfNoCentralServers() throws Exception {
    getAndExpect(AGENCIES_URL, Template.of("central-server-agencies/cs-agencies-empty-response.json"));
  }

  @Test
  void returnAgencyCodeListForOneServer_and_ignoreInvalidResponseForAnotherServers() throws Exception {
    var cs1 = createCentralServer(CS_TEST_CODE1);
    createCentralServer(CS_TEST_CODE2);

    stubGet(INNREACH_LOCALSERVERS_URL, xToCodeHeader(CS_TEST_CODE1),
        "agency-codes/d2ir-local-servers-response-01.json");
    stubGet(INNREACH_LOCALSERVERS_URL, xToCodeHeader(CS_TEST_CODE2),
        "agency-codes/d2ir-local-servers-broken-response.json");

    getAndExpect(AGENCIES_URL,
        Template.of("central-server-agencies/cs-agencies-single-server-response.json", cs1.getId(), CS_TEST_CODE1));
  }

  @Test
  void returnEmptyAgencyCodeList_when_requestIsNotAuthorized() throws Exception {
    createCentralServer(CS_TEST_CODE1);

    stubFor(WireMock.get(urlEqualTo(INNREACH_LOCALSERVERS_URL))
        .willReturn(aResponse()
            .withStatus(HttpStatus.UNAUTHORIZED.value())));

    getAndExpect(AGENCIES_URL, Template.of("central-server-agencies/cs-agencies-empty-response.json"));
  }

  @Test
  void returnEmptyAgencyCodeList_when_responseContainsErrors() throws Exception {
    createCentralServer(CS_TEST_CODE1);

    stubGet(INNREACH_LOCALSERVERS_URL, "error/d2ir-error-response.json");

    getAndExpect(AGENCIES_URL, Template.of("central-server-agencies/cs-agencies-empty-response.json"));
  }

  @Test
  void returnItemTypesForSingleCentralServer() throws Exception {
    var cs = createCentralServer(CS_TEST_CODE1);

    stubGet(INNREACH_ITEMTYPES_URL, "item-types/d2ir-item-types-response-01.json");

    getAndExpect(ITEMTYPES_URL,
        Template.of("central-item-types/cs-item-types-single-server-response.json", cs.getId(), CS_TEST_CODE1));
  }

  @Test
  void returnItemTypesForSeveralCentralServers() throws Exception {
    var cs1 = createCentralServer(CS_TEST_CODE1);
    var cs2 = createCentralServer(CS_TEST_CODE2);

    stubGet(INNREACH_ITEMTYPES_URL, xToCodeHeader(CS_TEST_CODE1),
        "item-types/d2ir-item-types-response-01.json");
    stubGet(INNREACH_ITEMTYPES_URL, xToCodeHeader(CS_TEST_CODE2),
        "item-types/d2ir-item-types-response-01.json");

    getAndExpect(ITEMTYPES_URL,
        Template.of("central-item-types/cs-item-types-two-server-response.json", cs1.getId(), CS_TEST_CODE1,
            cs2.getId(), CS_TEST_CODE2));
  }

  @Test
  void returnEmptyItemTypesListIfNoCentralServers() throws Exception {
    getAndExpect(ITEMTYPES_URL, Template.of("central-item-types/cs-item-types-empty-response.json"));
  }

  @Test
  void returnItemTypesListForOneServer_and_ignoreInvalidResponseForAnotherServers() throws Exception {
    var cs1 = createCentralServer(CS_TEST_CODE1);
    createCentralServer(CS_TEST_CODE2);

    stubGet(INNREACH_ITEMTYPES_URL, xToCodeHeader(CS_TEST_CODE1),
        "item-types/d2ir-item-types-response-01.json");
    stubGet(INNREACH_ITEMTYPES_URL, xToCodeHeader(CS_TEST_CODE2),
        "item-types/d2ir-item-types-broken-response.json");

    getAndExpect(ITEMTYPES_URL,
        Template.of("central-item-types/cs-item-types-single-server-response.json", cs1.getId(), CS_TEST_CODE1));
  }

  @Test
  void returnEmptyItemTypesList_when_requestIsNotAuthorized() throws Exception {
    createCentralServer(CS_TEST_CODE1);

    stubFor(WireMock.get(urlEqualTo(INNREACH_ITEMTYPES_URL))
        .willReturn(aResponse()
            .withStatus(HttpStatus.UNAUTHORIZED.value())));

    getAndExpect(ITEMTYPES_URL, Template.of("central-item-types/cs-item-types-empty-response.json"));
  }

  @Test
  void returnEmptyItemTypesList_when_responseContainsErrors() throws Exception {
    createCentralServer(CS_TEST_CODE1);

    stubGet(INNREACH_ITEMTYPES_URL, "error/d2ir-error-response.json");

    getAndExpect(ITEMTYPES_URL, Template.of("central-item-types/cs-item-types-empty-response.json"));
  }

  @Test
  void returnPatronTypesForSingleCentralServer() throws Exception {
    var cs = createCentralServer(CS_TEST_CODE1);

    stubGet(INNREACH_PATRONTYPES_URL, "patron-types/d2ir-patron-types-response-01.json");

    getAndExpect(PATRONTYPES_URL,
        Template.of("central-patron-types/cs-patron-types-single-server-response.json", cs.getId(), CS_TEST_CODE1));
  }

  @Test
  void returnPatronTypesForSeveralCentralServers() throws Exception {
    var cs1 = createCentralServer(CS_TEST_CODE1);
    var cs2 = createCentralServer(CS_TEST_CODE2);

    stubGet(INNREACH_PATRONTYPES_URL, xToCodeHeader(CS_TEST_CODE1),
        "patron-types/d2ir-patron-types-response-01.json");
    stubGet(INNREACH_PATRONTYPES_URL, xToCodeHeader(CS_TEST_CODE2),
        "patron-types/d2ir-patron-types-response-01.json");

    getAndExpect(PATRONTYPES_URL,
        Template.of("central-patron-types/cs-patron-types-two-server-response.json", cs1.getId(), CS_TEST_CODE1,
            cs2.getId(), CS_TEST_CODE2));
  }

  @Test
  void returnEmptyPatronTypesListIfNoCentralServers() throws Exception {
    getAndExpect(PATRONTYPES_URL, Template.of("central-patron-types/cs-patron-types-empty-response.json"));
  }

  @Test
  void returnPatronTypesListForOneServer_and_ignoreInvalidResponseForAnotherServers() throws Exception {
    var cs1 = createCentralServer(CS_TEST_CODE1);
    createCentralServer(CS_TEST_CODE2);

    stubGet(INNREACH_PATRONTYPES_URL, xToCodeHeader(CS_TEST_CODE1),
        "patron-types/d2ir-patron-types-response-01.json");
    stubGet(INNREACH_PATRONTYPES_URL, xToCodeHeader(CS_TEST_CODE2),
        "patron-types/d2ir-patron-types-broken-response.json");

    getAndExpect(PATRONTYPES_URL,
        Template.of("central-patron-types/cs-patron-types-single-server-response.json", cs1.getId(), CS_TEST_CODE1));
  }

  @Test
  void returnEmptyPatronTypesList_when_requestIsNotAuthorized() throws Exception {
    createCentralServer(CS_TEST_CODE1);

    stubFor(WireMock.get(urlEqualTo(INNREACH_PATRONTYPES_URL))
        .willReturn(aResponse()
            .withStatus(HttpStatus.UNAUTHORIZED.value())));

    getAndExpect(PATRONTYPES_URL, Template.of("central-patron-types/cs-patron-types-empty-response.json"));
  }

  @Test
  void returnEmptyPatronTypesList_when_responseContainsErrors() throws Exception {
    createCentralServer(CS_TEST_CODE1);

    stubGet(INNREACH_PATRONTYPES_URL, "error/d2ir-error-response.json");

    getAndExpect(PATRONTYPES_URL, Template.of("central-patron-types/cs-patron-types-empty-response.json"));
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

}
