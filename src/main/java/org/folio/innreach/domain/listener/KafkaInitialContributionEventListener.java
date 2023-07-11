package org.folio.innreach.domain.listener;

import lombok.extern.log4j.Log4j2;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.folio.innreach.domain.dto.folio.inventorystorage.InstanceIterationEvent;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.List;

import static org.folio.innreach.config.KafkaListenerConfiguration.KAFKA_CONTAINER_FACTORY;

@Component
@Log4j2
public class KafkaInitialContributionEventListener {

  {
    log.info("KafkaInitialContributionEventListener bean is created");
  }
  @KafkaListener(
    containerFactory = "kafkaInitialContributionContainer",
    id = "${kafka.listener.contribution.id}",
    groupId = "${kafka.listener.contribution.group-id}",
    topicPattern = "${kafka.listener.contribution.topic-pattern}",
    concurrency = "${kafka.listener.contribution.concurrency}"
  )
  public void processInitialContributionEvents(List<ConsumerRecord<String, InstanceIterationEvent>> consumerRecords) {
      log.info("Inside processInitialContributionEvents");
      consumerRecords.forEach(consumerRecord -> {
        log.info("key {} ", consumerRecord.key());
        log.info("value {} ", consumerRecord.value());
      });
  }
}
