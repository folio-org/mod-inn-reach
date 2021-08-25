package org.folio.innreach.listener;

import static org.folio.innreach.domain.service.impl.KafkaService.EVENT_LISTENER_ID;

import java.util.List;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;

import org.folio.innreach.domain.dto.folio.inventoryStorage.InstanceIterationEvent;
import org.folio.innreach.domain.service.ContributionService;
import org.folio.innreach.utils.BatchProcessor;

@Log4j2
@RequiredArgsConstructor
public class KafkaMessageListener {

  private final BatchProcessor batchProcessor;
  private final ContributionService contributionService;

  @KafkaListener(
    id = EVENT_LISTENER_ID,
    containerFactory = "kafkaListenerContainerFactory",
    topicPattern = "#{folioKafkaProperties.listener['events'].topicPattern}",
    groupId = "#{folioKafkaProperties.listener['events'].groupId}",
    concurrency = "#{folioKafkaProperties.listener['events'].concurrency}")
  public void handleEvents(List<ConsumerRecord<String, InstanceIterationEvent>> consumerRecords) {
    log.info("Processing instance ids from kafka events [number of events: {}]", consumerRecords.size());

    var events = toIterationEvents(consumerRecords);

    batchProcessor.process(events, contributionService::contributeInstances, KafkaMessageListener::logFailedEvent);
  }

  private static void logFailedEvent(InstanceIterationEvent event, Exception e) {
    log.warn("Failed to index resource event [eventType: {}, tenantId: {}, jobId: {}, instanceId: {}]",
      event.getType(), event.getTenant(), event.getJobId(), event.getInstanceId(), e);
  }

  private static List<InstanceIterationEvent> toIterationEvents(List<ConsumerRecord<String, InstanceIterationEvent>> consumerRecords) {
    consumerRecords.forEach(r -> r.value().setInstanceId(r.key()));

    return consumerRecords.stream()
      .map(ConsumerRecord::value)
      .collect(Collectors.toList());
  }

}
