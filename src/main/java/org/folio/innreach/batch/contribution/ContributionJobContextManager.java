package org.folio.innreach.batch.contribution;

import lombok.experimental.UtilityClass;
import lombok.extern.log4j.Log4j2;
import org.springframework.core.NamedInheritableThreadLocal;

@UtilityClass
@Log4j2
public class ContributionJobContextManager {

  private static final InheritableThreadLocal<ContributionJobContext> contributionJobContextHolder =
    new NamedInheritableThreadLocal<>("ContributionJobContextHolder");

  public static void beginContributionJobContext(ContributionJobContext contributionJobContext) {
    log.info("Contribution job context manager: iterationJobId is: {}", contributionJobContext.getIterationJobId());
    contributionJobContextHolder.set(contributionJobContext);
  }

  public static void endContributionJobContext() {
    log.info("Contribution job context manager: endContributionJobContext called");
    contributionJobContextHolder.remove();
  }

  public static ContributionJobContext getContributionJobContext() {
    return contributionJobContextHolder.get();
  }

}
