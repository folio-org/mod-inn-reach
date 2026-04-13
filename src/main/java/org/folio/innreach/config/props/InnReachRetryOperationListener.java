package org.folio.innreach.config.props;

import lombok.extern.log4j.Log4j2;
import org.springframework.core.retry.RetryException;
import org.springframework.core.retry.RetryListener;
import org.springframework.core.retry.RetryPolicy;
import org.springframework.core.retry.RetryState;
import org.springframework.core.retry.Retryable;

@Log4j2
public class InnReachRetryOperationListener implements RetryListener {

  @Override
  public void onRetryableExecution(RetryPolicy retryPolicy, Retryable<?> retryable, RetryState retryState) {
    var executionName = retryable.getName();
    var retryCount = retryState.getRetryCount();
    var exception = retryState.getExceptions().isEmpty() ? null : retryState.getLastException();

    if (exception == null) {
      log.info("onRetryableExecution:: retry attempt {} for execution of {}",
        retryCount, executionName);
      return;
    }

    log.info("onRetryableExecution:: Error {}, retry attempt {} for execution of {}",
      exception.getClass().getSimpleName(), retryCount, executionName);
  }

  @Override
  public void onRetryPolicyExhaustion(RetryPolicy retryPolicy, Retryable<?> retryable, RetryException exception) {
    log.info("onRetryPolicyExhaustion: retry on method = {} exhausted after {} attempts: {}",
      retryable.getName(), exception.getRetryCount(), exception.getMessage());
  }
}
