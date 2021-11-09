package org.folio.innreach.controller.base;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import static org.folio.innreach.fixture.TestUtil.readFile;

import java.util.Collections;
import java.util.Map;

import javax.validation.Valid;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.MappingBuilder;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.common.Slf4jNotifier;
import io.github.glytching.junit.extension.watcher.WatcherExtension;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.RestController;

import org.folio.innreach.ModInnReachApplication;
import org.folio.innreach.util.JsonHelper;
import org.folio.spring.integration.XOkapiHeaders;
import org.folio.spring.liquibase.FolioLiquibaseConfiguration;
import org.folio.tenant.domain.dto.TenantAttributes;
import org.folio.tenant.rest.resource.TenantApi;

@Log4j2
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    classes = { ModInnReachApplication.class,	BaseApiControllerTest.TestTenantController.class })
@AutoConfigureMockMvc
@ActiveProfiles({ "test", "testcontainers-pg" })
@ExtendWith(WatcherExtension.class)
public class BaseApiControllerTest {

  @EnableAutoConfiguration(exclude = { FolioLiquibaseConfiguration.class })
  @RestController("folioTenantController")
  @Profile("test")
  static class TestTenantController implements TenantApi {

    @Override
    public ResponseEntity<String> postTenant(@Valid TenantAttributes tenantAttributes) {
        return ResponseEntity.ok("OK");
    }
  }

  protected static WireMockServer wm =
      new WireMockServer(wireMockConfig()
          .dynamicPort()
          .usingFilesUnderClasspath("wm")
          .notifier(new Slf4jNotifier(true)));

  @Autowired
  protected MockMvc mockMvc;
  @Autowired
  private JsonHelper jsonHelper;


  @DynamicPropertySource
  static void registerOkapiURL(DynamicPropertyRegistry registry) {
    registry.add("okapi_url", () -> wm.baseUrl());
    log.info("OKAPI Url: {}", wm.baseUrl());
  }

  @BeforeAll
  static void beforeAll() {
    wm.start();
    WireMock.configureFor(new WireMock(wm));
    log.info("Wire mock started");
  }

  @AfterAll
  static void afterAll() {
    wm.stop();
    log.info("Wire mock stopped");
  }

  @AfterEach
  void tearDown() {
    wm.resetAll();
  }

  protected void getAndExpect(String url, Template expectedResult) throws Exception {
    mockMvc.perform(get(url))
        .andExpect(status().isOk())
        .andExpect(content()
            .json(readTemplate(expectedResult)));
  }

  protected void putAndExpect(URI uri, Object requestBody, Template expectedResult) throws Exception {
    mockMvc.perform(put(uri.getUrlTemplate(), uri.getUriVars())
            .content(jsonHelper.toJson(requestBody))
            .contentType(MediaType.APPLICATION_JSON)
            .headers(getOkapiHeaders()))
        .andExpect(status().isOk())
        .andExpect(content().json(
            readTemplate(expectedResult)));
  }

  protected static void stubGet(String url, String responsePath) {
    stubGet(url, Collections.emptyMap(), responsePath);
  }

  protected static void stubGet(String url, Map<String, String> requestHeaders, String responsePath) {
    MappingBuilder getBuilder = WireMock.get(urlEqualTo(url));

    requestHeaders.forEach((name, value) -> getBuilder.withHeader(name, equalTo(value)));

    stubFor(getBuilder
      .willReturn(aResponse()
        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
        .withHeader(XOkapiHeaders.URL, wm.baseUrl())
        .withBodyFile(responsePath)));
  }

  protected static void stubPost(String url, String responsePath) {
    MappingBuilder builder = WireMock.post(urlEqualTo(url));

    stubFor(builder
      .willReturn(aResponse()
        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
        .withHeader(XOkapiHeaders.URL, wm.baseUrl())
        .withBodyFile(responsePath)));
  }

  protected static void stubPut(String url, String responsePath) {
    MappingBuilder builder = WireMock.put(urlEqualTo(url));

    stubFor(builder
      .willReturn(aResponse()
        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
        .withHeader(XOkapiHeaders.URL, wm.baseUrl())
        .withBodyFile(responsePath)));
  }

  private static String readTemplate(Template template) {
    String path = "json/" + template.getFile();

    return String.format(readFile(path), template.getParams());
  }

  public static HttpHeaders getOkapiHeaders() {
    HttpHeaders headers = new HttpHeaders();
    headers.add(XOkapiHeaders.URL, wm.baseUrl());
    return headers;
  }

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

}
