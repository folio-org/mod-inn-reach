package org.folio.innreach.it.base;

import static org.assertj.core.api.Assertions.assertThat;
import static org.folio.innreach.support.wiremock.WiremockContainerExtension.WM_URL_PROPERTY;
import static org.folio.spring.integration.XOkapiHeaders.TENANT;
import static org.folio.spring.integration.XOkapiHeaders.TOKEN;
import static org.folio.spring.integration.XOkapiHeaders.URL;
import static org.folio.spring.integration.XOkapiHeaders.USER_ID;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.context.jdbc.SqlMergeMode.MergeMode.MERGE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.UUID;
import lombok.SneakyThrows;
import org.folio.innreach.support.kafka.WithKafkaContainer;
import org.folio.innreach.support.postgres.WithPostgresContainer;
import org.folio.innreach.support.wiremock.WithWiremockContainer;
import org.folio.tenant.domain.dto.TenantAttributes;
import org.junit.jupiter.api.BeforeAll;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.SqlMergeMode;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@AutoConfigureMockMvc
@WithPostgresContainer
@WithKafkaContainer
@WithWiremockContainer
@SqlMergeMode(MERGE)
public abstract class BaseIntegrationTest {

  protected static final String MODULE_NAME = "mod-inn-reach";
  protected static final String TEST_TENANT = "testing";
  protected static final String TEST_TOKEN = "dGVzdF9qd3RfdG9rZW4=";
  protected static final String TEST_USER_ID = UUID.randomUUID().toString();

  private static final JsonMapper JSON_MAPPER = JsonMapper.builder()
    .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
    .build();

  protected static MockMvc mockMvc;

  @BeforeAll
  static void setupMockMvc(@Autowired MockMvc mockMvc) {
    BaseIntegrationTest.mockMvc = mockMvc;
  }

  @SneakyThrows
  protected static void enableTenant() {
    enableTenant(TEST_TENANT, new TenantAttributes());
  }

  @SneakyThrows
  @SuppressWarnings("SameParameterValue")
  protected static void enableTenant(String tenant, TenantAttributes tenantAttributes) {
    tenantAttributes.moduleTo(MODULE_NAME);
    mockMvc.perform(post("/_/tenant")
        .content(asJsonString(tenantAttributes))
        .contentType(APPLICATION_JSON)
        .header(TENANT, tenant)
        .header(URL, System.getProperty(WM_URL_PROPERTY)))
      .andExpect(status().is2xxSuccessful());
  }

  @SneakyThrows
  protected static void purgeTenant() {
    purgeTenant(TEST_TENANT);
  }

  @SneakyThrows
  @SuppressWarnings("SameParameterValue")
  protected static void purgeTenant(String tenantId) {
    var tenantAttributes = new TenantAttributes().moduleFrom(MODULE_NAME).purge(true);
    mockMvc.perform(post("/_/tenant")
        .content(asJsonString(tenantAttributes))
        .contentType(APPLICATION_JSON)
        .header(TENANT, tenantId)
        .header(TOKEN, TEST_TOKEN))
      .andExpect(status().is2xxSuccessful());
  }

  public static HttpHeaders defaultHeaders() {
    return defaultHeaders(TEST_USER_ID);
  }

  public static HttpHeaders defaultHeaders(String userId) {
    var headers = new HttpHeaders();
    headers.setContentType(APPLICATION_JSON);
    headers.put(TENANT, List.of(TEST_TENANT));
    headers.add(URL, getWiremockUrl());
    headers.add(TOKEN, TEST_TOKEN);
    headers.add(USER_ID, userId);
    return headers;
  }

  protected static String getWiremockUrl() {
    var url = System.getProperty(WM_URL_PROPERTY);
    assertThat(url).isNotBlank();
    return url;
  }

  @SneakyThrows
  protected static String asJsonString(Object obj) {
    return JSON_MAPPER.writeValueAsString(obj);
  }

  @SneakyThrows
  protected static <T> T fromJson(MvcResult result, Class<T> type) {
    return JSON_MAPPER.readValue(result.getResponse().getContentAsString(), type);
  }
}
