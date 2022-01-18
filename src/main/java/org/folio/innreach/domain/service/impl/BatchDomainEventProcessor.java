package org.folio.innreach.domain.service.impl;

import static org.folio.innreach.config.KafkaListenerConfiguration.KAFKA_RETRY_TEMPLATE;

import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Service;

import org.folio.innreach.domain.event.DomainEvent;

@Log4j2
@Service
@RequiredArgsConstructor
public class BatchDomainEventProcessor {

  private final TenantScopedExecutionService executionService;

  @Qualifier(value = KAFKA_RETRY_TEMPLATE)
  private final RetryTemplate retryTemplate;

  public <T> void process(List<DomainEvent<T>> batch, Consumer<DomainEvent<T>> recordProcessor) {
    var tenantEventsMap = batch.stream().collect(Collectors.groupingBy(DomainEvent::getTenant));
    for (var tenantEvents : tenantEventsMap.entrySet()) {
      var tenantId = tenantEvents.getKey();
      var events = tenantEvents.getValue();

      executionService.runTenantScoped(tenantId,
        () -> processTenantEvents(tenantId, events, recordProcessor));
    }
  }

  private <T> void processTenantEvents(String tenant, List<DomainEvent<T>> events,
                                       Consumer<DomainEvent<T>> recordProcessor) {
    log.info("Processing tenant {} events {}", tenant, events.size());
    for (var event : events) {
      log.info("Processing event {}", event);
      try {
        retryTemplate.execute((c) -> {
          recordProcessor.accept(event);
          return null;
        });
      } catch (Exception e) {
        log.warn("Failed to process event {}", event, e);
      }
    }
  }
}
