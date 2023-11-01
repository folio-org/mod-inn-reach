package org.folio.innreach.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.folio.innreach.batch.contribution.service.ContributionJobRunner;
import org.folio.innreach.domain.dto.folio.inventorystorage.InstanceIterationEvent;
import org.folio.innreach.domain.service.impl.TenantScopedExecutionService;
import org.folio.innreach.external.exception.InnReachConnectionException;
import org.folio.innreach.external.exception.ServiceSuspendedException;
import org.folio.innreach.external.exception.SocketTimeOutExceptionWrapper;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.retry.support.RetryTemplate;

import org.folio.innreach.config.props.FolioKafkaProperties;
import org.folio.innreach.domain.event.DomainEvent;
import org.folio.innreach.domain.service.impl.DomainEventTypeResolver;
import org.springframework.util.backoff.BackOff;
import org.springframework.util.backoff.FixedBackOff;

import static org.folio.innreach.batch.contribution.ContributionJobContextManager.endContributionJobContext;
import static org.folio.innreach.batch.contribution.ContributionJobContextManager.getContributionJobContext;

@EnableKafka
@Log4j2
@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(FolioKafkaProperties.class)
public class KafkaListenerConfiguration {

  public static final String KAFKA_CONTAINER_FACTORY = "kafkaDomainEventContainerFactory";
  public static final String KAFKA_CONSUMER_FACTORY = "kafkaDomainEventConsumerFactory";
  public static final String BATCH_EVENT_PROCESSOR_RETRY_TEMPLATE = "batchEventRetryTemplate";

  private final ObjectMapper mapper;
  private final KafkaProperties kafkaProperties;
  private final DomainEventTypeResolver typeResolver;
  private final ContributionJobRunner contributionJobRunner;
  private final TenantScopedExecutionService executionService;

  private final RetryConfig retryConfig;

  @Bean(KAFKA_CONSUMER_FACTORY)
  public ConsumerFactory<String, DomainEvent> kafkaDomainEventConsumerFactory() {
    var consumerProperties = kafkaProperties.buildConsumerProperties();

    JsonDeserializer<DomainEvent> deserializer = new JsonDeserializer<>(mapper);
    deserializer.setTypeResolver(typeResolver);
    deserializer.setUseTypeHeaders(false);
    deserializer.addTrustedPackages("*");

    var errorDeserializer = new ErrorHandlingDeserializer<>(deserializer);
    errorDeserializer.setFailedDeserializationFunction(
      info -> {
        log.error("Unable to deserialize value from topic: " + info.getTopic(), info.getException());
        return null;
      });

    return new DefaultKafkaConsumerFactory<>(consumerProperties, new StringDeserializer(), errorDeserializer);
  }

  @Bean(KAFKA_CONTAINER_FACTORY)
  public ConcurrentKafkaListenerContainerFactory<String, DomainEvent> kafkaDomainEventContainerFactory() {
    var factory = new ConcurrentKafkaListenerContainerFactory<String, DomainEvent>();
    factory.setBatchListener(true);
    factory.setConsumerFactory(kafkaDomainEventConsumerFactory());
 //   factory.setBatchErrorHandler(((exception, data) -> log.error("Unable to consume event {}", data, exception)));
    factory.setCommonErrorHandler(errorHandler());
    return factory;
  }

  @Bean("kafkaInitialContributionConsumer")
  public ConsumerFactory<String, InstanceIterationEvent> kafkaInitialContributionEventConsumerFactory() {
    var consumerProperties = kafkaProperties.buildConsumerProperties();

    JsonDeserializer<InstanceIterationEvent> deserializer = new JsonDeserializer<>(InstanceIterationEvent.class);
    deserializer.setUseTypeHeaders(false);
    deserializer.addTrustedPackages("*");

    var errorDeserializer = new ErrorHandlingDeserializer<>(deserializer);
    errorDeserializer.setFailedDeserializationFunction(
      info -> {
        log.error("kafkaInitialContributionEventConsumerFactory:: Unable to deserialize value from topic: " + info.getTopic(), info.getException());
        return null;
      });

    return new DefaultKafkaConsumerFactory<>(consumerProperties, new StringDeserializer(), errorDeserializer);
  }

  @Bean("kafkaInitialContributionContainer")
  public ConcurrentKafkaListenerContainerFactory<String, InstanceIterationEvent> kafkaInitialContributionEventContainerFactory() {
    var factory = new ConcurrentKafkaListenerContainerFactory<String, InstanceIterationEvent>();
    factory.setBatchListener(true);
    factory.setConsumerFactory(kafkaInitialContributionEventConsumerFactory());
    factory.setCommonErrorHandler(errorHandler());
    return factory;
  }

  @Bean(BATCH_EVENT_PROCESSOR_RETRY_TEMPLATE)
  public RetryTemplate batchEventRetryTemplate() {
    return RetryTemplate.builder()
      .maxAttempts(2)
      .fixedBackoff(100)
      .build();
  }

  public DefaultErrorHandler errorHandler() {
    BackOff fixedBackOff = new FixedBackOff(retryConfig.getInterval(), retryConfig.getMaxAttempts());
    DefaultErrorHandler errorHandler = new DefaultErrorHandler((consumerRecord, exception) -> {
      log.info("inside errorHandler for Ongoing contribution");
      // logic to execute when all the retry attempts are exhausted
      executionService.runTenantScoped(getContributionJobContext().getTenantId(),
          () -> contributionJobRunner.completeContribution(getContributionJobContext()));
      endContributionJobContext();
    }, fixedBackOff);
    errorHandler.addRetryableExceptions(ServiceSuspendedException.class);
    errorHandler.addRetryableExceptions(SocketTimeOutExceptionWrapper.class);
    errorHandler.addRetryableExceptions(FeignException.class);
    errorHandler.addRetryableExceptions(InnReachConnectionException.class);
    return errorHandler;
  }

}
