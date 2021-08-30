package org.folio.innreach.utils;

import static java.util.Collections.singletonList;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.retry.support.RetryTemplate;

@Log4j2
@RequiredArgsConstructor
public class BatchProcessor {

  private final RetryTemplate retryTemplate;

  /**
   * Consumes batch of values as list and tries to process them using the strategy with retry.
   *
   * <p> At first, a batch will be retried by the specified retry policy, then, if it's failing, it would be processed
   * by single value at one time, if the value would be failed to process - failedValueConsumer will be executed.
   * </p>
   *
   * @param batch               list of values as {@link List} object
   * @param batchConsumer       batch consumer as {@link Consumer} lambda function
   * @param failedValueConsumer bi value consumer, where first - is the failed value, second is the related error
   * @param <T>                 generic type for batch value
   */
  public <T> void process(List<T> batch, Consumer<List<T>> batchConsumer,
                          BiConsumer<T, Exception> failedValueConsumer) {
    try {
      execute(batch, batchConsumer);
    } catch (Exception e) {
      if (batch.size() == 1) {
        failedValueConsumer.accept(batch.iterator().next(), e);
      } else {
        log.warn("Failed to process batch of values: {}", e.getMessage());
        processMessagesOneByOne(batch, batchConsumer, failedValueConsumer);
      }
    }
  }

  private <T> void processMessagesOneByOne(List<T> batch, Consumer<List<T>> batchConsumer,
                                           BiConsumer<T, Exception> failedValueConsumer) {
    log.info("attempting to process batch messages one by one");
    for (T batchValue : batch) {
      try {
        execute(singletonList(batchValue), batchConsumer);
      } catch (Exception e) {
        failedValueConsumer.accept(batchValue, e);
      }
    }
  }

  private <T> void execute(List<T> batch, Consumer<List<T>> consumer) {
    retryTemplate.execute(ctx -> {
      consumer.accept(batch);
      return null;
    });
  }

}
