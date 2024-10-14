package org.folio.innreach.domain.service.impl;

  import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.innreach.domain.event.DomainEvent;
import org.folio.innreach.domain.service.KafkaEventProcessorService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Service
@Log4j2
@RequiredArgsConstructor
public class KafkaEventProcessorServiceImpl implements KafkaEventProcessorService {

  private final TenantScopedExecutionService executionService;
  @Value("${innReachTenants}")
  private String innReachTenants;

  @Override
  public <T> void process(T event, Consumer<T> eventProcessor, String tenant) {
    try {
      log.info("process: event {} , tenant {} ", event, tenant);
      executionService.runTenantScoped(tenant,
        () -> eventProcessor.accept(event));
    } catch (Exception ex) {
      log.warn("Unable to save kafka event into outbox table ", ex);
    }
  }

  @Override
  public <T> void process(List<DomainEvent<T>> events, BiConsumer<List<DomainEvent<T>>, String> eventProcessor) {
    try {
      Map<String, List<DomainEvent<T>>> tenantMap = events.stream().collect(Collectors.groupingBy(DomainEvent::getTenant));
      tenantMap.forEach((tenant, eventList) -> {
        log.debug("process:: processing eventList {} for tenant {}", eventList, tenant);
        if (innReachTenants.contains(tenant)) {
          executionService.runTenantScoped(tenant,
            () -> eventProcessor.accept(eventList, tenant));
        } else {
          log.info("process:: Ignoring event of unknown tenant {}", tenant);
        }
      });
    } catch (Exception ex) {
      log.error("process:: Unable to save kafka event {} into outbox table ", events, ex);
    }
  }
}
