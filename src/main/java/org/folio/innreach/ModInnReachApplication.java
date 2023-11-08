package org.folio.innreach;

import org.apache.commons.lang3.StringUtils;
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

  public static final String SYSTEM_USER_PASSWORD = "SYSTEM_USER_PASSWORD";
  public static void main(String[] args) {
    if (StringUtils.isEmpty(System.getenv(SYSTEM_USER_PASSWORD))) {
      throw new IllegalArgumentException("Required environment variable is missing: " + SYSTEM_USER_PASSWORD);
    }
    SpringApplication.run(ModInnReachApplication.class, args);
  }
}
