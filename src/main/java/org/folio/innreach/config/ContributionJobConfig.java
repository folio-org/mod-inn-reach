package org.folio.innreach.config;

import lombok.extern.log4j.Log4j2;
import org.folio.innreach.config.props.InnReachRetryPolicy;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.support.RetryTemplate;

import org.folio.innreach.batch.contribution.listener.ContributionExceptionListener;
import org.folio.innreach.config.props.ContributionJobProperties;
import org.folio.innreach.domain.service.ContributionService;

@Configuration
@Log4j2
@EnableConfigurationProperties(ContributionJobProperties.class)
public class ContributionJobConfig {

  private static final int BACKOFF_MAX_INTERVAL = 10000;
  public static final int MAX_ATTEMPTS = 4;

  @Bean("contributionRetryTemplate")
  public RetryTemplate contributionRetryTemplate() {
    return RetryTemplate.builder()
      .customPolicy(new InnReachRetryPolicy(MAX_ATTEMPTS))
      .fixedBackoff(BACKOFF_MAX_INTERVAL)
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
