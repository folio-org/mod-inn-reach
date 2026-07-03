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
import org.folio.spring.integration.XOkapiHeaders;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;

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

  @BeforeAll
  static void setUpTenant() {
    createTopics(TENANT_TOPICS);
    setUpMockForTestTenantInit();
    enableTenant();
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
