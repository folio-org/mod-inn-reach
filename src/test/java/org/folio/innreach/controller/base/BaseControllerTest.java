package org.folio.innreach.controller.base;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import org.folio.innreach.ModInnReachApplication;
import org.folio.spring.liquibase.FolioLiquibaseConfiguration;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = { ModInnReachApplication.class,
	BaseControllerTest.TestApplicationContext.class })
@ActiveProfiles({ "test", "testcontainers-pg" })
public class BaseControllerTest {

    @EnableAutoConfiguration(exclude = FolioLiquibaseConfiguration.class)
    static class TestApplicationContext {
    }
}
