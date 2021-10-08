package org.folio.innreach.batch.contribution.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

import static org.folio.innreach.batch.contribution.IterationEventReaderFactory.CONSUMER_REC_PROCESSOR;
import static org.folio.innreach.batch.contribution.IterationEventReaderFactory.ITERATION_JOB_ID_HEADER;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiConsumer;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import org.folio.innreach.batch.KafkaItemReader;
import org.folio.innreach.domain.dto.folio.inventorystorage.InstanceIterationEvent;

@ExtendWith(MockitoExtension.class)
class KafkaItemReaderTest {

  @Spy
  private List<TopicPartition> topicPartitions;

  @Spy
  private Map<TopicPartition, Long> partitionOffsets;

  @Mock(answer = Answers.RETURNS_DEEP_STUBS)
  private KafkaConsumer<String, InstanceIterationEvent> kafkaConsumer;

  @Mock
  private Iterator<ConsumerRecord<String, InstanceIterationEvent>> consumerRecords;

  @Mock
  private BiConsumer<String, InstanceIterationEvent> consumer;

  @InjectMocks
  private KafkaItemReader<String, InstanceIterationEvent> reader;

  @BeforeEach
  void setUp() {
    openMocks(this);
    reader.setRecordProcessor(CONSUMER_REC_PROCESSOR);
  }

  @Test
  void shouldOpen() {
    reader.open();

    verify(kafkaConsumer).assign(topicPartitions);
  }

  @Test
  void shouldInitConsumerWhenOpen() {
    reader.open();

    verify(kafkaConsumer).assign(topicPartitions);
  }

  @Test
  void shouldRead() {
    var instanceId = UUID.randomUUID();

    var rec = new ConsumerRecord<>(
      "topic", 0, 0, instanceId.toString(), new InstanceIterationEvent());
    rec.headers().add(ITERATION_JOB_ID_HEADER, "test".getBytes());

    when(kafkaConsumer.poll(any()).iterator()).thenReturn(List.of(rec).iterator());

    var event = reader.read();

    assertEquals(instanceId, event.getInstanceId());
  }

  @Test
  void shouldReturnNullOnRead() {
    var instanceId = UUID.randomUUID();
    var consumerRecord = new ConsumerRecord<>(
      "topic", 0, 0,
      instanceId.toString(), new InstanceIterationEvent());

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
