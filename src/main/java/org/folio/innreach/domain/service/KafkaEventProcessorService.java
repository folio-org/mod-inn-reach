package org.folio.innreach.domain.service;

import java.util.function.Consumer;

public interface KafkaEventProcessorService {
  <T> void process(T event, Consumer<T> eventProcessor, String tenant);
}
