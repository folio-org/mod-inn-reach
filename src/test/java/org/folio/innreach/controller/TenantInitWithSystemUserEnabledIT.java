package org.folio.innreach.controller;

import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.github.tomakehurst.wiremock.client.WireMock;
import org.folio.innreach.domain.listener.KafkaCirculationEventListener;
import org.folio.innreach.domain.listener.KafkaInitialContributionEventListener;
import org.folio.innreach.domain.listener.KafkaInventoryEventListener;
import org.folio.innreach.it.base.BaseIntegrationTest;
import org.folio.spring.integration.XOkapiHeaders;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * Verifies that the application context loads and {@code POST /_/tenant} succeeds
 * when {@code folio.system-user.enabled=true}.
 *
 * <p>This exercises the {@code OptionalSystemUserConfig} bean creation path (from
 * folio-spring-system-user) which injects an unqualified {@code HttpServiceProxyFactory}.
 * The {@code PrimaryHttpServiceProxyFactoryConfig} ensures the correct (base) factory
 * is selected when multiple candidates exist.
 */
@TestPropertySource(properties = "folio.system-user.enabled=true")
class TenantInitWithSystemUserEnabledIT extends BaseIntegrationTest {

  @MockitoBean
  private KafkaCirculationEventListener kafkaCirculationEventListener;
  @MockitoBean
  private KafkaInventoryEventListener kafkaInventoryEventListener;
  @MockitoBean
  private KafkaInitialContributionEventListener kafkaInitialContributionEventListener;

  @BeforeAll
  static void setUpMock() {
    stubFor(WireMock.post(urlPathEqualTo("/users"))
      .willReturn(ok().withHeader("Content-Type", "application/json")
        .withBody("{\"id\": \"test-user-id\"}")));
    stubFor(WireMock.get(urlPathEqualTo("/users"))
      .willReturn(ok().withHeader("Content-Type", "application/json")
        .withBody("[]")));
    stubFor(WireMock.post(urlPathEqualTo("/perms/users"))
      .willReturn(ok().withHeader("Content-Type", "application/json")
        .withBody("{\"id\": \"test-perm-id\"}")));
  }

  @Test
  void postTenant_succeeds_whenSystemUserEnabled() throws Exception {
    mockMvc.perform(post("/_/tenant")
        .header(XOkapiHeaders.TENANT, "test_tenant")
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"moduleTo\": \"mod-inn-reach-1.0.0\"}"))
      .andExpect(status().isNoContent());
  }
}
