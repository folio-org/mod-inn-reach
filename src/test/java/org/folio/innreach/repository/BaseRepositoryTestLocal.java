package org.folio.innreach.repository;

import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Testcontainers;

@DataJpaTest(includeFilters = @ComponentScan.Filter(classes = EnableJpaAuditing.class))
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DirtiesContext
@ActiveProfiles("local")
abstract class BaseRepositoryTestLocal {

//  @Container
//  static final PostgreSQLContainer postgresqlContainer = new PostgreSQLContainer("postgres:11-alpine")
//      .withDatabaseName("db")
//      .withUsername("postgres")
//      .withPassword("postgres");
//
//  @DynamicPropertySource
//  static void setDatasourceProperties(DynamicPropertyRegistry propertyRegistry) {
//    propertyRegistry.add("spring.datasource.url", postgresqlContainer::getJdbcUrl);
//    propertyRegistry.add("spring.datasource.password", postgresqlContainer::getPassword);
//    propertyRegistry.add("spring.datasource.username", postgresqlContainer::getUsername);
//  }

}
