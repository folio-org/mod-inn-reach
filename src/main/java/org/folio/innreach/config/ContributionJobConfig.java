package org.folio.innreach.config;

import java.net.SocketTimeoutException;
import java.time.Duration;
import lombok.extern.log4j.Log4j2;
import org.folio.innreach.config.props.InnReachRetryOperationListener;
import org.folio.innreach.external.exception.InnReachConnectionException;
import org.folio.innreach.external.exception.ServiceSuspendedException;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.folio.innreach.batch.contribution.listener.ContributionExceptionListener;
import org.folio.innreach.config.props.ContributionJobProperties;
import org.folio.innreach.domain.service.ContributionService;
import org.springframework.core.retry.RetryPolicy;
import org.springframework.core.retry.RetryTemplate;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

@Configuration
@Log4j2
@EnableConfigurationProperties(ContributionJobProperties.class)
public class ContributionJobConfig {

  private static final int BACKOFF_MAX_INTERVAL = 10000;
  public static final int MAX_ATTEMPTS = 4;

  @Bean("contributionRetryTemplate")
  public RetryTemplate contributionRetryTemplate() {
    var retryPolicy = RetryPolicy.builder()
      .maxRetries(MAX_ATTEMPTS)
      .delay(Duration.ofMillis(BACKOFF_MAX_INTERVAL))
      .excludes(ServiceSuspendedException.class, HttpClientErrorException.class, HttpServerErrorException.class,
        SocketTimeoutException.class, InnReachConnectionException.class)
      .build();

    var contributionRetryTemplate = new RetryTemplate(retryPolicy);
    contributionRetryTemplate.setRetryListener(new InnReachRetryOperationListener());

    return contributionRetryTemplate;
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
