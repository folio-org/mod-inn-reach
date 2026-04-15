package org.folio.innreach;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.resilience.annotation.EnableResilientMethods;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableAsync
@EnableScheduling
@EnableResilientMethods
public class ModInnReachApplication {
  public static void main(String[] args) {
    SpringApplication.run(ModInnReachApplication.class, args);
  }
}
