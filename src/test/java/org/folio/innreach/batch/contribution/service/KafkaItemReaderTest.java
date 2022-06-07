package org.folio.innreach.batch.contribution.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import static org.folio.innreach.batch.contribution.IterationEventReaderFactory.CONSUMER_REC_PROCESSOR;
import static org.folio.innreach.batch.contribution.IterationEventReaderFactory.ITERATION_JOB_ID_HEADER;

import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.folio.innreach.batch.KafkaItemReader;
import org.folio.innreach.domain.dto.folio.inventorystorage.InstanceIterationEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.support.serializer.JsonDeserializer;

@ExtendWith(MockitoExtension.class)
class KafkaItemReaderTest {

  @Mock(answer = Answers.RETURNS_DEEP_STUBS)
  private KafkaConsumer<String, InstanceIterationEvent> kafkaConsumer;
  @Autowired
  private ObjectMapper mapper;
  JsonDeserializer<InstanceIterationEvent> deserializer = new JsonDeserializer<>(mapper);

  @Mock
  private Iterator<ConsumerRecord<String, InstanceIterationEvent>> consumerRecords;

  @InjectMocks
  private KafkaItemReader<String, InstanceIterationEvent> reader = new KafkaItemReader<>
    (new Properties(), "topic", new StringDeserializer(), deserializer);

  @BeforeEach
  void setUp() {
    reader.setRecordProcessor(CONSUMER_REC_PROCESSOR);
  }

  @Test
  void shouldOpen() {
    reader.open();

    verify(kafkaConsumer).subscribe(any(List.class));
  }

  @Test
  void shouldRead() {
    var instanceId = UUID.randomUUID();
    var jobId = UUID.randomUUID();

    var rec = new ConsumerRecord<>(
      "topic", 0, 0, instanceId.toString(), new InstanceIterationEvent());
    rec.headers().add(ITERATION_JOB_ID_HEADER, jobId.toString().getBytes());

    when(kafkaConsumer.poll(any()).iterator()).thenReturn(List.of(rec).iterator());

    var event = reader.read();

    assertEquals(instanceId, event.getInstanceId());
    assertEquals(jobId, event.getJobId());
  }

  @Test
  void shouldReturnNullOnRead() {
    when(consumerRecords.hasNext()).thenReturn(false);

    var event = reader.read();

    assertNull(event);
  }

  @Test
  void shouldUpdate() {
    reader.update();

    verify(kafkaConsumer).commitSync();
  }

  @Test
  void shouldClose() {
    reader.close();

    verify(kafkaConsumer).close();
  }

}
