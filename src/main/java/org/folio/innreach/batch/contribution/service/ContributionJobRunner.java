package org.folio.innreach.batch.contribution.service;

import static java.lang.Math.max;

import static org.folio.innreach.batch.contribution.ContributionJobContextManager.beginContributionJobContext;
import static org.folio.innreach.batch.contribution.ContributionJobContextManager.endContributionJobContext;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.Future;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import com.google.common.collect.Iterables;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.stereotype.Service;

import org.folio.innreach.batch.KafkaItemReader;
import org.folio.innreach.batch.contribution.ContributionJobContext;
import org.folio.innreach.batch.contribution.ContributionJobContext.Statistics;
import org.folio.innreach.batch.contribution.IterationEventReaderFactory;
import org.folio.innreach.batch.contribution.listener.ContributionExceptionListener;
import org.folio.innreach.batch.contribution.listener.ContributionJobStatsListener;
import org.folio.innreach.config.props.ContributionJobProperties;
import org.folio.innreach.domain.dto.folio.inventorystorage.InstanceIterationEvent;
import org.folio.innreach.domain.service.ContributionService;
import org.folio.innreach.domain.service.ContributionValidationService;
import org.folio.innreach.domain.service.InventoryViewService;
import org.folio.innreach.domain.service.RecordContributionService;
import org.folio.innreach.dto.Instance;
import org.folio.innreach.dto.Item;
import org.folio.spring.FolioExecutionContext;

// TODO remove after testing
import org.folio.innreach.external.client.feign.TestClient;

@Service
@Log4j2
@RequiredArgsConstructor
public class ContributionJobRunner {

  // TODO Remove after testing.
  private final TestClient testClient;

  private static final String DE_CONTRIBUTE_INSTANCE_MSG = "De-contributing ineligible instance";

  @Qualifier("instanceExceptionListener")
  private final ContributionExceptionListener instanceExceptionListener;
  @Qualifier("itemExceptionListener")
  private final ContributionExceptionListener itemExceptionListener;
  private final ContributionJobStatsListener statsListener;
  private final RecordContributionService recordContributionService;
  private final ContributionValidationService validationService;
  private final InventoryViewService inventoryViewService;
  private final ContributionJobProperties jobProperties;
  private final ContributionService contributionService;
  private final FolioExecutionContext folioContext;
  @Qualifier("contributionRetryTemplate")
  private final RetryTemplate retryTemplate;
  private final IterationEventReaderFactory itemReaderFactory;

  private static final List<UUID> runningInitialContributions = Collections.synchronizedList(new ArrayList<>());

  @Async
  public Future<Void> runInitialContributionAsync(UUID centralServerId, String tenantId, UUID contributionId, UUID iterationJobId) {
    var context = ContributionJobContext.builder()
      .contributionId(contributionId)
      .iterationJobId(iterationJobId)
      .centralServerId(centralServerId)
      .tenantId(tenantId)
      .build();

    runInitialContribution(context);

    return new AsyncResult<>(null);
  }

  public void runInitialContribution(ContributionJobContext context) {
    log.info("Starting initial contribution job {}", context);

    var contributionId = context.getContributionId();
    try (var kafkaReader = itemReaderFactory.createReader(context.getTenantId())) {
      kafkaReader.open();

      log.info("Opened kafka reader for contribution {} for tenant {}", contributionId, context.getTenantId());

      // TODO Why call this using a BiConsumer. What is the benefit of that? Is this hiding an exception?
      run(context, (centralServerId, stats) -> {
        while (!isCanceled(contributionId)) {
          InstanceIterationEvent event = readEvent(kafkaReader);
          stats.addKafkaMessagesRead(1);

          log.info("Event read");

          var iterationJobId = context.getIterationJobId();

          if (event == null) {
            log.info("Exiting kafka message reader for contribution {} and job {}", contributionId, iterationJobId);
            return;
          }
          log.info("Processing instance iteration event = {}", event);

          var instanceId = event.getInstanceId();

           // TODO This has been the reason for some job failures, with 400k instances ~12/31 but it isn't present in _all_ job fails.
//          if (isUnknownEvent(event, iterationJobId)) {
//            log.info("Skipping unknown event, current job is {}", iterationJobId);
//            continue;
//          }

          // TODO Make this call the local web server used for the test. Same below.
         Instance instance = simulateLoadingInstance(stats);
//          Instance instance = loadInstanceWithItems(instanceId);

          // TODO Simulate this in the data.
          if (instance == null) {
            log.info("Instance is null, skipping"); // NOTE this log statement doesn't exist in the deployed code.
            continue;
          }

          // TODO Make multiple HTTP calls making a feign client to call some local http endpoint.
          simulateContribution(stats);
          //simulateContributionItems(stats);
          // TODO These two values should be the same if no messages are dropped.
          log.info("Test iterations: {} {}", stats.getRecordsTotal(), stats.getKafkaMessagesRead());
//          if (isEligibleForContribution(centralServerId, instance)) {
//            contributeInstance(centralServerId, instance, stats);
//            contributeInstanceItems(centralServerId, instance, stats);
//          } else if (isContributed(centralServerId, instance)) {
//            deContributeInstance(centralServerId, instance, stats);
//          }
        }
      });
    }
  }

  // TODO Try to simulate the stats collection. Remove after testing.
  private void simulateContribution(Statistics s) {
    makeSimulatedRequest(s);
    s.addRecordsTotal(1);
  }

  private void simulateContributionItems(Statistics s) {
    // For now just make this one.
    makeSimulatedRequest(s);
    s.addRecordsTotal(1);
  }

  private void makeSimulatedRequest(Statistics s) {
    // Simulate the try/catch around http client calls.
    try {
      log.info("Making simulated request");
      String postBody = "somerandomstring"; // Should probably make this a random length.
      URI testServer = URI.create("http://localhost:8080");
      String res = testClient.makeTestRequest(testServer, postBody);
      log.info("Response from test server: {}", res);
    } catch (Exception e) {
      log.warn("Error while simulating request: {} {}", e.getMessage(), e.getStackTrace());
    } finally {
      s.addRecordsProcessed(1);
    }
  }

  private Instance simulateLoadingInstance(Statistics s) {
    // TODO Implement this with an occasional null value. This could just randomly return null or perhaps leave that until problems have been eliminated.
    makeSimulatedRequest(s);
    return new Instance();
  }

  public void runInstanceContribution(UUID centralServerId, Instance instance) {
    log.info("Validating instance {} for contribution to central server {}", instance.getId(), centralServerId);

    boolean eligibleInstance = isEligibleForContribution(centralServerId, instance);
    boolean contributedInstance = isContributed(centralServerId, instance);

    if (!eligibleInstance && !contributedInstance) {
      log.info("Skipping ineligible and non-contributed instance");
      return;
    }

    runOngoing(centralServerId, (ctx, stats) -> {
      log.info("Starting ongoing instance contribution job {}", ctx);

      if (eligibleInstance) {
        log.info("Contributing instance");
        contributeInstance(centralServerId, instance, stats);

        if (!contributedInstance) {
          log.info("Contributing items of new instance");
          contributeInstanceItems(centralServerId, instance, stats);
        }
      } else if (contributedInstance) {
        log.info(DE_CONTRIBUTE_INSTANCE_MSG);
        deContributeInstance(centralServerId, instance, stats);
      }
    });
  }

  public void runInstanceDeContribution(UUID centralServerId, Instance deletedInstance) {
    log.info("Validating instance {} for de-contribution from central server {}", deletedInstance.getId(), centralServerId);

    if (!isContributed(centralServerId, deletedInstance)) {
      log.info("Skipping non-contributed instance");
      return;
    }

    runOngoing(centralServerId, (ctx, stats) -> {
      log.info("Starting ongoing instance de-contribution job {}", ctx);
      deContributeInstance(centralServerId, deletedInstance, stats);
    });
  }

  public void runItemContribution(UUID centralServerId, Instance instance, Item item) {
    log.info("Validating item {} for contribution to central server {}", item.getId(), centralServerId);

    boolean eligibleItem = isEligibleForContribution(centralServerId, item);
    boolean contributedItem = isContributed(centralServerId, instance, item);

    if (!eligibleItem && !contributedItem) {
      log.info("Skipping ineligible and non-contributed item");
      return;
    }

    runOngoing(centralServerId, (ctx, stats) -> {
      log.info("Starting ongoing item contribution job {}", ctx);

      if (isEligibleForContribution(centralServerId, instance)) {
        log.info("Re-contributing instance to update bib status");
        contributeInstance(centralServerId, instance, stats);

        if (eligibleItem) {
          log.info("Contributing item");
          contributeItem(centralServerId, instance.getHrid(), item, stats);
        } else if (contributedItem) {
          log.info("De-contributing item");
          deContributeItem(centralServerId, item, stats);
        }
      } else if (contributedItem) {
        log.info(DE_CONTRIBUTE_INSTANCE_MSG);
        deContributeInstance(centralServerId, instance, stats);
      }
    });
  }

  public void runItemMove(UUID centralServerId, Instance newInstance, Instance oldInstance, Item item) {
    log.info("Validating item {} for moving to a new instance {} on central server {}", item.getId(), newInstance.getId(), centralServerId);

    boolean eligibleItem = isEligibleForContribution(centralServerId, item);
    boolean contributedItem = isContributed(centralServerId, oldInstance, item);

    if (!eligibleItem && !contributedItem) {
      log.info("Skipping ineligible and non-contributed item");
      return;
    }

    runOngoing(centralServerId, (ctx, stats) -> {
      log.info("Starting ongoing item move job {}", ctx);

      // de-contribute item and update old instance
      if (contributedItem) {
        if (isEligibleForContribution(centralServerId, oldInstance)) {
          log.info("De-contributing item from old instance");
          deContributeItem(centralServerId, item, stats);

          log.info("Re-contributing old instance to update bib status");
          contributeInstance(centralServerId, oldInstance, stats);
        } else {
          log.info("De-contributing old instance");
          deContributeInstance(centralServerId, oldInstance, stats);
        }
      }

      // contribute item to a new instance
      if (isEligibleForContribution(centralServerId, newInstance)) {
        log.info("Re-contributing new instance to update bib status");
        contributeInstance(centralServerId, newInstance, stats);

        if (eligibleItem) {
          log.info("Contributing item to new instance");
          contributeItem(centralServerId, newInstance.getHrid(), item, stats);
        }
      }
    });
  }

  public void runItemDeContribution(UUID centralServerId, Instance instance, Item deletedItem) {
    log.info("Validating item {} for de-contribution from central server {}", deletedItem.getId(), centralServerId);
    if (!isContributed(centralServerId, instance, deletedItem)) {
      log.info("Skipping non-contributed item");
      return;
    }

    runOngoing(centralServerId, (context, stats) -> {
      log.info("Starting ongoing item de-contribution job {}", context);

      if (isEligibleForContribution(centralServerId, instance)) {
        log.info("De-contributing item");
        deContributeItem(centralServerId, deletedItem, stats);

        log.info("Re-contributing instance to update bib status");
        contributeInstance(centralServerId, instance, stats);
      } else {
        log.info(DE_CONTRIBUTE_INSTANCE_MSG);
        deContributeInstance(centralServerId, instance, stats);
      }
    });
  }

  public void cancelJobs() {
    log.info("Cancelling unfinished contributions...");
    contributionService.cancelAll();
    runningInitialContributions.clear();
  }

  public void cancelInitialContribution(UUID contributionId) {
    log.info("Cancelling initial contribution job {}", contributionId);
    runningInitialContributions.remove(contributionId);
  }

  private boolean isEligibleForContribution(UUID centralServerId, Instance instance) {
    return validationService.isEligibleForContribution(centralServerId, instance);
  }

  private boolean isContributed(UUID centralServerId, Instance instance) {
    return recordContributionService.isContributed(centralServerId, instance);
  }

  private boolean isEligibleForContribution(UUID centralServerId, Item item) {
    return validationService.isEligibleForContribution(centralServerId, item);
  }

  private boolean isContributed(UUID centralServerId, Instance instance, Item item) {
    return recordContributionService.isContributed(centralServerId, instance, item);
  }

  private void contributeInstanceItems(UUID centralServerId, Instance instance, Statistics stats) {
    var bibId = instance.getHrid();
    var items = instance.getItems().stream()
      .filter(i -> isEligibleForContribution(centralServerId, i))
      .collect(Collectors.toList());

    int chunkSize = max(jobProperties.getChunkSize(), 1);

    StreamSupport.stream(Iterables.partition(items, chunkSize).spliterator(), false)
      .forEach(itemsChunk -> contributeItemsChunk(centralServerId, bibId, itemsChunk, stats));
  }

  private void contributeItem(UUID centralServerId, String bibId, Item item, Statistics stats) {
    contributeItemsChunk(centralServerId, bibId, List.of(item), stats);
  }

  private void contributeItemsChunk(UUID centralServerId, String bibId, List<Item> items, Statistics stats) {
    var itemsCount = items.size();
    try {
      stats.addRecordsTotal(itemsCount);
      recordContributionService.contributeItems(centralServerId, bibId, items);
      stats.addRecordsContributed(itemsCount);
    } catch (Exception e) {
      // not possible to guess what item failed when the chunk of multiple items is being contributed
      var recordId = items.size() == 1 ? items.get(0).getId() : null;
      itemExceptionListener.logWriteError(e, recordId);
    } finally {
      stats.addRecordsProcessed(itemsCount);
      statsListener.updateStats(stats);
    }
  }

  private void contributeInstance(UUID centralServerId, Instance instance, Statistics stats) {
    try {
      stats.addRecordsTotal(1);
      recordContributionService.contributeInstance(centralServerId, instance);
      stats.addRecordsContributed(1);
    } catch (Exception e) {
      instanceExceptionListener.logWriteError(e, instance.getId());
    } finally {
      stats.addRecordsProcessed(1);
      updateStats(stats);
    }
  }

  private void deContributeInstance(UUID centralServerId, Instance instance, Statistics stats) {
    try {
      stats.addRecordsTotal(1);
      recordContributionService.deContributeInstance(centralServerId, instance);
      stats.addRecordsDeContributed(1);
    } catch (Exception e) {
      instanceExceptionListener.logWriteError(e, instance.getId());
    } finally {
      stats.addRecordsProcessed(1);
      updateStats(stats);
    }
  }

  private void deContributeItem(UUID centralServerId, Item item, Statistics stats) {
    try {
      stats.addRecordsTotal(1);
      recordContributionService.deContributeItem(centralServerId, item);
      stats.addRecordsDeContributed(1);
    } catch (Exception e) {
      itemExceptionListener.logWriteError(e, item.getId());
    } finally {
      stats.addRecordsProcessed(1);
      updateStats(stats);
    }
  }

  private void runOngoing(UUID centralServerId, BiConsumer<ContributionJobContext, Statistics> processor) {
    var contribution = contributionService.createOngoingContribution(centralServerId);
    var context = ContributionJobContext.builder()
      .contributionId(contribution.getId())
      .centralServerId(centralServerId)
      .tenantId(folioContext.getTenantId())
      .build();

    var stats = new Statistics();
    try {
      beginContributionJobContext(context);

      processor.accept(context, stats);
    } catch (Exception e) {
      log.warn("Failed to run contribution job for central server {}", centralServerId, e);
      throw e;
    } finally {
      completeContribution(context, stats);
      endContributionJobContext();
    }
  }

  private void run(ContributionJobContext context, BiConsumer<UUID, Statistics> processor) {
    var stats = new Statistics();
    var centralServerId = context.getCentralServerId();
    var contributionId = context.getContributionId();
    try {
      runningInitialContributions.add(contributionId);
      beginContributionJobContext(context);
      processor.accept(centralServerId, stats);
    } catch (Exception e) {
      log.warn("Failed to run contribution job for central server {}", centralServerId, e);
      throw e;
    } finally {
      if (!isCanceled(contributionId)) {
        completeContribution(context, stats);
        runningInitialContributions.remove(contributionId);
      }
      endContributionJobContext();
    }
  }

  private boolean isUnknownEvent(InstanceIterationEvent event, UUID iterationJobId) {
    return !Objects.equals(event.getJobId(), iterationJobId);
  }

  private void completeContribution(ContributionJobContext context, Statistics stats) {
    try {
      // TODO Comment this out for now because it wants to write to the db which we don't care about.
      //contributionService.completeContribution(context.getContributionId());
      log.info("Completed contribution job {}", context);
      log.info("Kafka messages read: {}", stats.getKafkaMessagesRead());
      log.info("Records processed total: {}", stats.getRecordsTotal());
    } catch (Exception e) {
      log.warn("Failed to complete contribution job {}", context, e);
    }
  }

  private boolean isCanceled(UUID contributionId) {
    return !runningInitialContributions.contains(contributionId);
  }

  private void updateStats(Statistics stats) {
    statsListener.updateStats(stats);
  }

  private Instance loadInstanceWithItems(UUID instanceId) {
    Instance instance = null;
    try {
      instance = retryTemplate.execute(r -> inventoryViewService.getInstance(instanceId));
    } catch (Exception e) {
      instanceExceptionListener.logProcessError(e, instanceId);
    }
    return instance;
  }

  private InstanceIterationEvent readEvent(KafkaItemReader<String, InstanceIterationEvent> kafkaReader) {
    try {
      return retryTemplate.execute(r -> kafkaReader.read());
    } catch (Exception e) {
      instanceExceptionListener.logReaderError(e);
      throw new IllegalStateException("Can't read instance iteration event: " + e.getMessage(), e);
    }
  }

}
