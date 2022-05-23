package org.folio.innreach.batch;

import java.time.Duration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.function.Consumer;

import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;

/**
 * <p>
 * Apache Kafka item reader.
 * Uses a {@link KafkaConsumer} to read data from a given topic.
 * </p>
 *
 * <p>
 * Since {@link KafkaConsumer} is not thread-safe, this reader is not thread-safe.
 * </p>
 */
@Setter
@RequiredArgsConstructor
public class KafkaItemReader<K, V> implements AutoCloseable {

  private static final long DEFAULT_POLL_TIMEOUT = 30L;

  private final Properties consumerProperties;
  private final String topic;
  private final Map<TopicPartition, Long> partitionOffsets = new HashMap<>();

  private KafkaConsumer<K, V> kafkaConsumer;
  private Iterator<ConsumerRecord<K, V>> consumerRecords;
  private Consumer<ConsumerRecord<K, V>> recordProcessor;
  private Duration pollTimeout = Duration.ofSeconds(DEFAULT_POLL_TIMEOUT);

  public void open() {
    if (kafkaConsumer == null) {
      kafkaConsumer = new KafkaConsumer<>(consumerProperties);
    }

    kafkaConsumer.subscribe(List.of(topic));
  }

  public V read() {
    if (consumerRecords == null || !consumerRecords.hasNext()) {
      consumerRecords = kafkaConsumer.poll(pollTimeout).iterator();
    }

    if (consumerRecords.hasNext()) {
      ConsumerRecord<K, V> rec = consumerRecords.next();
      recordProcessor.accept(rec);
      partitionOffsets.put(new TopicPartition(rec.topic(), rec.partition()), rec.offset());
      return rec.value();
    } else {
      return null;
    }
  }

  public void update() {
    kafkaConsumer.commitSync();
  }

  @Override
  public void close() {
    if (kafkaConsumer != null) {
      kafkaConsumer.close();
    }
  }

}
