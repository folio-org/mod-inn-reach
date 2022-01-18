package org.folio.innreach.domain.listener;

import java.util.UUID;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;

import org.folio.innreach.domain.event.DomainEvent;
import org.folio.innreach.domain.event.DomainEventType;
import org.folio.innreach.domain.listener.base.KafkaTest;
import org.folio.innreach.dto.LoanDTO;

class KafkaCirculationEventListenerTest extends KafkaTest {
  private static final String TOPIC = "folio.testing.circulation.loan";

  @Autowired
  private KafkaTemplate<String, DomainEvent<LoanDTO>> kafkaTemplate;

  @Test
  void test() {
    var event = DomainEvent.builder().recordId(UUID.randomUUID())
      .tenant("testing")
      .timestamp(System.currentTimeMillis())
      .type(DomainEventType.CREATED)
      .data(new LoanDTO().id(UUID.randomUUID()))
      .build();

    kafkaTemplate.send(new ProducerRecord(TOPIC, event));
  }
}
