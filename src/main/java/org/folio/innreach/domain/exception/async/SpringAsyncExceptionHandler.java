package org.folio.innreach.domain.exception.async;

import java.lang.reflect.Method;

import lombok.extern.log4j.Log4j2;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;

@Log4j2
public class SpringAsyncExceptionHandler implements AsyncUncaughtExceptionHandler {

  @Override
  public void handleUncaughtException(Throwable throwable, Method method, Object... objects) {
    log.error("Async method [{}] throw exception", method, throwable);
  }
}
