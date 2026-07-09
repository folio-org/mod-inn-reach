package org.folio.innreach.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.folio.innreach.it.base.BaseIT;
import org.folio.spring.integration.XOkapiHeaders;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

/**
 * Verifies that the application context loads and {@code POST /_/tenant} succeeds
 * when {@code folio.system-user.enabled=false} (the default in {@code application-test.yml}).
 */
class TenantInitIT extends BaseIT {

  @Test
  void postTenant_succeeds_whenSystemUserDisabled() throws Exception {
    mockMvc.perform(post("/_/tenant")
        .header(XOkapiHeaders.TENANT, "test_tenant")
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"moduleTo\": \"mod-inn-reach-1.0.0\"}"))
      .andExpect(status().isNoContent());
  }
}
