package org.folio.innreach.domain.service;

import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;

import org.folio.innreach.domain.exception.ResourceVersionConflictException;

public interface RetryableUpdateTemplate<Key, Rec> extends UpdateTemplate<Key, Rec> {

  @Override
  @Retryable(value = ResourceVersionConflictException.class,
      maxAttemptsExpression = "#{${retryable-update.retry-attempts}}",
      backoff = @Backoff(delayExpression = "#{${retryable-update.retry-interval-ms}}"))
  default Rec update(Key k, Finder<Key, Rec> finder, UpdateOperation<Rec> updater) {
    return UpdateTemplate.super.update(k, finder, updater);
  }

}
