package org.folio.innreach.domain.listener.base;

import static org.folio.innreach.domain.listener.base.BaseKafkaApiTest.CIRC_LOAN_TOPIC;

import java.util.concurrent.Callable;

import javax.validation.Valid;

import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.stereotype.Service;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.bind.annotation.RestController;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import org.folio.innreach.ModInnReachApplication;
import org.folio.innreach.domain.event.DomainEvent;
import org.folio.innreach.domain.service.impl.TenantScopedExecutionService;
import org.folio.spring.liquibase.FolioLiquibaseConfiguration;
import org.folio.tenant.domain.dto.TenantAttributes;
import org.folio.tenant.rest.resource.TenantApi;

@ActiveProfiles("test")
@EmbeddedKafka(topics = {CIRC_LOAN_TOPIC})
@SpringBootTest(
  classes = {ModInnReachApplication.class, BaseKafkaApiTest.TestTenantController.class, BaseKafkaApiTest.TestTenantScopedExecutionService.class})
@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class BaseKafkaApiTest {

  public static final String CIRC_LOAN_TOPIC = "folio.testing.circulation.loan";

  @Container
  public static PostgreSQLContainer<?> postgresqlContainer = new PostgreSQLContainer<>("postgres:11-alpine");

  @Autowired
  protected EmbeddedKafkaBroker embeddedKafkaBroker;

  protected KafkaTemplate<String, DomainEvent> kafkaTemplate;

  @BeforeAll
  public void setUp() {
    kafkaTemplate = buildKafkaTemplate();
  }

  @DynamicPropertySource
  static void setDatasourceProperties(DynamicPropertyRegistry propertyRegistry) {
    postgresqlContainer.start();
    propertyRegistry.add("spring.datasource.url", postgresqlContainer::getJdbcUrl);
    propertyRegistry.add("spring.datasource.password", postgresqlContainer::getPassword);
    propertyRegistry.add("spring.datasource.username", postgresqlContainer::getUsername);
  }

  private KafkaTemplate<String, DomainEvent> buildKafkaTemplate() {
    var senderProps = KafkaTestUtils.producerProps(embeddedKafkaBroker);
    senderProps.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
    senderProps.put("value.serializer", "org.springframework.kafka.support.serializer.JsonSerializer");
    return new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(senderProps));
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

  @Primary
  @Service
  @Profile("test")
  static class TestTenantScopedExecutionService extends TenantScopedExecutionService {
    public TestTenantScopedExecutionService() {
      super(null, null);
    }

    @SneakyThrows
    @Override
    public <T> T executeTenantScoped(String tenantId, Callable<T> job) {
      return job.call();
    }
  }
}
