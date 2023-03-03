package org.folio.innreach.config;

import lombok.extern.log4j.Log4j2;
import org.folio.innreach.config.props.InnReachRetryPolicy;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.kafka.event.ListenerContainerIdleEvent;
import org.springframework.retry.support.RetryTemplate;

import org.folio.innreach.batch.contribution.listener.ContributionExceptionListener;
import org.folio.innreach.config.props.ContributionJobProperties;
import org.folio.innreach.domain.service.ContributionService;

@Configuration
@Log4j2
@EnableConfigurationProperties(ContributionJobProperties.class)
public class ContributionJobConfig {

  private static final int BACKOFF_MAX_INTERVAL = 15000;
  private static final int BACKOFF_MULTIPLIER = 2;

  @Bean("contributionRetryTemplate")
  public RetryTemplate contributionRetryTemplate(ContributionJobProperties jobProperties) {
    return RetryTemplate.builder()
      .customPolicy(new InnReachRetryPolicy(3))
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


  //GK
  @EventListener
  public void eventHandler(ListenerContainerIdleEvent event) {
    log.info("No messages received for " + event.getIdleTime() + " milliseconds");

  }
}
