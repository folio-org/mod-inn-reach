package org.folio.innreach.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.retry.support.RetryTemplate;

import org.folio.innreach.domain.event.DomainEvent;

@EnableKafka
@Log4j2
@Configuration
@RequiredArgsConstructor
public class KafkaListenerConfiguration {

  public static final String KAFKA_RETRY_TEMPLATE = "kafkaRetryTemplate";

  @Bean
  public ConcurrentKafkaListenerContainerFactory<String, DomainEvent> kafkaListenerContainerFactory(ConsumerFactory<String, DomainEvent> consumerFactory) {
    var factory = new ConcurrentKafkaListenerContainerFactory<String, DomainEvent>();
    factory.setBatchListener(true);
    factory.setConsumerFactory(consumerFactory);
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

}
