package org.folio.innreach.controller;

import static com.github.tomakehurst.wiremock.client.WireMock.requestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.folio.innreach.support.wiremock.WiremockContainerExtension.getWireMockClient;

import org.folio.innreach.it.base.BaseIT;
import org.folio.innreach.support.wiremock.WireMockStub;
import org.folio.tenant.domain.dto.TenantAttributes;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.test.context.TestPropertySource;

/**
 * Verifies that the application context loads and {@code POST /_/tenant} succeeds
 * when {@code folio.system-user.enabled=true}.
 *
 * <p>This exercises the {@code OptionalSystemUserConfig} bean creation path (from
 * folio-spring-system-user) which injects an unqualified {@code HttpServiceProxyFactory}.
 * The {@code PrimaryHttpServiceProxyFactoryConfig} ensures the correct (base) factory
 * is selected when multiple candidates exist.
 */
@TestPropertySource(properties = {
  "folio.system-user.enabled=true"
})
class TenantInitWithSystemUserEnabledIT extends BaseIT {

  private static final String TEST_TENANT = "test_tenant";

  @Test
  @WireMockStub(value = {
    "/wm/mappings/mod-authn/201-post(system_creds).json",
    "/wm/mappings/mod-authn/201-post(system_login).json",
    "/wm/mappings/mod-permissions/200-get-perms(system+empty).json",
    "/wm/mappings/mod-permissions/201-post(system).json",
    "/wm/mappings/mod-users/users/200-get-user(system+empty).json",
    "/wm/mappings/mod-users/users/201-post-user(system).json"
  })
  void postTenant_succeeds_whenSystemUserEnabled() {
    enableTenant(TEST_TENANT, new TenantAttributes().moduleTo("mod-inn-reach-1.0.0"));

    assertThatApiIsCalledOnce("/users", HttpMethod.POST.name());
    assertThatApiIsCalledOnce("/users", HttpMethod.GET.name());

    purgeTenant(TEST_TENANT);
  }

  private static void assertThatApiIsCalledOnce(String urlPath, String method) {
    var wiremock = getWireMockClient();
    wiremock.verifyThat(1, requestedFor(method, urlPathEqualTo(urlPath)));
  }
}
