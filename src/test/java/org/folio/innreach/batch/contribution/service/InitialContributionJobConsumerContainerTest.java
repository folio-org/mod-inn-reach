package org.folio.innreach.batch.contribution.service;

import org.folio.innreach.external.exception.ServiceSuspendedException;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.*;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.kafka.core.*;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.support.serializer.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.folio.innreach.batch.contribution.ContributionJobContext;
import org.folio.innreach.batch.contribution.InitialContributionJobConsumerContainer;
import org.folio.innreach.batch.contribution.listener.ContributionExceptionListener;
import org.folio.innreach.config.RetryConfig;
import org.folio.innreach.config.props.ContributionJobProperties;
import org.folio.innreach.domain.dto.folio.inventorystorage.InstanceIterationEvent;
import org.folio.innreach.domain.listener.base.BaseKafkaApiTest;
import org.folio.innreach.domain.service.impl.ContributionServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.apache.kafka.clients.consumer.ConsumerConfig.GROUP_ID_CONFIG;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class InitialContributionJobConsumerContainerTest extends BaseKafkaApiTest{

  public static final String TOPIC = "folio.contrib.tester.innreach";
  public static final String TEST_TENANT = "testTenant";

  private final Long maxInterval = 2000L;

  private final Long maxAttempt = 2L;
  @Autowired
  private KafkaProperties kafkaProperties;

  @Autowired
  private ObjectMapper mapper;

  @Autowired
  private ContributionJobProperties jobProperties;

  @Autowired
  private RetryConfig retryConfig;

  @Mock
  ContributionJobRunner contributionJobRunner;

  @Autowired
  ContributionServiceImpl contributionService;


  @BeforeEach
  void clearMap() {
    InitialContributionJobConsumerContainer.consumersMap.clear();
  }

  @Test
  @Order(1)
  void testStartAndStopConsumerIfServiceException() throws InterruptedException {
    var context = prepareContext();
    var initialContributionJobConsumerContainer = prepareContributionJobConsumerContainer();
    InitialContributionMessageListener initialContributionMessageListener = prepareInitialContributionMessageListener(context);

    this.produceEvent();

    doThrow(ServiceSuspendedException.class).when(contributionJobRunner)
      .runInitialContribution(any(), any(), any(), any());

    initialContributionJobConsumerContainer.tryStartOrCreateConsumer(initialContributionMessageListener);

    await().atMost(Duration.ofSeconds(15L)).until(()->!InitialContributionJobConsumerContainer.consumersMap.get(TOPIC).isRunning());


  }

  @Test
  @Order(2)
  void testStartOrCreateConsumer() throws InterruptedException {
    var context = prepareContext();
    var initialContributionJobConsumerContainer = prepareContributionJobConsumerContainer();
    InitialContributionMessageListener initialContributionMessageListener = prepareInitialContributionMessageListener(context);

    this.produceEvent();

    initialContributionJobConsumerContainer.tryStartOrCreateConsumer(initialContributionMessageListener);

    Mockito.doNothing().when(contributionJobRunner).runInitialContribution(any(), any(), any(), any());

    Assertions.assertNotNull(InitialContributionJobConsumerContainer.consumersMap.get(TOPIC));

    Assertions.assertEquals(1,InitialContributionJobConsumerContainer.consumersMap.size());

  }

  @Test
  @Order(3)
  void stopConsumer() {
    var context = prepareContext();

    var initialContributionJobConsumerContainer = prepareContributionJobConsumerContainer();
    InitialContributionMessageListener initialContributionMessageListener = prepareInitialContributionMessageListener(context);

    initialContributionJobConsumerContainer.tryStartOrCreateConsumer(initialContributionMessageListener);

    InitialContributionJobConsumerContainer.stopConsumer(TOPIC);
    Assertions.assertNotNull(InitialContributionJobConsumerContainer.consumersMap.get(TOPIC));
  }

  @NotNull
  private InitialContributionMessageListener prepareInitialContributionMessageListener(ContributionJobContext context) {
    var contributionProcessor = new ContributionProcessor(contributionJobRunner);


    return new InitialContributionMessageListener(contributionProcessor, context, new ContributionJobContext.Statistics());
  }

  @Test
  @Order(4)
  void testContainerIfRunning() {

    var context = prepareContext();

    var consumerProperties = kafkaProperties.buildConsumerProperties();

    ConsumerFactory<String, InstanceIterationEvent> factory = new DefaultKafkaConsumerFactory<>(consumerProperties,keyDeserializer(),valueDeserializer());

    ContainerProperties containerProps = new ContainerProperties(TOPIC);

    containerProps.setPollTimeout(100);

    containerProps.setAckMode(ContainerProperties.AckMode.RECORD);

    var initialContributionJobConsumerContainer = prepareContributionJobConsumerContainer();
    var contributionProcessor = new ContributionProcessor(contributionJobRunner);

    var container = new ConcurrentMessageListenerContainer<>(factory,containerProps);

    InitialContributionMessageListener initialContributionMessageListener =
      new InitialContributionMessageListener(contributionProcessor, context, new ContributionJobContext.Statistics());

    container.setupMessageListener(initialContributionMessageListener);

    InitialContributionJobConsumerContainer.consumersMap.put(TOPIC,
      container);

    initialContributionJobConsumerContainer.tryStartOrCreateConsumer(initialContributionMessageListener);
    Mockito.doNothing().when(contributionJobRunner).runInitialContribution(any(), any(), any(), any());
    Assertions.assertNotNull(InitialContributionJobConsumerContainer.consumersMap.get(TOPIC));

  }

  private ContributionJobContext prepareContext() {
    return ContributionJobContext.builder()
      .contributionId(UUID.randomUUID())
      .iterationJobId(UUID.randomUUID())
      .centralServerId(UUID.randomUUID())
      .tenantId(TEST_TENANT)
      .build();
  }

  private void produceEvent() {
    new KafkaTemplate<String, InstanceIterationEvent>(producerFactory()).send(TOPIC, createInstanceIterationEvent());
  }

  public InitialContributionJobConsumerContainer prepareContributionJobConsumerContainer() {
      var consumerProperties = kafkaProperties.buildConsumerProperties();
      consumerProperties.put(GROUP_ID_CONFIG, jobProperties.getReaderGroupId());
      var contributionExceptionListener = new ContributionExceptionListener(contributionService, "instanceContribution");

      return new InitialContributionJobConsumerContainer(consumerProperties,TOPIC,keyDeserializer(),valueDeserializer(), maxInterval, maxAttempt, contributionExceptionListener);
    }

  private Deserializer<InstanceIterationEvent> valueDeserializer() {
    JsonDeserializer<InstanceIterationEvent> deserializer = new JsonDeserializer<>(InstanceIterationEvent.class, mapper);
    deserializer.setUseTypeHeaders(false);
    deserializer.addTrustedPackages("*");

    return deserializer;
  }

  private Deserializer<String> keyDeserializer() {
    return new StringDeserializer();
  }

  public InstanceIterationEvent createInstanceIterationEvent() {

    return InstanceIterationEvent.of(UUID.randomUUID(),"UPDATE","diku",UUID.randomUUID());
  }

  public <V> ProducerFactory<String, V> producerFactory() {
    Map<String, Object> props = new HashMap<>(kafkaProperties.buildProducerProperties());
    props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
    props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
    return new DefaultKafkaProducerFactory<>(props);
  }

}
