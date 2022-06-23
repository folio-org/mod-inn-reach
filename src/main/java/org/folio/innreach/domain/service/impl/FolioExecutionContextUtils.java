package org.folio.innreach.domain.service.impl;

import static org.folio.spring.scope.FolioExecutionScopeExecutionContextManager.beginFolioExecutionContext;
import static org.folio.spring.scope.FolioExecutionScopeExecutionContextManager.endFolioExecutionContext;

import java.util.concurrent.Callable;

import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;

import org.folio.spring.FolioExecutionContext;

@UtilityClass
public class FolioExecutionContextUtils {

  @SneakyThrows
  public static <T> T executeWithinContext(FolioExecutionContext context, Callable<T> job) {
    try {
      beginFolioExecutionContext(context);
      return job.call();
    } finally {
      endFolioExecutionContext();
    }
  }

}
