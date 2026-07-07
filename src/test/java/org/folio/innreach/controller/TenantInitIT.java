package org.folio.innreach.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.folio.innreach.domain.listener.KafkaCirculationEventListener;
import org.folio.innreach.domain.listener.KafkaInitialContributionEventListener;
import org.folio.innreach.domain.listener.KafkaInventoryEventListener;
import org.folio.innreach.it.base.BaseIntegrationTest;
import org.folio.spring.integration.XOkapiHeaders;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * Verifies that the application context loads and {@code POST /_/tenant} succeeds
 * when {@code folio.system-user.enabled=false} (the default in {@code application-test.yml}).
 */
class TenantInitIT extends BaseIntegrationTest {

  @MockitoBean
  private KafkaCirculationEventListener kafkaCirculationEventListener;
  @MockitoBean
  private KafkaInventoryEventListener kafkaInventoryEventListener;
  @MockitoBean
  private KafkaInitialContributionEventListener kafkaInitialContributionEventListener;

  @Test
  void postTenant_succeeds_whenSystemUserDisabled() throws Exception {
    mockMvc.perform(post("/_/tenant")
        .header(XOkapiHeaders.TENANT, "test_tenant")
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"moduleTo\": \"mod-inn-reach-1.0.0\"}"))
      .andExpect(status().isNoContent());
  }
}
