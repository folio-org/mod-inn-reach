package org.folio.innreach.config;

import java.time.OffsetDateTime;
import java.util.Optional;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.auditing.DateTimeProvider;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import org.folio.innreach.domain.entity.base.AuditableUser;
import org.folio.innreach.domain.entity.base.AuditorAwareImpl;
import org.folio.innreach.domain.service.UserService;
import org.folio.spring.FolioExecutionContext;

@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorAware", dateTimeProviderRef = "dateTimeProvider", modifyOnCreate = false)
public class JpaAuditingConfig {

  @Bean
  public AuditorAware<AuditableUser> auditorAware(FolioExecutionContext context, UserService userService) {
    return new AuditorAwareImpl(context, userService);
  }

  @Bean
  public DateTimeProvider dateTimeProvider() {
      return () -> Optional.of(OffsetDateTime.now());
  }
}
