package org.folio.innreach.domain.listener.base;

import static org.folio.innreach.domain.listener.base.BaseKafkaApiTest.CIRC_CHECKIN_TOPIC;
import static org.folio.innreach.domain.listener.base.BaseKafkaApiTest.CIRC_LOAN_TOPIC;
import static org.folio.innreach.domain.listener.base.BaseKafkaApiTest.CIRC_REQUEST_TOPIC;
import static org.folio.innreach.domain.listener.base.BaseKafkaApiTest.INITIAL_CONTRIBUTION_TOPIC;
import static org.folio.innreach.domain.listener.base.BaseKafkaApiTest.INVENTORY_HOLDING_TOPIC;
import static org.folio.innreach.domain.listener.base.BaseKafkaApiTest.INVENTORY_INSTANCE_TOPIC;
import static org.folio.innreach.domain.listener.base.BaseKafkaApiTest.INVENTORY_ITEM_TOPIC;
import static org.folio.innreach.domain.listener.base.BaseKafkaApiTest.INVENTORY_ITEM_TOPIC1;
import static org.folio.innreach.domain.listener.base.BaseKafkaApiTest.INVENTORY_ITEM_TOPIC2;
import static org.folio.innreach.domain.listener.base.BaseKafkaApiTest.INVENTORY_ITEM_TOPIC4;
import static org.folio.innreach.domain.listener.base.BaseKafkaApiTest.TestTenantScopedExecutionService;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import lombok.SneakyThrows;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.folio.innreach.ModInnReachApplication;
import org.folio.innreach.domain.event.DomainEvent;
import org.folio.innreach.domain.service.impl.TenantScopedExecutionService;
import org.folio.innreach.external.client.feign.InnReachAuthClient;
import org.folio.innreach.external.dto.AccessTokenDTO;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
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
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@ActiveProfiles("test")
@EmbeddedKafka(topics = {CIRC_LOAN_TOPIC, CIRC_REQUEST_TOPIC, INITIAL_CONTRIBUTION_TOPIC, CIRC_CHECKIN_TOPIC,
  INVENTORY_ITEM_TOPIC, INVENTORY_HOLDING_TOPIC, INVENTORY_INSTANCE_TOPIC, INVENTORY_ITEM_TOPIC1, INVENTORY_ITEM_TOPIC2, INVENTORY_ITEM_TOPIC4})
@SpringBootTest(
  classes = {ModInnReachApplication.class, TestTenantScopedExecutionService.class})
@DirtiesContext
@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class BaseKafkaApiTest {

  public static final String CIRC_LOAN_TOPIC = "folio.testing.circulation.loan";
  public static final String CIRC_REQUEST_TOPIC = "folio.testing.circulation.request";
  public static final String CIRC_CHECKIN_TOPIC = "folio.testing.circulation.check-in";
  public static final String INVENTORY_ITEM_TOPIC = "folio.testing.inventory.item";
  public static final String INVENTORY_HOLDING_TOPIC = "folio.testing.inventory.holdings-record";
  public static final String INVENTORY_INSTANCE_TOPIC = "folio.testing.inventory.instance";
  public static final String INVENTORY_ITEM_TOPIC1 = "folio.testing1.inventory.item";
  public static final String INVENTORY_ITEM_TOPIC2 = "folio.testing2.inventory.item";
  public static final String INVENTORY_ITEM_TOPIC4 = "folio.testing4.inventory.item";
  public static final String INITIAL_CONTRIBUTION_TOPIC = "folio.testing.inventory.instance-contribution";


  @Container
  public static PostgreSQLContainer<?> postgresqlContainer = new PostgreSQLContainer<>(Objects
    .toString(System.getenv("TESTCONTAINERS_POSTGRES_IMAGE"), "postgres:16-alpine"));

  @Autowired
  protected EmbeddedKafkaBroker embeddedKafkaBroker;

  @MockitoBean
  protected InnReachAuthClient innReachAuthClient;

  protected KafkaTemplate<String, DomainEvent> kafkaTemplate;

  @BeforeAll
  public void setUp() {
    kafkaTemplate = buildKafkaTemplate();
  }

  @BeforeEach
  public void setUpMocks() {
    when(innReachAuthClient.getAccessToken(any(), any())).thenReturn(ResponseEntity.ok(new AccessTokenDTO()));
  }

  protected static <T> List<ConsumerRecord<String, DomainEvent<T>>> asSingleConsumerRecord(String topic, UUID entityId, DomainEvent<T> event) {
    return List.of(new ConsumerRecord(topic, 1, 1, entityId.toString(), event));
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

  @Primary
  @Service
  @Profile("test")
  static class TestTenantScopedExecutionService extends TenantScopedExecutionService {
    public TestTenantScopedExecutionService() {
      super(null);
    }

    @SneakyThrows
    public void runTenantScoped(String tenantId, Runnable job) {
      // Adding this condition intentionally to simulate the error scenario which may happen when the system user is used for login
      if(tenantId.equals("testing4")) {
        throw new RuntimeException("testing exception");
      }
      job.run();
    }

    public void executeAsyncTenantScoped(String tenantId, Runnable job) {
      job.run();
    }

  }
}
