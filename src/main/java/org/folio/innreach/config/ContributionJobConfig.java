package org.folio.innreach.config;

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

  @Bean
  public RetryTemplate batchRetryTemplate(ContributionJobProperties jobProperties) {
    return RetryTemplate.builder()
      .maxAttempts(jobProperties.getRetryAttempts())
      .fixedBackoff(jobProperties.getRetryIntervalMs())
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
