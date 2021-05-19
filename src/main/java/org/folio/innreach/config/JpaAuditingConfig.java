package org.folio.innreach.config;

import java.time.OffsetDateTime;
import java.util.Optional;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.auditing.DateTimeProvider;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorAware", dateTimeProviderRef = "dateTimeProvider", modifyOnCreate = false)
public class JpaAuditingConfig {

  @Bean
  public AuditorAware<String> auditorAware() {
    return () -> Optional.of("admin");
  }

  @Bean
  public DateTimeProvider dateTimeProvider() {
      return () -> Optional.of(OffsetDateTime.now());
  }
}
