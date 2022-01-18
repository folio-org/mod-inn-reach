package org.folio.innreach.domain.listener.base;

import javax.validation.Valid;

import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.bind.annotation.RestController;

import org.folio.innreach.ModInnReachApplication;
import org.folio.spring.liquibase.FolioLiquibaseConfiguration;
import org.folio.tenant.domain.dto.TenantAttributes;
import org.folio.tenant.rest.resource.TenantApi;

@EmbeddedKafka
@ExtendWith(SpringExtension.class)
@RunWith(SpringRunner.class)
@SpringBootTest(classes = {ModInnReachApplication.class, KafkaTest.TestTenantController.class})
@DirtiesContext
@ActiveProfiles({"test", "testcontainers-pg"})
public class KafkaTest {

  @Autowired
  protected EmbeddedKafkaBroker embeddedKafkaBroker;

  @EnableAutoConfiguration(exclude = {FolioLiquibaseConfiguration.class})
  @RestController("folioTenantController")
  @Profile("test")
  static class TestTenantController implements TenantApi {

    @Override
    public ResponseEntity<String> postTenant(@Valid TenantAttributes tenantAttributes) {
      return ResponseEntity.ok("OK");
    }
  }
}
