package org.folio.innreach.domain.service.impl;

import java.util.Collection;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.type.TypeFactory;
import org.apache.kafka.common.header.Headers;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.kafka.support.serializer.JsonTypeResolver;
import org.springframework.stereotype.Service;

import org.folio.innreach.config.props.FolioKafkaProperties;
import org.folio.innreach.config.props.FolioKafkaProperties.KafkaListenerProperties;
import org.folio.innreach.domain.event.DomainEvent;

@Service
public class DomainEventTypeResolver implements JsonTypeResolver {

  private static final String DOMAIN_EVENT_DATA_TYPE_CACHE = "domain-event-data-type";

  private final Collection<KafkaListenerProperties> listeners;

  public DomainEventTypeResolver(FolioKafkaProperties kafkaProperties) {
    this.listeners = kafkaProperties.getListener().values();
  }

  @Cacheable(cacheNames = DOMAIN_EVENT_DATA_TYPE_CACHE, key = "#topic")
  @Override
  public JavaType resolveType(String topic, byte[] data, Headers headers) {
    return listeners.stream()
      .filter(listener -> topic.matches(listener.getTopicPattern()))
      .findFirst()
      .map(KafkaListenerProperties::getDataType)
      .map(this::toJavaType)
      .orElse(null);
  }

  private JavaType toJavaType(Class<?> listenerDataType) {
    return TypeFactory.defaultInstance().constructParametricType(DomainEvent.class, listenerDataType);
  }
}
