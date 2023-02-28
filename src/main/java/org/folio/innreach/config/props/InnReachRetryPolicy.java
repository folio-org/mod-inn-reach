package org.folio.innreach.config.props;

import lombok.extern.log4j.Log4j2;
import org.folio.innreach.external.exception.ServiceSuspendedException;
import org.springframework.classify.Classifier;
import org.springframework.retry.RetryPolicy;
import org.springframework.retry.policy.ExceptionClassifierRetryPolicy;
import org.springframework.retry.policy.NeverRetryPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;

@Log4j2
public class InnReachRetryPolicy extends ExceptionClassifierRetryPolicy {

    public InnReachRetryPolicy(int maxAttempts) {
      final SimpleRetryPolicy simpleRetryPolicy = new SimpleRetryPolicy();
      simpleRetryPolicy.setMaxAttempts(maxAttempts);

      this.setExceptionClassifier((Classifier<Throwable, RetryPolicy>) classifiable -> {

        if (classifiable instanceof ServiceSuspendedException) {
            log.info("classifiable is of ServiceSuspendedException");
          return new NeverRetryPolicy();
        }
        log.info("other exception---");
        return simpleRetryPolicy;
      });
    }
}
