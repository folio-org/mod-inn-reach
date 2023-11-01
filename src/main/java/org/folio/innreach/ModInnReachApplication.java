package org.folio.innreach;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableFeignClients
@EnableAsync
@EnableRetry
@EnableCaching
@EnableScheduling
public class ModInnReachApplication {

  public static void main(String[] args) {
    SpringApplication.run(ModInnReachApplication.class, args);
  }
}
