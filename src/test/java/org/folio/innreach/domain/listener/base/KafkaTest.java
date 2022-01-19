package org.folio.innreach.domain.listener.base;

import java.util.Map;

import javax.validation.Valid;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.bind.annotation.RestController;

import org.folio.innreach.ModInnReachApplication;
import org.folio.innreach.domain.event.DomainEvent;
import org.folio.spring.liquibase.FolioLiquibaseConfiguration;
import org.folio.tenant.domain.dto.TenantAttributes;
import org.folio.tenant.rest.resource.TenantApi;

@EmbeddedKafka(topics = "folio.testing.circulation.loan")
@ExtendWith(SpringExtension.class)
@RunWith(SpringRunner.class)
@SpringBootTest(classes = {ModInnReachApplication.class, KafkaTest.TestTenantController.class})
@DirtiesContext
@ActiveProfiles({"test", "testcontainers-pg"})
public class KafkaTest {

  @Autowired
  protected EmbeddedKafkaBroker embeddedKafkaBroker;

  protected KafkaTemplate<String, DomainEvent> kafkaTemplate;

  @BeforeEach
  public void setUp() {
    kafkaTemplate = buildKafkaTemplate();
  }

  private KafkaTemplate<String, DomainEvent> buildKafkaTemplate() {
    Map<String, Object> senderProps = KafkaTestUtils.producerProps(embeddedKafkaBroker);
    senderProps.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
    senderProps.put("value.serializer", "org.springframework.kafka.support.serializer.JsonSerializer");
    ProducerFactory<String, DomainEvent> pf = new DefaultKafkaProducerFactory<>(senderProps);
    return new KafkaTemplate<>(pf);
  }

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
