package org.folio.innreach.batch;

import java.time.Duration;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.function.BiConsumer;

import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.support.AbstractItemStreamItemReader;

/**
 * <p>
 * An {@link org.springframework.batch.item.ItemReader} implementation for Apache Kafka.
 * Uses a {@link KafkaConsumer} to read data from a given topic.
 * Multiple partitions within the same topic can be assigned to this reader.
 * </p>
 *
 * <p>
 * Since {@link KafkaConsumer} is not thread-safe, this reader is not thread-safe.
 * </p>
 */
@Setter
@RequiredArgsConstructor
public class KafkaItemReader<K, V> extends AbstractItemStreamItemReader<V> {

  private static final long DEFAULT_POLL_TIMEOUT = 30L;

  private final Properties consumerProperties;
  private final List<TopicPartition> topicPartitions;

  private Map<TopicPartition, Long> partitionOffsets;
  private KafkaConsumer<K, V> kafkaConsumer;
  private Iterator<ConsumerRecord<K, V>> consumerRecords;
  private BiConsumer<K, V> recordProcessor;
  private Duration pollTimeout = Duration.ofSeconds(DEFAULT_POLL_TIMEOUT);

  @Override
  public void open(ExecutionContext executionContext) {
    if (kafkaConsumer == null) {
      kafkaConsumer = new KafkaConsumer<>(consumerProperties);
    }

    kafkaConsumer.assign(topicPartitions);
    partitionOffsets.forEach(kafkaConsumer::seek);
  }

  @Override
  public V read() {
    if (consumerRecords == null || !consumerRecords.hasNext()) {
      consumerRecords = kafkaConsumer.poll(pollTimeout).iterator();
    }

    if (consumerRecords.hasNext()) {
      ConsumerRecord<K, V> rec = consumerRecords.next();
      recordProcessor.accept(rec.key(), rec.value());
      partitionOffsets.put(new TopicPartition(rec.topic(), rec.partition()), rec.offset());
      return rec.value();
    } else {
      return null;
    }
  }

  @Override
  public void update(ExecutionContext executionContext) {
    kafkaConsumer.commitSync();
  }

  @Override
  public void close() {
    if (kafkaConsumer != null) {
      kafkaConsumer.close();
    }
  }

}
