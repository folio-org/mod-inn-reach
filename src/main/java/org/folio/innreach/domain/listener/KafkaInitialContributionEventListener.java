package org.folio.innreach.domain.listener;

import lombok.extern.log4j.Log4j2;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.folio.innreach.domain.dto.folio.inventorystorage.InstanceIterationEvent;
import org.folio.innreach.domain.service.KafkaEventProcessorService;
import org.folio.innreach.mapper.JobExecutionStatusMapper;
import org.folio.innreach.repository.JobExecutionStatusRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

import static org.folio.innreach.batch.contribution.IterationEventReaderFactory.ITERATION_JOB_ID_HEADER;


@Component
@Log4j2
public class KafkaInitialContributionEventListener {

  @Autowired
  private KafkaEventProcessorService kafkaEventProcessorService;

  @Autowired
  private JobExecutionStatusRepository jobExecutionStatusRepository;
  @Autowired
  private JobExecutionStatusMapper jobExecutionStatusMapper;

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
      log.info("processInitialContributionEvents :: consumerRecord {} ", consumerRecord);
      var instanceIterationEvent = getInstanceIterationEventFromKafkaRecord(consumerRecord);
      kafkaEventProcessorService.process(instanceIterationEvent, event ->
          jobExecutionStatusRepository.save(jobExecutionStatusMapper.toEntity(event))
        , instanceIterationEvent.getTenant());
    });
  }

  private InstanceIterationEvent getInstanceIterationEventFromKafkaRecord(ConsumerRecord<String, InstanceIterationEvent> record) {
    var instanceIterationEvent = record.value();
    instanceIterationEvent.setInstanceId(UUID.fromString(record.key()));
    instanceIterationEvent.setJobId(UUID.fromString(new String(record.headers().lastHeader(ITERATION_JOB_ID_HEADER).value())));
    return instanceIterationEvent;
  }

}
