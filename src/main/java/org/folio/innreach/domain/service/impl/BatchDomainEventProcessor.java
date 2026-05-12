package org.folio.innreach.domain.service.impl;

import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.innreach.external.exception.InnReachConnectionException;
import org.folio.innreach.external.exception.ServiceSuspendedException;
import org.folio.innreach.external.exception.InnReachTimeOutException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.listener.ListenerExecutionFailedException;
import org.springframework.stereotype.Service;

import org.folio.innreach.domain.event.DomainEvent;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

@Log4j2
@Service
@RequiredArgsConstructor
public class BatchDomainEventProcessor {

  private final TenantScopedExecutionService executionService;

  @Value("${innReachTenants}")
  private String innReachTenants;

  public <T> void process(List<DomainEvent<T>> batch, Consumer<DomainEvent<T>> recordProcessor) {
    var tenantEventsMap = batch.stream().collect(Collectors.groupingBy(DomainEvent::getTenant));
    for (var tenantEventsEntry : tenantEventsMap.entrySet()) {
      var tenantId = tenantEventsEntry.getKey();
      var events = tenantEventsEntry.getValue();
      if (innReachTenants.contains(tenantId)) {
        try {
          executionService.runTenantScoped(tenantId,
            () -> processTenantEvents(events, recordProcessor));
        } catch (ServiceSuspendedException | HttpClientErrorException | HttpServerErrorException | InnReachConnectionException |
               InnReachTimeOutException e) {
          log.error("process:: exception thrown on events processing: {}", e.getMessage(), e);
          throw e;
        } catch (ListenerExecutionFailedException listenerExecutionFailedException) {
          log.warn("process:: Consuming events not permitted for system user [tenantId={}]", tenantId);
        }
      } else {
        log.warn("process:: Ignoring event of unknown tenant {}", tenantId);
      }
    }
  }

  private <T> void processTenantEvents(List<DomainEvent<T>> events, Consumer<DomainEvent<T>> recordProcessor) {
    for (var event : events) {
      log.info("processTenantEvents:: Processing event type {} for {}", event.getType(), event.getClass().getSimpleName());
      try {
          recordProcessor.accept(event);
      } catch (ServiceSuspendedException | HttpClientErrorException | HttpServerErrorException | InnReachConnectionException e) {
        log.error("processTenantEvents:: Exception thrown from event processing: {}", e.getMessage(), e);
        throw e;
      } catch (Exception e) {
        log.error("processTenantEvents:: Failed to process event type {} for {}: {}",
          event.getType(), event.getClass().getSimpleName(), e.getMessage(), e);
      }
    }
  }
}
