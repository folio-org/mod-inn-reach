package org.folio.innreach.controller.base;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import jakarta.validation.Valid;

import org.folio.innreach.domain.listener.KafkaInitialContributionEventListener;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.bind.annotation.RestController;

import org.folio.innreach.ModInnReachApplication;
import org.folio.innreach.domain.listener.KafkaCirculationEventListener;
import org.folio.innreach.domain.listener.KafkaInventoryEventListener;
import org.folio.innreach.external.client.feign.InnReachAuthClient;
import org.folio.innreach.external.dto.AccessTokenDTO;
import org.folio.spring.liquibase.FolioLiquibaseConfiguration;
import org.folio.tenant.domain.dto.TenantAttributes;
import org.folio.tenant.rest.resource.TenantApi;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = {ModInnReachApplication.class,
  BaseControllerTest.TestTenantController.class})
@ActiveProfiles({"test", "testcontainers-pg"})
public class BaseControllerTest {

  @MockitoBean
  private KafkaCirculationEventListener kafkaCirculationEventListener;

  @MockitoBean
  private KafkaInventoryEventListener kafkaInventoryEventListener;

  @MockitoBean
  private KafkaInitialContributionEventListener kafkaInitialContributionEventListener;

  @MockitoBean
  protected InnReachAuthClient innReachAuthClient;

  @BeforeEach
  public void init() {
    when(innReachAuthClient.getAccessToken(any(), any())).thenReturn(ResponseEntity.ok(new AccessTokenDTO()));
  }

  @EnableAutoConfiguration(exclude = {FolioLiquibaseConfiguration.class})
  @RestController("folioTenantController")
  @Profile("test")
  static class TestTenantController implements TenantApi {

    @Override
    public ResponseEntity<Void> deleteTenant(String operationId) {
      return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Void> postTenant(@Valid TenantAttributes tenantAttributes) {
      return ResponseEntity.status(HttpStatus.CREATED).build();
    }

  }

}
