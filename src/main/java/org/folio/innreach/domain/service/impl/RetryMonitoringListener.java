package org.folio.innreach.domain.service.impl;

import lombok.extern.log4j.Log4j2;
import org.springframework.retry.RetryContext;
import org.springframework.retry.interceptor.MethodInvocationRetryCallback;
import org.springframework.retry.listener.MethodInvocationRetryListenerSupport;

@Log4j2
public class RetryMonitoringListener extends MethodInvocationRetryListenerSupport {

  @Override
  protected <T, E extends Throwable> void doOnError(RetryContext context,
      MethodInvocationRetryCallback<T, E> callback, Throwable throwable) {
    log.info("Retryable exception happened: exception = [{}], method = [{}], retryCount = [{}]",
        throwable.toString(), callback.getLabel(), context.getRetryCount());
  }

  @Override
  protected <T, E extends Throwable> void doClose(RetryContext context,
      MethodInvocationRetryCallback<T, E> callback, Throwable throwable) {
    log.info("Retryable method invocation finished: method = [{}], retryCount = [{}], exhausted = [{}]",
        callback.getLabel(), context.getRetryCount(), context.isExhaustedOnly());
  }

}