package org.folio.innreach.batch.contribution.service;

import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.folio.innreach.batch.contribution.InitialContributionJobConsumerContainer;
import org.folio.innreach.domain.dto.folio.inventorystorage.InstanceIterationEvent;
import org.folio.innreach.external.exception.InnReachConnectionException;
import org.folio.innreach.external.exception.ServiceSuspendedException;
import org.folio.innreach.external.exception.SocketTimeOutExceptionWrapper;
import org.springframework.kafka.listener.MessageListener;

import java.util.UUID;

import static org.folio.innreach.batch.contribution.IterationEventReaderFactory.ITERATION_JOB_ID_HEADER;

@RequiredArgsConstructor
@Log4j2
public class InitialContributionMessageListener implements MessageListener<String, InstanceIterationEvent> {

  private final IMessageProcessor iMessageProcessor;

  @Override
  public void onMessage(
    ConsumerRecord<String, InstanceIterationEvent> consumerRecord) {

    log.info("Inside InitialContributionMessageListener:onMessage");
    try {
      UUID jobId = UUID.fromString(new String(consumerRecord.headers().lastHeader(ITERATION_JOB_ID_HEADER).value()));
      UUID instanceId = UUID.fromString(consumerRecord.key());

      log.info("InitialContributionMessageListener: Initial contribution message listener called: JobId: {}, InstanceId: {}", jobId, instanceId);

      InstanceIterationEvent instanceIterationEvent = consumerRecord.value();

      instanceIterationEvent.setInstanceId(instanceId);
      instanceIterationEvent.setJobId(jobId);
      InitialContributionJobConsumerContainer.stopConsumer(consumerRecord.topic());
      iMessageProcessor.processMessage(instanceIterationEvent, consumerRecord.topic());
    }
    catch (ServiceSuspendedException | FeignException | InnReachConnectionException | SocketTimeOutExceptionWrapper e) {
      throw e;
    }
    catch (Exception e) {
      log.info("InitialContributionMessageListener: error happened while consuming : {}", e.getMessage());
    }
  }
}
