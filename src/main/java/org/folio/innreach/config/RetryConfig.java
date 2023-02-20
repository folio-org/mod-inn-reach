package org.folio.innreach.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.RetryListener;

import org.folio.innreach.domain.service.impl.RetryMonitoringListener;

@Configuration
@Getter
public class RetryConfig {
  @Value(value = "${kafka.backoff.interval}")
  private Long interval;

  @Value(value = "${kafka.backoff.max_failure}")
  private Long maxAttempts;

  @Bean
  public RetryListener retryMonitoringListener() {
    return new RetryMonitoringListener();
  }
}
