package org.folio.innreach.domain.listener;

import lombok.extern.log4j.Log4j2;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.folio.innreach.domain.dto.folio.inventorystorage.InstanceIterationEvent;
import org.folio.innreach.domain.event.DomainEvent;
import org.folio.innreach.external.client.InnReachAuthClient;
import org.folio.innreach.it.base.BaseTenantIntegrationTest;
import org.folio.innreach.repository.JobExecutionStatusRepository;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.awaitility.Awaitility.await;
import static org.folio.innreach.domain.listener.KafkaInitialContributionEventListener.ITERATION_JOB_ID_HEADER;
import static org.folio.innreach.domain.listener.KafkaListenersConstants.INITIAL_CONTRIBUTION_TOPIC;
import static org.junit.jupiter.api.Assertions.assertEquals;

@Log4j2
class KafkaInitialContributionEventListenerTest extends BaseTenantIntegrationTest {

  private static KafkaTemplate<String, DomainEvent> kafkaTemplate;

  @MockitoSpyBean
  private JobExecutionStatusRepository jobExecutionStatusRepository;

  @MockitoBean
  protected InnReachAuthClient innReachAuthClient;

  @BeforeAll
  static void setUp() {
    kafkaTemplate = buildKafkaTemplate();
  }

  @Test
  void testInitialContributionEvent() {
    var event = InstanceIterationEvent.of(null, "iterate", TEST_TENANT, null);
    List<Header> headers = new ArrayList<>();
    UUID jobId = UUID.randomUUID();
    headers.add(new RecordHeader(ITERATION_JOB_ID_HEADER, jobId.toString().getBytes()));
    kafkaTemplate.send(new ProducerRecord(INITIAL_CONTRIBUTION_TOPIC, null, UUID.randomUUID().toString(), event, headers));
    await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
      var jobExecutionStatus = jobExecutionStatusRepository.findAll();
      assertEquals(1, jobExecutionStatus.size());
      assertEquals(jobExecutionStatus.get(0).getJobId(), jobId);
    });
  }

}
