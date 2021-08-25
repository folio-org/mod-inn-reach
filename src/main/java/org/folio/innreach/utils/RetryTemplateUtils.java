package org.folio.innreach.utils;

import org.springframework.retry.RetryPolicy;
import org.springframework.retry.backoff.FixedBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;

public class RetryTemplateUtils {

  public static RetryTemplate createRetryTemplate(long retryIntervalMs, int maxAttempts) {
    FixedBackOffPolicy backOffPolicy = new FixedBackOffPolicy();
    backOffPolicy.setBackOffPeriod(retryIntervalMs);

    RetryPolicy retryPolicy = new SimpleRetryPolicy(maxAttempts);

    var retryTemplate = new RetryTemplate();
    retryTemplate.setBackOffPolicy(backOffPolicy);
    retryTemplate.setRetryPolicy(retryPolicy);
    return retryTemplate;
  }

}
