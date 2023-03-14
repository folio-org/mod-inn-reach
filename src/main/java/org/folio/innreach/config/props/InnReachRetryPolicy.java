package org.folio.innreach.config.props;

import feign.FeignException;
import lombok.extern.log4j.Log4j2;
import org.folio.innreach.external.exception.InnReachConnectionException;
import org.folio.innreach.external.exception.ServiceSuspendedException;
import org.folio.innreach.external.exception.SocketTimeOutExceptionWrapper;
import org.springframework.classify.Classifier;
import org.springframework.retry.RetryPolicy;
import org.springframework.retry.policy.ExceptionClassifierRetryPolicy;
import org.springframework.retry.policy.NeverRetryPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;

import java.net.SocketTimeoutException;

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
        else if (classifiable instanceof FeignException) {
          log.info("classifiable is of FeignException");
          return new NeverRetryPolicy();
        }
        else if (classifiable instanceof SocketTimeoutException) {
          log.info("classifiable is of SocketTimeOutException");
          return new NeverRetryPolicy();
        }
        else if (classifiable instanceof InnReachConnectionException) {
          log.info("classifiable is of InnReachConnectionException");
          return new NeverRetryPolicy();
        }
        log.info("other exception---");
        return simpleRetryPolicy;
      });
    }
}
