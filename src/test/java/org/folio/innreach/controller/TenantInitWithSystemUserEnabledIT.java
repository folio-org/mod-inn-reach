package org.folio.innreach.controller;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.common.Slf4jNotifier;
import org.folio.innreach.ModInnReachApplication;
import org.folio.innreach.domain.listener.KafkaCirculationEventListener;
import org.folio.innreach.domain.listener.KafkaInitialContributionEventListener;
import org.folio.innreach.domain.listener.KafkaInventoryEventListener;
import org.folio.spring.integration.XOkapiHeaders;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Verifies that the application context loads and {@code POST /_/tenant} succeeds
 * when {@code folio.system-user.enabled=true}.
 *
 * <p>This exercises the {@code OptionalSystemUserConfig} bean creation path (from
 * folio-spring-system-user) which injects an unqualified {@code HttpServiceProxyFactory}.
 * The {@code PrimaryHttpServiceProxyFactoryConfig} ensures the correct (base) factory
 * is selected when multiple candidates exist.
 *
 * <p>This test activates only the {@code it} profile (not {@code test})
 * so the stub {@code TestTenantController} from {@code BaseControllerTest} is not loaded.
 * The real {@code TenantController} from folio-spring-base handles the request and
 * returns 204 NO_CONTENT.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
  classes = ModInnReachApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("it")
@TestPropertySource(properties = "folio.system-user.enabled=true")
class TenantInitWithSystemUserEnabledIT {

  private static final WireMockServer wm =
    new WireMockServer(wireMockConfig().dynamicPort().notifier(new Slf4jNotifier(true)));

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private KafkaCirculationEventListener kafkaCirculationEventListener;
  @MockitoBean
  private KafkaInventoryEventListener kafkaInventoryEventListener;
  @MockitoBean
  private KafkaInitialContributionEventListener kafkaInitialContributionEventListener;

  @DynamicPropertySource
  static void props(DynamicPropertyRegistry registry) {
    registry.add("folio.okapi-url", wm::baseUrl);
  }

  @BeforeAll
  static void startWm() {
    wm.start();
    WireMock.configureFor(new WireMock(wm));
  }

  @AfterAll
  static void stopWm() {
    wm.stop();
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
