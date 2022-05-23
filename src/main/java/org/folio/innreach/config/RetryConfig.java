package org.folio.innreach.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.RetryListener;

import org.folio.innreach.domain.service.impl.RetryMonitoringListener;

@Configuration
public class RetryConfig {

  @Bean
  public RetryListener retryMonitoringListener() {
    return new RetryMonitoringListener();
  }

}