package org.folio.innreach;

import org.folio.spring.FolioExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableFeignClients
@EnableAsync
@EnableRetry
@EnableCaching
public class ModInnReachApplication {

  @Autowired(required = false)
  private static FolioExecutionContext folioExecutionContext;

  public static void main(String[] args) {
    System.out.println("Inside main with folioExecutionContext value" + folioExecutionContext);
    if(folioExecutionContext != null)
      System.out.println("folioContext header value "+folioExecutionContext.getAllHeaders() + " userId " +folioExecutionContext.getUserId());
    SpringApplication.run(ModInnReachApplication.class, args);

  }
}
