package org.folio.innreach.batch.contribution.service;

import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.folio.innreach.batch.contribution.ContributionJobContext;
import org.folio.innreach.domain.dto.folio.inventorystorage.InstanceIterationEvent;
import org.springframework.kafka.listener.MessageListener;

import java.util.UUID;

import static org.folio.innreach.batch.contribution.IterationEventReaderFactory.ITERATION_JOB_ID_HEADER;

@RequiredArgsConstructor
@Log4j2
public class InitialContributionMessageListener implements MessageListener<String, InstanceIterationEvent> {

  private final IMessageProcessor iMessageProcessor;

  private final ContributionJobContext.Statistics statistics;

  @Override
  public void onMessage(
    ConsumerRecord<String, InstanceIterationEvent> consumerRecord) {


    UUID jobId = UUID.fromString(new String(consumerRecord.headers().lastHeader(ITERATION_JOB_ID_HEADER).value()));

    log.info("JobId-->:{}",jobId);

    UUID instanceId = UUID.fromString(consumerRecord.key());

    log.info("InstanceId-->>:{}",instanceId);

    InstanceIterationEvent instanceIterationEvent = consumerRecord.value();

    instanceIterationEvent.setInstanceId(instanceId);
    instanceIterationEvent.setJobId(jobId);

    iMessageProcessor.processMessage(instanceIterationEvent,statistics, consumerRecord.topic());

  }
}
