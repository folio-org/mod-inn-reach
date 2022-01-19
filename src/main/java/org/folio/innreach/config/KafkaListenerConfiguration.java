package org.folio.innreach.config;

import static org.folio.innreach.config.props.FolioKafkaProperties.KafkaListenerProperties;

import java.util.Collection;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonTypeResolver;
import org.springframework.retry.support.RetryTemplate;

import org.folio.innreach.config.props.FolioKafkaProperties;
import org.folio.innreach.domain.event.DomainEvent;

@EnableKafka
@Log4j2
@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(FolioKafkaProperties.class)
public class KafkaListenerConfiguration {

  public static final String KAFKA_CONTAINER_FACTORY = "kafkaDomainEventContainerFactory";
  public static final String KAFKA_CONSUMER_FACTORY = "kafkaDomainEventConsumerFactory";
  public static final String KAFKA_RETRY_TEMPLATE = "kafkaRetryTemplate";

  private final KafkaProperties kafkaProperties;
  private final ObjectMapper mapper;
  private final FolioKafkaProperties folioKafkaProperties;

  @Bean(KAFKA_CONSUMER_FACTORY)
  public ConsumerFactory<String, DomainEvent> kafkaDomainEventConsumerFactory() {
    var consumerProperties = kafkaProperties.buildConsumerProperties();

    JsonDeserializer<DomainEvent> deserializer = new JsonDeserializer<>(mapper);
    deserializer.setTypeResolver(new DomainEventTypeResolver(folioKafkaProperties.getListener().values(), mapper));

    return new DefaultKafkaConsumerFactory<>(consumerProperties, new StringDeserializer(), deserializer);
  }

  @Bean(KAFKA_CONTAINER_FACTORY)
  public ConcurrentKafkaListenerContainerFactory<String, DomainEvent> kafkaDomainEventContainerFactory() {
    var factory = new ConcurrentKafkaListenerContainerFactory<String, DomainEvent>();
    factory.setBatchListener(true);
    factory.setConsumerFactory(kafkaDomainEventConsumerFactory());
    factory.setBatchErrorHandler(((exception, data) -> log.error("Unable to consume event {}", data, exception)));
    return factory;
  }

  @Bean(KAFKA_RETRY_TEMPLATE)
  public RetryTemplate kafkaRetryTemplate() {
    return RetryTemplate.builder()
      .maxAttempts(2)
      .fixedBackoff(100)
      .build();
  }

  @RequiredArgsConstructor
  private static class DomainEventTypeResolver implements JsonTypeResolver {

    private final Collection<KafkaListenerProperties> listeners;
    private final ObjectMapper mapper;

    @Override
    public JavaType resolveType(String topic, byte[] data, Headers headers) {
      var listener = listeners.stream()
        .filter(e -> topic.matches(e.getTopicPattern()))
        .findFirst()
        .orElseThrow(() -> new RuntimeException("No listener is registered for topic: " + topic));

      return mapper.getTypeFactory().constructParametricType(DomainEvent.class, listener.getDataType());
    }
  }

}
