package org.folio.innreach.repository;

import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
abstract class BaseRepositoryTest {

  @Container
  static final PostgreSQLContainer postgresqlContainer = new PostgreSQLContainer("postgres:11-alpine")
      .withDatabaseName("db")
      .withUsername("postgres")
      .withPassword("postgres");

  @DynamicPropertySource
  static void setDatasourceProperties(DynamicPropertyRegistry propertyRegistry) {
    propertyRegistry.add("spring.datasource.url", postgresqlContainer::getJdbcUrl);
    propertyRegistry.add("spring.datasource.password", postgresqlContainer::getPassword);
    propertyRegistry.add("spring.datasource.username", postgresqlContainer::getUsername);
  }

}
