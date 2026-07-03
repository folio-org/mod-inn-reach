package org.folio.innreach.it.base;

import static com.github.tomakehurst.wiremock.client.WireMock.created;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.noContent;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.awaitility.Durations.ONE_HUNDRED_MILLISECONDS;
import static org.awaitility.Durations.ONE_MINUTE;
import static org.folio.innreach.fixture.TestUtil.circHeaders;
import static org.folio.innreach.fixture.TestUtil.readFile;
import static org.folio.innreach.support.kafka.KafkaContainerExtension.createTopics;
import static org.folio.innreach.support.kafka.KafkaContainerExtension.deleteTopics;
import static org.folio.innreach.support.wiremock.WiremockContainerExtension.getWireMockClient;
import static org.folio.innreach.support.wiremock.WiremockStubExtension.resetWiremockStubs;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;

import com.github.tomakehurst.wiremock.client.MappingBuilder;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.matching.UrlPattern;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.awaitility.Awaitility;
import org.awaitility.core.ThrowingRunnable;
import org.folio.innreach.util.JsonHelper;
import org.folio.spring.integration.XOkapiHeaders;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.ResultActions;

import javax.sql.DataSource;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public abstract class BaseTenantIntegrationTest extends BaseIntegrationTest {

  protected static final List<String> TENANT_TOPICS = List.of(
    "folio.testing.circulation.loan",
    "folio.testing.circulation.request",
    "folio.testing.circulation.check-in",
    "folio.testing.inventory.item",
    "folio.testing.inventory.holdings-record",
    "folio.testing.inventory.instance",
    "folio.testing.inventory.instance-contribution"
  );

  protected static WireMock wiremock;

  private static JsonHelper jsonHelper;

  private static final String TENANT_SCHEMA = TEST_TENANT + "_mod_inn_reach";

  @BeforeAll
  static void setUpTenant(@Autowired JsonHelper jh, @Autowired DataSource dataSource) {
    jsonHelper = jh;
    createTopics(TENANT_TOPICS);
    setUpMockForTestTenantInit();
    enableTenant();
    configureSearchPath(dataSource);
    resetWiremockStubs();
    wiremock = getWireMockClient();
  }

  @AfterAll
  static void tearDownTenant() {
    resetWiremockStubs();
    purgeTenant();
    deleteTopics(TENANT_TOPICS);
    wiremock = null;
  }

  /**
   * Adds WireMock stubs required for tenant initialization.
   * Override or extend as needed for stubs required by reference data loading,
   * system user creation, etc.
   */
  protected static void setUpMockForTestTenantInit() {
    // Add WireMock stubs needed during POST /_/tenant here.
    // For example, stubs for reference-data endpoints that the app calls during init.
  }

  /**
   * Configures HikariCP to set search_path on every new connection to include
   * the tenant schema. This ensures @Sql scripts and JPA queries target the
   * correct schema created by enableTenant().
   */
  private static void configureSearchPath(DataSource dataSource) {
    if (dataSource instanceof HikariDataSource hds) {
      hds.setConnectionInitSql(
        "SET timezone = 'UTC'; SET search_path TO " + TENANT_SCHEMA + ", public");
      // Evict existing connections so they pick up the new init SQL
      var pool = hds.getHikariPoolMXBean();
      if (pool != null) {
        pool.softEvictConnections();
      }
    }
  }

  // --- WireMock helpers migrated from BaseApiControllerTest ---

  public static HttpHeaders getOkapiHeaders() {
    HttpHeaders headers = circHeaders();
    headers.add(XOkapiHeaders.URL, getWiremockUrl());
    return headers;
  }

  public static void awaitUntilAsserted(ThrowingRunnable runnable) {
    Awaitility.await()
      .atMost(ONE_MINUTE)
      .pollInterval(ONE_HUNDRED_MILLISECONDS)
      .untilAsserted(runnable);
  }

  public static void awaitAssertion(ThrowingRunnable runnable) {
    awaitUntilAsserted(runnable);
  }

  protected void getAndExpect(String url, Template expectedResult) throws Exception {
    mockMvc.perform(get(url))
      .andExpect(status().isOk())
      .andExpect(content()
        .json(readTemplate(expectedResult)));
  }

  protected void putAndExpect(URI uri, Object requestBody, Template expectedResult) throws Exception {
    putReq(uri, requestBody)
      .andExpect(status().isOk())
      .andExpect(content().json(
        readTemplate(expectedResult)));
  }

  protected ResultActions putReq(URI uri, Object requestBody) throws Exception {
    return mockMvc.perform(put(uri.getUrlTemplate(), uri.getUriVars())
      .content(jsonHelper.toJson(requestBody))
      .contentType(MediaType.APPLICATION_JSON)
      .headers(getOkapiHeaders()));
  }

  protected static void stubGet(String url, String responsePath) {
    stubGet(url, Collections.emptyMap(), responsePath);
  }

  protected static void stubGet(String path, String responsePath,
                                Map<String, String> queryParamAndValues) {
    stubGet(urlPathEqualTo(path), responsePath, ResponseActions.none(), mappingBuilder -> {
      queryParamAndValues.forEach((param, value) ->
        mappingBuilder.withQueryParam(param, equalTo(value)));
      return mappingBuilder;
    });
  }

  protected static void stubGet(String url, Map<String, String> requestHeaders,
                                String responsePath) {
    stubGet(url, requestHeaders, responsePath, ResponseActions.none());
  }

  protected static void stubGet(String url, String responsePath,
                                ResponseActions additionalResponseActions) {
    stubGet(url, Collections.emptyMap(), responsePath, additionalResponseActions);
  }

  protected static void stubGet(String url, Map<String, String> requestHeaders,
                                String responsePath,
                                ResponseActions additionalResponseActions) {
    stubGet(url, responsePath, additionalResponseActions, mappingBuilder -> {
      requestHeaders.forEach((name, value) ->
        mappingBuilder.withHeader(name, equalTo(value)));
      return mappingBuilder;
    });
  }

  protected static void stubGet(String url, String responsePath,
                                ResponseActions additionalResponseActions,
                                MappingActions additionalMapping) {
    stubGet(urlEqualTo(url), responsePath, additionalResponseActions, additionalMapping);
  }

  protected static void stubGet(UrlPattern pattern, String responsePath,
                                ResponseActions additionalResponseActions,
                                MappingActions additionalMapping) {
    ResponseDefinitionBuilder responseBuilder = ok()
      .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
      .withHeader(XOkapiHeaders.URL, getWiremockUrl())
      .withBodyFile(responsePath);

    responseBuilder = additionalResponseActions.apply(responseBuilder);
    MappingBuilder mappingBuilder = WireMock.get(pattern).willReturn(responseBuilder);
    mappingBuilder = additionalMapping.apply(mappingBuilder);

    stubFor(mappingBuilder);
  }

  protected static void stubPost(String url, String responsePath) {
    stubPost(url, responsePath, ResponseActions.none(), MappingActions.none());
  }

  protected static void stubPost(String url, String responsePath,
                                 ResponseActions additionalResponseActions,
                                 MappingActions additionalMapping) {
    ResponseDefinitionBuilder responseBuilder = created()
      .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
      .withHeader(XOkapiHeaders.URL, getWiremockUrl())
      .withBodyFile(responsePath);

    responseBuilder = additionalResponseActions.apply(responseBuilder);
    MappingBuilder mappingBuilder = WireMock.post(urlEqualTo(url)).willReturn(responseBuilder);
    mappingBuilder = additionalMapping.apply(mappingBuilder);

    stubFor(mappingBuilder);
  }

  protected static void stubPut(String url) {
    stubPut(url, ResponseActions.none(), MappingActions.none());
  }

  protected static void stubPut(String url,
                                ResponseActions additionalResponseActions,
                                MappingActions additionalMapping) {
    ResponseDefinitionBuilder responseDefBuilder = additionalResponseActions.apply(noContent());
    MappingBuilder mappingBuilder = WireMock.put(urlEqualTo(url)).willReturn(responseDefBuilder);
    mappingBuilder = additionalMapping.apply(mappingBuilder);

    stubFor(mappingBuilder);
  }

  protected static void stubDelete(String url) {
    stubDelete(url, ResponseActions.none(), MappingActions.none());
  }

  protected static void stubDelete(String url,
                                   ResponseActions additionalResponseActions,
                                   MappingActions additionalMapping) {
    ResponseDefinitionBuilder responseDefBuilder = additionalResponseActions.apply(noContent());
    MappingBuilder mappingBuilder = WireMock.delete(urlEqualTo(url));
    mappingBuilder = additionalMapping.apply(mappingBuilder);

    stubFor(mappingBuilder);
  }

  protected static String readTemplate(Template template) {
    String path = "json/" + template.getFile();
    return String.format(readFile(path), template.getParams());
  }

  // --- Inner types migrated from BaseApiControllerTest ---

  @RequiredArgsConstructor(staticName = "of")
  @AllArgsConstructor(access = AccessLevel.PRIVATE)
  @Getter
  protected static class Template {
    final String file;
    Object[] params;

    public static Template of(String file, Object... params) {
      return new Template(file, params);
    }
  }

  @RequiredArgsConstructor(staticName = "of")
  @AllArgsConstructor(access = AccessLevel.PRIVATE)
  @Getter
  protected static class URI {
    final String urlTemplate;
    Object[] uriVars;

    public static URI of(String urlTemplate, Object... uriVars) {
      return new URI(urlTemplate, uriVars);
    }
  }

  protected interface MappingActions extends UnaryOperator<MappingBuilder> {
    static MappingActions none() {
      return t -> t;
    }
  }

  protected interface ResponseActions extends UnaryOperator<ResponseDefinitionBuilder> {
    static ResponseActions none() {
      return t -> t;
    }
  }
}
