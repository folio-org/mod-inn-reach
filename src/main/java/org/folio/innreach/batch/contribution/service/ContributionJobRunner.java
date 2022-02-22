package org.folio.innreach.batch.contribution.service;

import static java.lang.Math.max;

import static org.folio.innreach.batch.contribution.ContributionJobContextManager.beginContributionJobContext;
import static org.folio.innreach.batch.contribution.ContributionJobContextManager.endContributionJobContext;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import com.google.common.collect.Iterables;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
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

@Service
@Log4j2
@RequiredArgsConstructor
public class ContributionJobRunner {

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
  @Qualifier("contributionRetryTemplate")
  private final RetryTemplate retryTemplate;
  private final IterationEventReaderFactory itemReaderFactory;

  public void runInitialContributionAsync(UUID centralServerId, String tenantId, UUID contributionId, UUID iterationJobId) {
    var context = ContributionJobContext.builder()
      .contributionId(contributionId)
      .iterationJobId(iterationJobId)
      .centralServerId(centralServerId)
      .tenantId(tenantId)
      .build();

    CompletableFuture.runAsync(() -> runInitialContribution(context));
  }

  public void runInitialContribution(ContributionJobContext context) {
    log.info("Starting initial contribution job {}", context);

    try (var kafkaReader = itemReaderFactory.createReader(context.getTenantId())) {
      kafkaReader.open();

      run(context, (centralServerId, stats) -> {
        while (true) {
          var event = readEvent(kafkaReader);
          if (event == null) {
            return;
          }

          var instanceId = event.getInstanceId();
          var iterationJobId = context.getIterationJobId();

          if (isUnknownEvent(event, iterationJobId)) {
            log.info("Skipping unknown event, current job is {}", iterationJobId);
            continue;
          }

          log.info("Processing instance iteration event = {}", event);

          var instance = loadInstanceWithItems(instanceId);
          if (instance == null) {
            continue;
          }

          if (validationService.isEligibleForContribution(centralServerId, instance)) {
            contributeInstance(centralServerId, instance, stats);
            contributeInstanceItems(centralServerId, instance, stats);
          } else if (recordContributionService.isContributed(centralServerId, instance)) {
            deContributeInstance(centralServerId, instance, stats);
          }
        }
      });
    }
  }

  public void runInstanceContribution(UUID centralServerId, Instance instance) {
    boolean eligibleInstance = validationService.isEligibleForContribution(centralServerId, instance);
    boolean contributedInstance = recordContributionService.isContributed(centralServerId, instance);
    if (!eligibleInstance && !contributedInstance) {
      log.info("Skipping ineligible and non-contributed item");
      return;
    }

    runOngoing(centralServerId, (ctx, stats) -> {
      log.info("Starting ongoing instance contribution job {}", ctx);

      if (eligibleInstance) {
        contributeInstance(centralServerId, instance, stats);

        if (!contributedInstance) {
          contributeInstanceItems(centralServerId, instance, stats);
        }
      } else if (contributedInstance) {
        deContributeInstance(centralServerId, instance, stats);
      }
    });
  }

  public void runInstanceDeContribution(UUID centralServerId, Instance deletedInstance) {
    if (!recordContributionService.isContributed(centralServerId, deletedInstance)) {
      log.info("Skipping non-contributed instance");
      return;
    }

    log.info("Starting ongoing contribution job on instance {} delete for central server {}", deletedInstance.getId(), centralServerId);

    runOngoing(centralServerId, (ctx, stats) -> deContributeInstance(centralServerId, deletedInstance, stats));
  }

  public void runItemContribution(UUID centralServerId, Instance instance, Item updatedItem) {
    boolean eligibleItem = validationService.isEligibleForContribution(centralServerId, updatedItem);
    boolean contributedItem = recordContributionService.isContributed(centralServerId, instance, updatedItem);
    if (!eligibleItem && !contributedItem) {
      log.info("Skipping ineligible and non-contributed item");
      return;
    }

    runOngoing(centralServerId, (ctx, stats) -> {
      log.info("Starting ongoing contribution job {} on item update", ctx);

      if (eligibleItem) {
        contributeItem(centralServerId, instance.getHrid(), updatedItem, stats);
      } else if (contributedItem) {
        deContributeItem(centralServerId, updatedItem, stats);
      }

      // re-contributing instance to update item count
      contributeInstance(centralServerId, instance, stats);
    });
  }

  public void runItemMove(UUID centralServerId, Instance newInstance, Instance oldInstance, Item item) {
    boolean eligibleItem = validationService.isEligibleForContribution(centralServerId, item);
    boolean contributedItem = recordContributionService.isContributed(centralServerId, oldInstance, item);
    if (!eligibleItem && !contributedItem) {
      log.info("Skipping ineligible item");
      return;
    }

    runOngoing(centralServerId, (ctx, stats) -> {
      log.info("Starting ongoing contribution job {} on item moved", ctx);

      if (contributedItem) {
        deContributeItem(centralServerId, item, stats);
        // re-contributing old instance to update item count
        contributeInstance(centralServerId, oldInstance, stats);
      }

      if (eligibleItem) {
        contributeItem(centralServerId, newInstance.getHrid(), item, stats);
        // re-contributing new instance to update item count
        contributeInstance(centralServerId, newInstance, stats);
      }
    });
  }

  public void runItemDeContribution(UUID centralServerId, Instance instance, Item deletedItem) {
    if (!recordContributionService.isContributed(centralServerId, instance, deletedItem)) {
      log.info("Skipping non-contributed item");
      return;
    }

    runOngoing(centralServerId, (context, stats) -> {
      log.info("Starting ongoing contribution job {} on item delete {}", context);

      deContributeItem(centralServerId, deletedItem, stats);

      // re-contributing instance to update item count
      contributeInstance(centralServerId, instance, stats);
    });
  }

  public void cancelJobs() {
    log.info("Cancelling unfinished contributions...");
    contributionService.cancelAll();
  }

  private void contributeInstanceItems(UUID centralServerId, Instance instance, Statistics stats) {
    var bibId = instance.getHrid();
    var items = instance.getItems().stream()
      .filter(i -> validationService.isEligibleForContribution(centralServerId, i))
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
      .build();

    var stats = new Statistics();
    try {
      beginContributionJobContext(context);

      processor.accept(context, stats);
    } catch (Exception e) {
      log.warn("Failed to run contribution job for central server {}", centralServerId, e);
      throw e;
    } finally {
      completeContribution(context);
      endContributionJobContext();
    }
  }

  private void run(ContributionJobContext context, BiConsumer<UUID, Statistics> processor) {
    var stats = new Statistics();
    var centralServerId = context.getCentralServerId();
    try {
      beginContributionJobContext(context);

      processor.accept(centralServerId, stats);
    } catch (Exception e) {
      log.warn("Failed to run contribution job for central server {}", centralServerId, e);
      throw e;
    } finally {
      completeContribution(context);
      endContributionJobContext();
    }
  }

  private boolean isUnknownEvent(InstanceIterationEvent event, UUID iterationJobId) {
    return !Objects.equals(event.getJobId(), iterationJobId);
  }

  private void completeContribution(ContributionJobContext context) {
    contributionService.completeContribution(context.getContributionId());
    log.info("Completed contribution job {}", context);
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
