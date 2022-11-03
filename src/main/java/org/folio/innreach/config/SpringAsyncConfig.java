package org.folio.innreach.config;

import org.folio.innreach.domain.exception.async.SpringAsyncExceptionHandler;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import java.util.concurrent.Executor;

@Configuration
public class SpringAsyncConfig implements AsyncConfigurer {

  /*
   * We need a SimpleAsyncTaskExecutor here because it will be used to run all @Async methods.
   * Since it is a SimpleAsyncTaskExecutor, a new thread will be created for every task, and these threads will not be reused.
   * So this is the main trick. Once created, a new thread will inherit all inheritable thread-local variables,
   * and FolioExecutionContext is one of them. Because a new thread will be created for every asynchronous task,
   * it will use the correct FolioExecutionContext received from the main thread used to spin up this one.
   */
  @Override
  public Executor getAsyncExecutor() {
    return new SimpleAsyncTaskExecutor("SimpleAsync-");
  }

  @Override
  public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
    return new SpringAsyncExceptionHandler();
  }

  @Bean("modAsyncExecutor")
  public ThreadPoolTaskScheduler prepareScheduler() {
    ThreadPoolTaskScheduler executor = new ThreadPoolTaskScheduler();
    //TODO: the pool size should be configurable through properties/env variables
    executor.setPoolSize(100);
    executor.setWaitForTasksToCompleteOnShutdown(true);
    executor.setThreadNamePrefix("Async Executor -> ");
    return executor;
  }

}