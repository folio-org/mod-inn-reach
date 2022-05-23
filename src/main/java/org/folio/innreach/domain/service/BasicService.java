package org.folio.innreach.domain.service;

import java.util.Optional;
import java.util.function.Supplier;

import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;

import org.folio.innreach.domain.exception.ResourceVersionConflictException;

public interface BasicService<K, R> extends UpdateTemplateWithFinder<K, R> {

  R create(R aRecord);

  R update(R aRecord);

  Optional<R> find(K key);

  @Retryable(value = ResourceVersionConflictException.class,
      maxAttemptsExpression = "#{${retryable-update.on-conflict.retry-attempts}}",
      backoff = @Backoff(delayExpression = "#{${retryable-update.on-conflict.retry-interval-ms}}"),
      listeners = {"retryMonitoringListener"})
  default Optional<R> changeAndUpdate(K key, UpdateOperation<R> change) {
    return update(key, change.andThen(this::update));
  }

  @Retryable(value = ResourceVersionConflictException.class,
      maxAttemptsExpression = "#{${retryable-update.on-conflict.retry-attempts}}",
      backoff = @Backoff(delayExpression = "#{${retryable-update.on-conflict.retry-interval-ms}}"),
      listeners = {"retryMonitoringListener"})
  default Optional<R> changeAndUpdate(K key, Supplier<? extends RuntimeException> notFoundExceptionSupplier,
      UpdateOperation<R> change) {
    return update(key, finder().toRequired(notFoundExceptionSupplier), change.andThen(this::update));
  }

  @Override
  default Finder<K, R> finder() {
    return this::find;
  }

}
