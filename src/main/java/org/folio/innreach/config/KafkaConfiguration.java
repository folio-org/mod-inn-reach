package org.folio.innreach.config;

import static org.apache.kafka.clients.consumer.ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG;
import static org.apache.kafka.clients.consumer.ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG;

import java.util.HashMap;
import java.util.Map;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.retry.support.RetryTemplate;

import org.folio.innreach.config.props.FolioKafkaProperties;
import org.folio.innreach.domain.dto.folio.inventorystorage.InstanceIterationEvent;
import org.folio.innreach.domain.service.ContributionService;
import org.folio.innreach.listener.KafkaMessageListener;
import org.folio.innreach.utils.BatchProcessor;

@Log4j2
@Configuration
@RequiredArgsConstructor
@ConditionalOnProperty(value = "kafka.enable", havingValue = "true", matchIfMissing = true)
public class KafkaConfiguration {

  public static final String KAFKA_BATCH_PROCESSOR = "kafkaBatchProcessor";
  private final KafkaProperties kafkaProperties;
  private final FolioKafkaProperties folioKafkaProperties;

  @Bean
  public ConcurrentKafkaListenerContainerFactory<String, InstanceIterationEvent> kafkaListenerContainerFactory() {
    var factory = new ConcurrentKafkaListenerContainerFactory<String, InstanceIterationEvent>();
    factory.setBatchListener(true);
    factory.setConsumerFactory(jsonNodeConsumerFactory());
    return factory;
  }

  @Bean(name = KAFKA_BATCH_PROCESSOR)
  public BatchProcessor batchProcessor() {
    var retryTemplate = RetryTemplate.builder()
      .maxAttempts(folioKafkaProperties.getRetryAttempts())
      .fixedBackoff(folioKafkaProperties.getRetryIntervalMs())
      .build();

    return new BatchProcessor(retryTemplate);
  }

  @Bean
  public KafkaMessageListener listener(BatchProcessor batchProcessor, ContributionService contributionService) {
    return new KafkaMessageListener(batchProcessor, contributionService);
  }

  private ConsumerFactory<String, InstanceIterationEvent> jsonNodeConsumerFactory() {
    var deserializer = new JsonDeserializer<>(InstanceIterationEvent.class);
    Map<String, Object> config = new HashMap<>(kafkaProperties.buildConsumerProperties());
    config.put(KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
    config.put(VALUE_DESERIALIZER_CLASS_CONFIG, deserializer);
    return new DefaultKafkaConsumerFactory<>(config, new StringDeserializer(), deserializer);
  }

}
