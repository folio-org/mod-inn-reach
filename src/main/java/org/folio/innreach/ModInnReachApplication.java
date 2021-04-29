package org.folio.innreach;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

@SpringBootApplication(exclude = {
//  DataSourceAutoConfiguration.class,
//  HibernateJpaAutoConfiguration.class,
//  DataSourceTransactionManagerAutoConfiguration.class
})
@EnableSwagger2
@EnableFeignClients
public class ModInnReachApplication {

  public static void main(String[] args) {
    SpringApplication.run(ModInnReachApplication.class, args);
  }

}
