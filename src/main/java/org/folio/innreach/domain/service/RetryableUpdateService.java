package org.folio.innreach.domain.service;

import java.util.Optional;
import java.util.function.Supplier;

import lombok.RequiredArgsConstructor;
import org.folio.innreach.domain.exception.ResourceVersionConflictException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.retry.RetryException;
import org.springframework.core.retry.RetryTemplate;
import org.springframework.stereotype.Service;

/**
 * Service that provides retryable update operations for BasicService implementations.
 * Uses the retryableUpdateTemplate to handle ResourceVersionConflictException.
 */
@Service
@RequiredArgsConstructor
public class RetryableUpdateService {

  @Qualifier("retryableUpdateTemplate")
  private final RetryTemplate retryTemplate;

  /**
   * Executes changeAndUpdate with retry logic.
   *
   * @param service the BasicService implementation
   * @param key the key to find the record
   * @param change the update operation to apply
   * @param <K> the key type
   * @param <R> the record type
   * @return Optional containing the updated record if found
   */
  public <K, R> Optional<R> changeAndUpdateWithRetry(BasicService<K, R> service, K key,
      UpdateTemplate.UpdateOperation<R> change) {
    try {
      return retryTemplate.execute(() -> service.changeAndUpdate(key, change));
    } catch (RetryException ex) {
      if (ex.getLastException() instanceof ResourceVersionConflictException rvex) {
        throw rvex;
      }
      throw new ResourceVersionConflictException(ex.getMessage());
    }
  }

  /**
   * Executes changeAndUpdate with retry logic and throws exception if not found.
   *
   * @param service the BasicService implementation
   * @param key the key to find the record
   * @param notFoundExceptionSupplier supplier for exception when record not found
   * @param change the update operation to apply
   * @param <K> the key type
   * @param <R> the record type
   * @return Optional containing the updated record
   */
  public <K, R> Optional<R> changeAndUpdateWithRetry(BasicService<K, R> service, K key,
      Supplier<? extends RuntimeException> notFoundExceptionSupplier,
      UpdateTemplate.UpdateOperation<R> change) {
    try {
      return retryTemplate.execute(() -> service.changeAndUpdate(key, notFoundExceptionSupplier, change));
    } catch (RetryException ex) {
      if (ex.getLastException() instanceof ResourceVersionConflictException rvex) {
        throw rvex;
      }
      throw new ResourceVersionConflictException(ex.getMessage());
    }
  }

}

