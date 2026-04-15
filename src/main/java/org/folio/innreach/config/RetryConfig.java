package org.folio.innreach.config;

import java.time.Duration;
import lombok.Getter;
import org.folio.innreach.config.props.InnReachRetryOperationListener;
import org.folio.innreach.domain.exception.ResourceVersionConflictException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.core.retry.RetryPolicy;
import org.springframework.core.retry.RetryTemplate;

@Configuration
@Getter
public class RetryConfig {
  @Value(value = "${kafka.backoff.interval}")
  private Long interval;

  @Value(value = "${kafka.backoff.max_failure}")
  private Long maxAttempts;

  @Value(value = "${retryable-update.on-conflict.retry-interval-ms}")
  private Long retryableUpdateInterval;

  @Value(value = "${retryable-update.on-conflict.retry-attempts}")
  private Long retryableUpdateMaxAttempts;

  @Bean("retryableUpdateTemplate")
  public RetryTemplate retryableUpdateTemplate() {
    var retryPolicy = RetryPolicy.builder()
      .maxRetries(getRetryableUpdateMaxAttempts())
      .delay(Duration.ofMillis(getRetryableUpdateInterval()))
      .includes(ResourceVersionConflictException.class)
      .build();

    var retryableUpdateTemplate = new RetryTemplate(retryPolicy);
    retryableUpdateTemplate.setRetryListener(new InnReachRetryOperationListener());

    return retryableUpdateTemplate;
  }
}
