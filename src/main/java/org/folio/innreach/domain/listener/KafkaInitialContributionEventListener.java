package org.folio.innreach.domain.listener;

import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.folio.innreach.domain.dto.folio.inventorystorage.InstanceIterationEvent;
import org.folio.innreach.domain.service.KafkaEventProcessorService;
import org.folio.innreach.mapper.JobExecutionStatusMapper;
import org.folio.innreach.repository.JobExecutionStatusRepository;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;


@Component
@Log4j2
@AllArgsConstructor
public class KafkaInitialContributionEventListener {

  private KafkaEventProcessorService kafkaEventProcessorService;
  private JobExecutionStatusRepository jobExecutionStatusRepository;
  private JobExecutionStatusMapper jobExecutionStatusMapper;
  public static final String ITERATION_JOB_ID_HEADER = "iteration-job-id";

  @KafkaListener(
    containerFactory = "kafkaInitialContributionContainer",
    id = "${kafka.listener.contribution.id}",
    groupId = "${kafka.listener.contribution.group-id}",
    topicPattern = "${kafka.listener.contribution.topic-pattern}",
    concurrency = "${kafka.listener.contribution.concurrency}")
  public void processInitialContributionEvents(List<ConsumerRecord<String, InstanceIterationEvent>> consumerRecords) {
    log.debug("processInitialContributionEvents:: Received records of size {} ", consumerRecords.size());
    consumerRecords.forEach(consumerRecord -> {
      log.info("processInitialContributionEvents :: consumerRecord {} ", consumerRecord);
      var instanceIterationEvent = getInstanceIterationEventFromKafkaRecord(consumerRecord);
      kafkaEventProcessorService.process(instanceIterationEvent, event ->
          jobExecutionStatusRepository.save(jobExecutionStatusMapper.toEntity(event))
        , instanceIterationEvent.getTenant());
    });
  }

  private InstanceIterationEvent getInstanceIterationEventFromKafkaRecord(
    ConsumerRecord<String, InstanceIterationEvent> consumerRecord) {
    var instanceIterationEvent = consumerRecord.value();
    instanceIterationEvent.setInstanceId(UUID.fromString(consumerRecord.key()));
    instanceIterationEvent.setJobId(UUID.fromString(
      new String(consumerRecord.headers().lastHeader(ITERATION_JOB_ID_HEADER).value())));
    return instanceIterationEvent;
  }

}
