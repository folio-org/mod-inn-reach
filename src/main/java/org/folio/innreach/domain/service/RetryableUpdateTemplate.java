package org.folio.innreach.domain.service;

import java.util.Optional;

import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;

import org.folio.innreach.domain.exception.ResourceVersionConflictException;

public interface RetryableUpdateTemplate<K, R> extends UpdateTemplate<K,R> {

  @Override
  @Retryable(value = ResourceVersionConflictException.class,
      maxAttemptsExpression = "#{${retryable-update.on-conflict.retry-attempts}}",
      backoff = @Backoff(delayExpression = "#{${retryable-update.on-conflict.retry-interval-ms}}"))
  default Optional<R> update(K key, Finder<? super K, ? extends R> finder, UpdateOperation<R> updater) {
    return UpdateTemplate.super.update(key, finder, updater);
  }

}
