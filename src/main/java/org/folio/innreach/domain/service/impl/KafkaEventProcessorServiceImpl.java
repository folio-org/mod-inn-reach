package org.folio.innreach.domain.service.impl;

import lombok.extern.log4j.Log4j2;
import org.folio.innreach.domain.service.KafkaEventProcessorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.function.Consumer;

@Service
@Log4j2
public class KafkaEventProcessorServiceImpl implements KafkaEventProcessorService {

  @Autowired
  private TenantScopedExecutionService executionService;


  @Override
  public <T> void process(T event, Consumer<T> eventProcessor, String tenant) {
    try {
      log.info("process: event {} , eventProcessor {} , tenant {} ", event, eventProcessor, tenant);
      executionService.runTenantScoped(tenant,
        () -> eventProcessor.accept(event));
    } catch (Exception ex) {
      log.warn("Unable to save kafka event into outbox table ", ex);
    }
  }
}
