package org.folio.innreach.config;

import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;

import org.folio.innreach.domain.exception.async.SpringAsyncExceptionHandler;

@Configuration
public class SpringAsyncConfig implements AsyncConfigurer {

  @Override
  public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
    return new SpringAsyncExceptionHandler();
  }
}
