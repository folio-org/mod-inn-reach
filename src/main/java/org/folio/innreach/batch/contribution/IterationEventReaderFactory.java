package org.folio.innreach.batch.contribution;

import static java.util.List.of;

import java.time.Duration;
import java.util.HashMap;
import java.util.Properties;
import java.util.UUID;
import java.util.function.Consumer;

import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.TopicPartition;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.stereotype.Component;

import org.folio.innreach.batch.KafkaItemReader;
import org.folio.innreach.config.props.ContributionJobProperties;
import org.folio.innreach.config.props.FolioEnvironment;
import org.folio.innreach.domain.dto.folio.inventorystorage.InstanceIterationEvent;

@Component
@RequiredArgsConstructor
public class IterationEventReaderFactory {

  public static final String ITERATION_JOB_ID_HEADER = "iteration-job-id";

  public static final Consumer<ConsumerRecord<String, InstanceIterationEvent>> CONSUMER_REC_PROCESSOR =
    rec -> {
      var event = rec.value();
      var jobId = getJobId(rec);
      event.setInstanceId(UUID.fromString(rec.key()));
      event.setJobId(jobId);
    };

  private final KafkaProperties kafkaProperties;
  private final FolioEnvironment folioEnv;
  private final ContributionJobProperties jobProperties;

  public KafkaItemReader<String, InstanceIterationEvent> createReader(String tenantId) {
    Properties props = new Properties();
    props.putAll(kafkaProperties.buildConsumerProperties());

    var topic = String.format("%s.%s.%s",
      folioEnv.getEnvironment(), tenantId, jobProperties.getReaderTopic());

    var reader = new KafkaItemReader<String, InstanceIterationEvent>(props, of(new TopicPartition(topic, 0)));
    reader.setPollTimeout(Duration.ofSeconds(jobProperties.getReaderPollTimeoutSec()));
    reader.setPartitionOffsets(new HashMap<>());
    reader.setRecordProcessor(CONSUMER_REC_PROCESSOR);
    return reader;
  }

  private static UUID getJobId(ConsumerRecord<String, InstanceIterationEvent> rec) {
    return UUID.fromString(new String(rec.headers().lastHeader(ITERATION_JOB_ID_HEADER).value()));
  }

}
