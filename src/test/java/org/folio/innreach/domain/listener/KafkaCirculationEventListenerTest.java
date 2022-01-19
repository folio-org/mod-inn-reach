package org.folio.innreach.domain.listener;

import java.util.UUID;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.SpyBean;

import org.folio.innreach.domain.event.DomainEvent;
import org.folio.innreach.domain.event.DomainEventType;
import org.folio.innreach.domain.event.EntityChangedData;
import org.folio.innreach.domain.listener.base.KafkaTest;
import org.folio.innreach.dto.LoanDTO;

class KafkaCirculationEventListenerTest extends KafkaTest {
  private static final String TOPIC = "folio.testing.circulation.loan";

  @SpyBean
  private KafkaCirculationEventListener listener;

  @Test
  void test() {
    var event = DomainEvent.builder().recordId(UUID.randomUUID())
      .tenant("testing")
      .timestamp(System.currentTimeMillis())
      .type(DomainEventType.CREATED)
      .data(EntityChangedData.builder().newEntity(new LoanDTO().id(UUID.randomUUID())).build())
      .build();

    kafkaTemplate.send(new ProducerRecord(TOPIC, UUID.randomUUID().toString(), event));
  }
}
