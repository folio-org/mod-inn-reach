package org.folio.innreach.controller.base;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;

import javax.validation.Valid;

import com.github.tomakehurst.wiremock.common.Slf4jNotifier;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.bind.annotation.RestController;

import org.folio.innreach.ModInnReachApplication;
import org.folio.spring.liquibase.FolioLiquibaseConfiguration;
import org.folio.tenant.domain.dto.TenantAttributes;
import org.folio.tenant.rest.resource.TenantApi;

@Log4j2
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = { ModInnReachApplication.class,
	BaseControllerTest.TestTenantController.class })
@ActiveProfiles({ "test", "testcontainers-pg" })
public class BaseControllerTest {

  @EnableAutoConfiguration(exclude = { FolioLiquibaseConfiguration.class })
  @RestController("folioTenantController")
  @Profile("test")
  static class TestTenantController implements TenantApi {

    @Override
    public ResponseEntity<String> postTenant(@Valid TenantAttributes tenantAttributes) {
        return ResponseEntity.ok("OK");
    }
  }

  @RegisterExtension
  protected static WireMockExtension wm = WireMockExtension.newInstance()
      .options(wireMockConfig().dynamicPort().usingFilesUnderClasspath("wm").notifier(new Slf4jNotifier(true)))
      .configureStaticDsl(true)
      .build();

  @DynamicPropertySource
  static void registerOkapiURL(DynamicPropertyRegistry registry) {
    registry.add("okapi.url", () -> wm.baseUrl());
    log.info("OKAPI Url: {}", wm.baseUrl());
  }

}
