package org.folio.innreach.controller.base;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;

import javax.validation.Valid;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.common.Slf4jNotifier;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
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

  protected static WireMockServer wm =
      new WireMockServer(wireMockConfig()
          .dynamicPort()
          .usingFilesUnderClasspath("wm")
          .notifier(new Slf4jNotifier(true)));

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

}
