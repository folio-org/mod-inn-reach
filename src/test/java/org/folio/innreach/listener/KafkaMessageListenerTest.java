package org.folio.innreach.listener;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.UUID;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.retry.support.RetryTemplate;

import org.folio.innreach.domain.dto.folio.inventorystorage.InstanceIterationEvent;
import org.folio.innreach.domain.service.ContributionService;
import org.folio.innreach.utils.BatchProcessor;

@ExtendWith(MockitoExtension.class)
class KafkaMessageListenerTest {

  private static final String ITERATION_TOPIC = "inventory.instance-contribution";
  private static final String TENANT_ID = "test";
  private static final String EVENT_TYPE = "ITERATION";
  private static final int RETRY_ATTEMPTS = 3;

  @Mock
  private ContributionService contributionService;

  @InjectMocks
  private KafkaMessageListener messageListener;

  @Spy
  private final BatchProcessor batchProcessor = new BatchProcessor(getRetryTemplate());

  @Test
  void shouldHandleEvents() {
    var instanceId1 = UUID.randomUUID().toString();
    var instanceId2 = UUID.randomUUID().toString();

    var event1 = InstanceIterationEvent.of(instanceId1, EVENT_TYPE, TENANT_ID, instanceId1);
    var event2 = InstanceIterationEvent.of(instanceId2, EVENT_TYPE, TENANT_ID, instanceId2);

    messageListener.handleEvents(List.of(
      new ConsumerRecord<>(ITERATION_TOPIC, 0, 0, instanceId1, event1),
      new ConsumerRecord<>(ITERATION_TOPIC, 0, 0, instanceId2, event2)
    ));

    var expectedEvents = List.of(event1, event2);

    verify(contributionService).contributeInstances(expectedEvents);
    verify(batchProcessor).process(eq(expectedEvents), any(), any());
  }

  @Test
  void shouldLogFailedEvent() {
    var instanceId = UUID.randomUUID().toString();
    var event = InstanceIterationEvent.of(instanceId, EVENT_TYPE, TENANT_ID, instanceId);

    doThrow(new RuntimeException("contribution failed"))
      .when(contributionService).contributeInstances(List.of(event));

    messageListener.handleEvents(List.of(new ConsumerRecord<>(ITERATION_TOPIC, 0, 0, instanceId, event)));
    verify(contributionService, times(RETRY_ATTEMPTS)).contributeInstances(List.of(event));
  }

  private static RetryTemplate getRetryTemplate() {
    return RetryTemplate.builder()
      .maxAttempts(RETRY_ATTEMPTS)
      .build();
  }

}
