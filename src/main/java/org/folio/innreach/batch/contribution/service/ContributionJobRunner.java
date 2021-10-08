package org.folio.innreach.batch.contribution.service;

import static java.util.List.of;

import static org.folio.innreach.batch.contribution.ContributionJobContextManager.beginContributionJobContext;
import static org.folio.innreach.batch.contribution.ContributionJobContextManager.endContributionJobContext;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.stream.StreamSupport;

import com.google.common.collect.Iterables;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.kafka.common.TopicPartition;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Service;

import org.folio.innreach.batch.KafkaItemReader;
import org.folio.innreach.batch.contribution.ContributionJobContext;
import org.folio.innreach.batch.contribution.ContributionJobContext.Statistics;
import org.folio.innreach.batch.contribution.listener.ContributionExceptionListener;
import org.folio.innreach.batch.contribution.listener.ContributionJobStatsListener;
import org.folio.innreach.config.props.ContributionJobProperties;
import org.folio.innreach.config.props.FolioEnvironment;
import org.folio.innreach.domain.dto.folio.inventorystorage.InstanceIterationEvent;
import org.folio.innreach.domain.service.ContributionService;
import org.folio.innreach.dto.Instance;
import org.folio.innreach.dto.Item;

@Service
@Log4j2
@RequiredArgsConstructor
public class ContributionJobRunner {

  public static final BiConsumer<String, InstanceIterationEvent> CONSUMER_REC_PROCESSOR =
    (String key, InstanceIterationEvent value) -> value.setInstanceId(UUID.fromString(key));

  @Qualifier("instanceExceptionListener")
  private final ContributionExceptionListener instanceExceptionListener;
  @Qualifier("itemExceptionListener")
  private final ContributionExceptionListener itemExceptionListener;
  private final ContributionJobStatsListener statsListener;
  private final InstanceLoader instanceLoader;
  private final InstanceContributor instanceContributor;
  private final ItemContributor itemContributor;
  private final ContributionJobProperties jobProperties;
  private final KafkaProperties kafkaProperties;
  private final FolioEnvironment folioEnv;
  private final ContributionService contributionService;
  private final RetryTemplate retryTemplate;

  public void runAsync(UUID centralServerId, String tenantId, UUID contributionId, UUID iterationJobId) {
    var context = ContributionJobContext.builder()
      .contributionId(contributionId)
      .iterationJobId(iterationJobId)
      .centralServerId(centralServerId)
      .tenantId(tenantId)
      .build();

    runAsync(context);
  }

  public void runAsync(ContributionJobContext context) {
    CompletableFuture.runAsync(() -> run(context));
  }

  public void run(ContributionJobContext context) {
    log.info("Starting contribution job {}", context);

    var stats = new Statistics();
    try (var kafkaReader =
           kafkaReader(kafkaProperties, jobProperties, folioEnv, context.getTenantId())) {

      beginContributionJobContext(context);

      kafkaReader.open();

      while (true) {
        var event = readEvent(kafkaReader, stats);
        if (event == null) {
          return;
        }

        Instance instance = loadInstance(event, stats);
        if (instance == null) {
          continue;
        }

        contributeInstance(instance, stats);

        contributeInstanceItems(instance, stats);
      }
    } catch (Exception e) {
      log.warn("Failed to run contribution job for central server {}", context.getCentralServerId(), e);
      throw e;
    } finally {
      completeContribution(context);
      endContributionJobContext();
    }
  }

  public void cancelJobs() {
    log.info("Cancelling unfinished contributions...");
    contributionService.cancelAll();
  }

  private void contributeInstanceItems(Instance instance, Statistics stats) {
    var bibId = instance.getHrid();
    var items = instance.getItems();

    StreamSupport.stream(Iterables.partition(items, jobProperties.getChunkSize()).spliterator(), false)
      .forEach(itemsChunk -> contributeItems(bibId, itemsChunk, stats));
  }

  private void contributeItems(String bibId, List<Item> items, Statistics stats) {
    try {
      stats.readCount += items.size();
      var writeCount = retryTemplate.execute(r -> itemContributor.contributeItems(bibId, items));
      stats.writeCount += writeCount;
      stats.writeSkipCount += items.size() - writeCount;
    } catch (Exception e) {
      stats.writeSkipCount += items.size();
      itemExceptionListener.logWriteError(e, null);
    } finally {
      statsListener.updateStats(stats);
    }
  }

  private void completeContribution(ContributionJobContext context) {
    contributionService.completeContribution(context.getCentralServerId());
    log.info("Completed contribution job {}", context);
  }

  private void updateStats(Statistics stats) {
    statsListener.updateStats(stats);
  }

  private void contributeInstance(Instance instance, Statistics stats) {
    try {
      instanceContributor.contributeInstance(instance);
      stats.writeCount++;
    } catch (Exception e) {
      stats.writeSkipCount++;
      instanceExceptionListener.logWriteError(e, instance.getId());
    } finally {
      updateStats(stats);
    }
  }

  private Instance loadInstance(InstanceIterationEvent event, Statistics stats) {
    Instance instance = null;
    try {
      instance = retryTemplate.execute(r -> instanceLoader.process(event));
    } catch (Exception e) {
      instanceExceptionListener.logProcessError(e, event.getInstanceId());
    }
    return instance;
  }

  private InstanceIterationEvent readEvent(KafkaItemReader<String, InstanceIterationEvent> kafkaReader, Statistics stats) {
    try {
      var event = retryTemplate.execute(r -> kafkaReader.read());

      if (event != null) {
        stats.readCount++;
        updateStats(stats);
      }

      return event;
    } catch (Exception e) {
      instanceExceptionListener.logReaderError(e);
      throw new RuntimeException("Can't read instance iteration event", e);
    }
  }

  private static KafkaItemReader<String, InstanceIterationEvent> kafkaReader(KafkaProperties kafkaProperties,
                                                                      ContributionJobProperties jobProperties,
                                                                      FolioEnvironment folioEnv, String tenantId) {
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

}
