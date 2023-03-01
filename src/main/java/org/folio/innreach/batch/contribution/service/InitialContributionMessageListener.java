package org.folio.innreach.batch.contribution.service;

import lombok.AllArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.folio.innreach.batch.contribution.ContributionJobContext;
import org.folio.innreach.domain.dto.folio.inventorystorage.InstanceIterationEvent;
import org.springframework.kafka.listener.AcknowledgingMessageListener;
import org.springframework.kafka.listener.MessageListener;
import org.springframework.kafka.support.Acknowledgment;

import java.util.UUID;

import static org.folio.innreach.batch.contribution.IterationEventReaderFactory.ITERATION_JOB_ID_HEADER;

@AllArgsConstructor
public class InitialContributionMessageListener implements MessageListener<String, InstanceIterationEvent> {

  IMessageProcessor iMessageProcessor;
  ContributionJobContext context;

  ContributionJobContext.Statistics statistics;

  @Override
  public void onMessage(
    ConsumerRecord<String, InstanceIterationEvent> consumerRecord) {

    //to check jobId and InstanceId

    UUID jobId = UUID.fromString(new String(consumerRecord.headers().lastHeader(ITERATION_JOB_ID_HEADER).value()));

    System.out.println("JobId-->"+jobId);

    UUID instanceId = UUID.fromString(consumerRecord.key());

    System.out.println("InstanceId-->>"+instanceId);


    //end

    // process message
    iMessageProcessor.processMessage(consumerRecord.key(), consumerRecord.value(), context, statistics, consumerRecord.topic());

    // commit offset
//    acknowledgment.acknowledge();
  }
}
