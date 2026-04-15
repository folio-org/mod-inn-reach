package org.folio.innreach.domain.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import lombok.SneakyThrows;
import org.folio.innreach.domain.exception.ResourceVersionConflictException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.core.retry.RetryException;
import org.springframework.core.retry.RetryTemplate;

class RetryableUpdateServiceTest {

  @Mock
  private RetryTemplate retryTemplate;

  @Mock
  private BasicService<UUID, String> basicService;

  @InjectMocks
  private RetryableUpdateService retryableUpdateService;

  @BeforeEach
  void setupBeforeEach() {
    MockitoAnnotations.openMocks(this);
  }

  @Test
  @SneakyThrows
  void returnUpdatedRecord_when_changeAndUpdateSucceeds() {
    var key = UUID.randomUUID();
    var expected = Optional.of("updated");
    UpdateTemplate.UpdateOperation<String> change = r -> r;

    when(retryTemplate.execute(any())).thenReturn(expected);

    var result = retryableUpdateService.changeAndUpdateWithRetry(basicService, key, change);

    assertSame(expected, result);
  }

  @Test
  @SneakyThrows
  void returnEmptyOptional_when_recordNotFound() {
    var key = UUID.randomUUID();
    Optional<String> expected = Optional.empty();
    UpdateTemplate.UpdateOperation<String> change = r -> r;

    when(retryTemplate.execute(any())).thenReturn(expected);

    var result = retryableUpdateService.changeAndUpdateWithRetry(basicService, key, change);

    assertTrue(result.isEmpty());
  }

  @Test
  @SneakyThrows
  void throwResourceVersionConflictException_when_retryExhaustedWithConflict() {
    var key = UUID.randomUUID();
    UpdateTemplate.UpdateOperation<String> change = r -> r;
    var conflictException = new ResourceVersionConflictException("optimistic locking conflict");
    var retryException = new RetryException("Retries exhausted", conflictException);

    when(retryTemplate.execute(any())).thenThrow(retryException);

    var thrown = assertThrows(ResourceVersionConflictException.class,
        () -> retryableUpdateService.changeAndUpdateWithRetry(basicService, key, change));

    assertSame(conflictException, thrown);
  }

  @Test
  @SneakyThrows
  void throwResourceVersionConflictException_when_retryExhaustedWithOtherException() {
    var key = UUID.randomUUID();
    UpdateTemplate.UpdateOperation<String> change = r -> r;
    var retryException = new RetryException("Retries exhausted", new RuntimeException("some error"));

    when(retryTemplate.execute(any())).thenThrow(retryException);

    var thrown = assertThrows(ResourceVersionConflictException.class,
        () -> retryableUpdateService.changeAndUpdateWithRetry(basicService, key, change));

    assertEquals("Retries exhausted", thrown.getMessage());
  }

  @Test
  @SneakyThrows
  void returnUpdatedRecord_when_changeAndUpdateWithNotFoundSupplierSucceeds() {
    var key = UUID.randomUUID();
    var expected = Optional.of("updated");
    UpdateTemplate.UpdateOperation<String> change = r -> r;

    when(retryTemplate.execute(any())).thenReturn(expected);

    var result = retryableUpdateService.changeAndUpdateWithRetry(
        basicService, key, () -> new RuntimeException("not found"), change);

    assertSame(expected, result);
  }

  @Test
  @SneakyThrows
  void throwNotFoundExceptionFromSupplier_when_recordNotFoundWithSupplier() {
    var key = UUID.randomUUID();
    UpdateTemplate.UpdateOperation<String> change = r -> r;
    var notFoundEx = new IllegalStateException("Record not found");

    when(retryTemplate.execute(any())).thenThrow(notFoundEx);

    assertThrows(IllegalStateException.class,
        () -> retryableUpdateService.changeAndUpdateWithRetry(
            basicService, key, () -> new RuntimeException("not found"), change));
  }

  @Test
  @SneakyThrows
  void throwOriginalConflictException_when_retryExhaustedWithConflictAndNotFoundSupplier() {
    var key = UUID.randomUUID();
    UpdateTemplate.UpdateOperation<String> change = r -> r;
    var conflictException = new ResourceVersionConflictException("version mismatch");
    var retryException = new RetryException("Retries exhausted", conflictException);

    when(retryTemplate.execute(any())).thenThrow(retryException);

    var thrown = assertThrows(ResourceVersionConflictException.class,
        () -> retryableUpdateService.changeAndUpdateWithRetry(
            basicService, key, () -> new RuntimeException("not found"), change));

    assertSame(conflictException, thrown);
  }

  @Test
  @SneakyThrows
  void wrapNonConflictRetryException_when_retryExhaustedWithOtherExceptionAndNotFoundSupplier() {
    var key = UUID.randomUUID();
    UpdateTemplate.UpdateOperation<String> change = r -> r;
    var retryException = new RetryException("Retries exhausted", new RuntimeException("timeout"));

    when(retryTemplate.execute(any())).thenThrow(retryException);

    var thrown = assertThrows(ResourceVersionConflictException.class,
        () -> retryableUpdateService.changeAndUpdateWithRetry(
            basicService, key, () -> new RuntimeException("not found"), change));

    assertEquals("Retries exhausted", thrown.getMessage());
  }
}









