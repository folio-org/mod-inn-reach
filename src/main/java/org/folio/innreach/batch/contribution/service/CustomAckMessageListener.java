package org.folio.innreach.batch.contribution.service;

import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.folio.innreach.domain.dto.folio.inventorystorage.InstanceIterationEvent;
import org.springframework.kafka.listener.AcknowledgingMessageListener;
import org.springframework.kafka.support.Acknowledgment;

@AllArgsConstructor
public class CustomAckMessageListener implements AcknowledgingMessageListener<String, InstanceIterationEvent> {

  IMessageProcessor iMessageProcessor;

  @Override
  public void onMessage(
    ConsumerRecord<String, InstanceIterationEvent> consumerRecord, Acknowledgment acknowledgment) {

    // process message
    iMessageProcessor.processMessage(consumerRecord.key(), consumerRecord.value());

    // commit offset
    acknowledgment.acknowledge();
  }
}
