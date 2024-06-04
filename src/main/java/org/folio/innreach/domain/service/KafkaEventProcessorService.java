package org.folio.innreach.domain.service;

import org.folio.innreach.domain.event.DomainEvent;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public interface KafkaEventProcessorService {
  <T> void process(T event, Consumer<T> eventProcessor, String tenant);
  <T> void process(List<DomainEvent<T>> event, BiConsumer<List<DomainEvent<T>>, String> eventProcessor);
}
