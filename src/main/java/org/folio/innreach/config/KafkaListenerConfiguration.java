package org.folio.innreach.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.retry.support.RetryTemplate;

import org.folio.innreach.config.props.FolioKafkaProperties;
import org.folio.innreach.domain.event.DomainEvent;
import org.folio.innreach.domain.service.impl.DomainEventTypeResolver;

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
    factory.setBatchErrorHandler(((exception, data) -> log.error("Unable to consume event {}", data, exception)));
    return factory;
  }

  @Bean(BATCH_EVENT_PROCESSOR_RETRY_TEMPLATE)
  public RetryTemplate batchEventRetryTemplate() {
    return RetryTemplate.builder()
      .maxAttempts(2)
      .fixedBackoff(100)
      .build();
  }

}
