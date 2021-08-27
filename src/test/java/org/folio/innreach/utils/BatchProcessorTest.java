package org.folio.innreach.utils;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.spy;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.retry.support.RetryTemplate;

@ExtendWith(MockitoExtension.class)
class BatchProcessorTest {

  private static final int MAX_RETRIES = 3;

  private final RetryTemplate retryTemplate = spy(RetryTemplate.builder().maxAttempts(MAX_RETRIES).fixedBackoff(1).build());

  private final BatchProcessor batchProcessor = new BatchProcessor(retryTemplate);

  @Test
  void shouldProcessBatch() {
    var consumedMessages = new ArrayList<Integer>();
    var failedMessages = new ArrayList<Pair<Integer, Exception>>();

    batchProcessor.process(List.of(1, 2, 3),
      consumedMessages::addAll,
      (value, err) -> failedMessages.add(Pair.of(value, err)));

    assertThat(consumedMessages).containsExactly(1, 2, 3);
    assertThat(failedMessages).isEmpty();
  }

  @Test
  void shouldProcessBatchWithRetry() {
    var consumedMessages = new ArrayList<Integer>();
    var failedMessages = new ArrayList<Pair<Integer, Exception>>();
    var attemptsCounter = new AtomicInteger(1);

    batchProcessor.process(List.of(1, 2, 3),
      attemptThrowingConsumer(consumedMessages, attemptsCounter, MAX_RETRIES - 1),
      (value, err) -> failedMessages.add(Pair.of(value, err)));

    assertThat(consumedMessages).containsExactly(1, 2, 3);
    assertThat(failedMessages).isEmpty();
  }

  @Test
  void shouldProcessBatchOneByOne() {
    var consumedMessages = new ArrayList<Integer>();
    var failedMessages = new ArrayList<Pair<Integer, Exception>>();
    var attemptsCounter = new AtomicInteger(1);

    batchProcessor.process(List.of(1, 2, 3),
      attemptThrowingConsumer(consumedMessages, attemptsCounter, MAX_RETRIES),
      (value, err) -> failedMessages.add(Pair.of(value, err)));

    assertThat(consumedMessages).containsExactly(1, 2, 3);
    assertThat(failedMessages).isEmpty();
  }

  @Test
  void shouldProcessFailedBatchOfSingleValue() {
    var consumedMessages = new ArrayList<Integer>();
    var failedMessages = new ArrayList<Pair<Integer, Exception>>();
    var attemptsCounter = new AtomicInteger(1);

    batchProcessor.process(singletonList(1),
      attemptThrowingConsumer(consumedMessages, attemptsCounter, MAX_RETRIES),
      (value, err) -> failedMessages.add(Pair.of(value, err)));

    assertThat(consumedMessages).isEmpty();
    assertThat(failedMessages).hasSize(1).satisfies(list -> verifyFailedMessage(list.get(0), 1));
  }

  @Test
  void shouldProcessFailedBatchOneByOne() {
    var consumedMessages = new ArrayList<Integer>();
    var failedMessages = new ArrayList<Pair<Integer, Exception>>();
    var attemptsCounter = new AtomicInteger(1);

    batchProcessor.process(List.of(1, 2, 3),
      attemptThrowingConsumer(consumedMessages, attemptsCounter, 12),
      (value, err) -> failedMessages.add(Pair.of(value, err)));

    assertThat(failedMessages).hasSize(3).satisfies(list -> {
      verifyFailedMessage(list.get(0), 1);
      verifyFailedMessage(list.get(1), 2);
      verifyFailedMessage(list.get(2), 3);
    });
  }

  private void verifyFailedMessage(Pair<Integer, Exception> value, int expectedValue) {
    assertThat(value.getKey()).isEqualTo(expectedValue);
    assertThat(value.getValue()).isInstanceOf(RuntimeException.class);
  }

  private static Consumer<List<Integer>> attemptThrowingConsumer(List<Integer> list, AtomicInteger cnt, int max) {
    return values -> {
      var currentAttempt = cnt.getAndIncrement();
      if (currentAttempt <= max) {
        throw new RuntimeException("error");
      }
      list.addAll(values);
    };
  }
}
