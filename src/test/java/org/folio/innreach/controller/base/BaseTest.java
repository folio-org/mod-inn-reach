package org.folio.innreach.controller.base;

import org.folio.innreach.ModInnReachApplication;
import org.folio.spring.liquibase.FolioLiquibaseConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(
  webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
  classes = {
    ModInnReachApplication.class,
    BaseTest.TestApplicationContext.class,
    DataSourceAutoConfiguration.class,
    HibernateJpaAutoConfiguration.class,
    DataSourceTransactionManagerAutoConfiguration.class
  }
)
@ActiveProfiles({"test", "testcontainers-pg"})
public class BaseTest {

  @EnableAutoConfiguration(exclude = FolioLiquibaseConfiguration.class)
  static class TestApplicationContext {
  }
}
