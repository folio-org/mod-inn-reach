package org.folio.innreach.batch.contribution;

import static org.apache.kafka.clients.consumer.ConsumerConfig.GROUP_ID_CONFIG;

import java.time.Duration;
import java.util.Properties;
import java.util.UUID;
import java.util.function.Consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.kafka.support.serializer.JsonDeserializer;
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
  private final ObjectMapper mapper;

  public KafkaItemReader<String, InstanceIterationEvent> createReader(String tenantId) {
    Properties props = new Properties();
    props.putAll(kafkaProperties.buildConsumerProperties());
    props.put(GROUP_ID_CONFIG, jobProperties.getReaderGroupId());
    JsonDeserializer<InstanceIterationEvent> deserializer = new JsonDeserializer<>(InstanceIterationEvent.class, mapper);

    var topic = String.format("%s.%s.%s",
      folioEnv.getEnvironment(), tenantId, jobProperties.getReaderTopic());

    var reader = new KafkaItemReader<String, InstanceIterationEvent>(props, topic, new StringDeserializer(), deserializer);
    reader.setPollTimeout(Duration.ofSeconds(jobProperties.getReaderPollTimeoutSec()));
    reader.setRecordProcessor(CONSUMER_REC_PROCESSOR);
    return reader;
  }

  private static UUID getJobId(ConsumerRecord<String, InstanceIterationEvent> rec) {
    return UUID.fromString(new String(rec.headers().lastHeader(ITERATION_JOB_ID_HEADER).value()));
  }

}
