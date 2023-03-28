package org.folio.innreach;

import org.folio.spring.FolioExecutionContext;
import org.springframework.beans.factory.BeanFactory;
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

  @Autowired
  private static BeanFactory beanFactory;

  public static void main(String[] args) {
    SpringApplication.run(ModInnReachApplication.class, args);
    var context = beanFactory.getBean(FolioExecutionContext.class);
    System.out.println("folioContextObject"+context.getAllHeaders() + context.getUserId());

  }
}
