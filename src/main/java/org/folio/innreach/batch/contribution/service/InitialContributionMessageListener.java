package org.folio.innreach.batch.contribution.service;

import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.folio.innreach.batch.contribution.ContributionJobContext;
import org.folio.innreach.domain.dto.folio.inventorystorage.InstanceIterationEvent;
import org.springframework.kafka.listener.AcknowledgingMessageListener;
import org.springframework.kafka.support.Acknowledgment;

import java.util.UUID;

import static org.folio.innreach.batch.contribution.IterationEventReaderFactory.ITERATION_JOB_ID_HEADER;

@AllArgsConstructor
@Log4j2
public class InitialContributionMessageListener implements AcknowledgingMessageListener<String, InstanceIterationEvent> {

  IMessageProcessor iMessageProcessor;
  ContributionJobContext context;

  ContributionJobContext.Statistics statistics;

  @Override
  public void onMessage(
    ConsumerRecord<String, InstanceIterationEvent> consumerRecord, Acknowledgment acknowledgment) {

    //to check jobId and InstanceId

    UUID jobId = UUID.fromString(new String(consumerRecord.headers().lastHeader(ITERATION_JOB_ID_HEADER).value()));

    System.out.println("JobId-->"+jobId);

    UUID instanceId = UUID.fromString(consumerRecord.key());

    System.out.println("InstanceId-->>"+instanceId);

    InstanceIterationEvent instanceIterationEvent = consumerRecord.value();

    instanceIterationEvent.setInstanceId(instanceId);
    instanceIterationEvent.setJobId(jobId);

    iMessageProcessor.processMessage(instanceIterationEvent, context, statistics, consumerRecord.topic());

    if(acknowledgment!=null) {
      log.info("Message is acknowledged for instanceId : {}",instanceId);
      acknowledgment.acknowledge();
    }
  }
}
