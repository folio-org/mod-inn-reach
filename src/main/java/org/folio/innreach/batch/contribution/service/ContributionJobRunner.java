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
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Iterables;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.innreach.external.exception.ServiceSuspendedException;
import org.folio.innreach.batch.contribution.InitialContributionJobConsumerContainer;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.retry.support.RetryTemplate;
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

  Integer totalRecords;
  private static ConcurrentHashMap<String, Integer> recordsProcessed = new ConcurrentHashMap<>();

  public void startInitialContribution(UUID centralServerId, String tenantId, UUID contributionId, UUID iterationJobId, Integer numberOfRecords) {
    var context = ContributionJobContext.builder()
      .contributionId(contributionId)
      .iterationJobId(iterationJobId)
      .centralServerId(centralServerId)
      .tenantId(tenantId)
      .build();

    totalRecords = numberOfRecords;

    recordsProcessed.put("testTenant", 0);

    InitialContributionJobConsumerContainer tempKafkaConsumer = itemReaderFactory.createInitialContributionConsumerContainer(tenantId);

    InitialContributionMessageListener initialContributionMessageListener = new InitialContributionMessageListener(new ContributionProcessor(this), context, new Statistics());

    tempKafkaConsumer.tryStartOrCreateConsumer(initialContributionMessageListener);
  }

  public void runInitialContribution(ContributionJobContext context, InstanceIterationEvent event, Statistics stats) {
    recordsProcessed.put(context.getTenantId(), recordsProcessed.get(context.getTenantId())+1);

    var contributionId = context.getContributionId();

    var iterationJobId = context.getIterationJobId();

    if (event == null) {
      log.info("Exiting kafka message reader for contribution {} and job {}", contributionId, iterationJobId);
      return;
    }
    log.info("Processing instance iteration event = {}", event);

    var instanceId = event.getInstanceId();

    if (isUnknownEvent(event, iterationJobId)) {
      log.info("Skipping unknown event, current job is {}", iterationJobId);
      return;
    }

    Instance instance = loadInstanceWithItems(instanceId);

    if (instance == null) {
      log.info("Instance is null, skipping");
      return;
    }

    var centralServerId = context.getCentralServerId();

    if (isEligibleForContribution(centralServerId, instance)) {
      contributeInstance(centralServerId, instance, stats);
      contributeInstanceItems(centralServerId, instance, stats);
    } else if (isContributed(centralServerId, instance)) {
      deContributeInstance(centralServerId, instance, stats);
    }
    if (Objects.equals(recordsProcessed.get(context.getTenantId()), totalRecords)) {
      completeContribution(context, stats);
      endContributionJobContext();
//      InitialContributionJobConsumerContainer.stopConsumer(topic);
    }
  }

  // TODO Try to simulate the stats collection. Remove after testing.
  public void simulateContribution() {
    makeSimulatedRequest();
//    s.addRecordsTotal(1);
  }

  private void simulateContributionItems() {
    // For now just make this one.
    makeSimulatedRequest();
//    s.addRecordsTotal(1);
  }

  private void makeSimulatedRequest(Statistics s) {
    // Simulate the try/catch around http client calls.
    try {
      log.info("Making simulated request");
      String postBody = "somerandomstring"; // Should probably make this a random length.
      URI testServer = URI.create("http://localhost:8080");
      var res = testClient.makeTestRequest(testServer, postBody);
      log.info("Response from test server: {}", res);
    } catch (Exception e) {
      log.warn("Error while simulating request: {} {}", e.getMessage(), e.getStackTrace());
    } finally {
      s.addRecordsProcessed(1);
    }
  }

  private void makeSimulatedRequest() {
    // Simulate the try/catch around http client calls.
    try {
      recordsProcessed.put("testTenant", recordsProcessed.get("testTenant")+1);
      log.info("Making simulated request");
      String postBody = "somerandomstring"; // Should probably make this a random length.
      URI testServer = URI.create("http://localhost:8080");
      var res = testClient.makeTestRequest(testServer, postBody);
      log.info("Response from test server: {}", res);
      var resObject = new ObjectMapper().writeValueAsString(res.getBody());
      if (resObject.contains("Contribution to d2irm is currently suspended")) {
        throw new ServiceSuspendedException("Contribution to d2irm is currently suspended");
      }
      if (Objects.equals(recordsProcessed.get("testTenant"), totalRecords)) {
        InitialContributionJobConsumerContainer.stopConsumer("folio.contrib.tester.innreach");
      }

    } catch (ServiceSuspendedException ex) {
      throw new ServiceSuspendedException(ex.getMessage());
    } catch (Exception e) {
      log.warn("Error while simulating request: {} {}", e.getMessage(), e.getStackTrace());
    }
  }

  public Instance simulateLoadingInstance(Statistics s) {
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
      contributionService.completeContribution(context.getContributionId());
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
