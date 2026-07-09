package org.folio.innreach.repository;

import org.folio.innreach.client.UsersClient;
import org.folio.innreach.domain.service.UserService;
import org.folio.innreach.domain.service.impl.UserServiceImpl;
import org.folio.innreach.support.postgres.WithPostgresContainer;
import org.folio.spring.FolioExecutionContext;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@DataJpaTest
@WithPostgresContainer
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DirtiesContext
@ActiveProfiles("test")
abstract class BaseRepositoryIT {

  @MockitoBean
  private FolioExecutionContext folioExecutionContext;
  @MockitoBean
  private UsersClient usersClient;

  @TestConfiguration
  static class BaseRepositoryConfiguration {

    @Bean
    public UserService userService(UsersClient userClient) {
      return new UserServiceImpl(userClient);
    }

  }

}
