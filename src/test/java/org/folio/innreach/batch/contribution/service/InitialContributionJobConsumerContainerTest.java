package org.folio.innreach.batch.contribution.service;

import org.jetbrains.annotations.NotNull;
import org.mockito.Mockito;
import org.springframework.boot.test.mock.mockito.SpyBean;
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
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.SendResult;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.apache.kafka.clients.consumer.ConsumerConfig.GROUP_ID_CONFIG;

class InitialContributionJobConsumerContainerTest extends BaseKafkaApiTest{

  public static final String TOPIC = "folio.contrib.tester.innreach";
  public static final String TEST_TENANT = "testTenant";
  @Autowired
  private KafkaProperties kafkaProperties;

  @Autowired
  private ObjectMapper mapper;

  @Autowired
  private ContributionJobProperties jobProperties;

  @Autowired
  private RetryConfig retryConfig;

  @SpyBean
  ContributionJobRunner contributionJobRunner;

  @Autowired
  ContributionServiceImpl contributionService;


  @Test
  public void testStartOrCreateConsumer() {
    var context = ContributionJobContext.builder()
      .contributionId(UUID.randomUUID())
      .iterationJobId(UUID.randomUUID())
      .centralServerId(UUID.randomUUID())
      .tenantId(TEST_TENANT)
      .build();
    var initialContributionJobConsumerContainer = prepare();
    var contributionProcessor = new ContributionProcessor(contributionJobRunner);


    InitialContributionMessageListener initialContributionMessageListener =
      new InitialContributionMessageListener(contributionProcessor, context, new ContributionJobContext.Statistics());

    this.produceEvent();

   initialContributionJobConsumerContainer.tryStartOrCreateConsumer(initialContributionMessageListener);

    Mockito.doNothing().when(contributionJobRunner).runInitialContribution(Mockito.any(),Mockito.any(),Mockito.any(),Mockito.any());

  }

  @NotNull
  private CompletableFuture<SendResult<String, InstanceIterationEvent>> produceEvent() {
    return new KafkaTemplate<String, InstanceIterationEvent>(producerFactory()).send(TOPIC, createInstanceIterationEvent());
  }


  public InitialContributionJobConsumerContainer prepare() {
      var consumerProperties = kafkaProperties.buildConsumerProperties();
      consumerProperties.put(GROUP_ID_CONFIG, jobProperties.getReaderGroupId());


      var contributionExceptionListener = new ContributionExceptionListener(contributionService, "instanceContribution");


      return new InitialContributionJobConsumerContainer(consumerProperties,TOPIC,keyDeserializer(),valueDeserializer(), retryConfig.getInterval(), retryConfig.getMaxAttempts(), contributionExceptionListener);

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
