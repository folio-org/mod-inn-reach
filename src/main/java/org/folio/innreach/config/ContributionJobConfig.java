package org.folio.innreach.config;

import org.folio.innreach.config.props.InnReachRetryPolicy;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.support.RetryTemplate;

import org.folio.innreach.batch.contribution.listener.ContributionExceptionListener;
import org.folio.innreach.config.props.ContributionJobProperties;
import org.folio.innreach.domain.service.ContributionService;

@Configuration
@EnableConfigurationProperties(ContributionJobProperties.class)
public class ContributionJobConfig {

  private static final int BACKOFF_MAX_INTERVAL = 60000;
  private static final int BACKOFF_MULTIPLIER = 2;

  @Bean("contributionRetryTemplate")
  public RetryTemplate contributionRetryTemplate(ContributionJobProperties jobProperties) {
    return RetryTemplate.builder()
      .customPolicy(new InnReachRetryPolicy(jobProperties.getRetryAttempts()))
      .exponentialBackoff(jobProperties.getRetryIntervalMs(), BACKOFF_MULTIPLIER, BACKOFF_MAX_INTERVAL)
      .build();
  }

  @Bean("instanceExceptionListener")
  public ContributionExceptionListener instanceListener(ContributionService contributionService) {
    return new ContributionExceptionListener(contributionService, "instanceContribution");
  }

  @Bean("itemExceptionListener")
  public ContributionExceptionListener itemListener(ContributionService contributionService) {
    return new ContributionExceptionListener(contributionService, "itemContribution");
  }

}
