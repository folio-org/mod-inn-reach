package org.folio.innreach.batch.contribution.service;

import static org.folio.innreach.batch.contribution.ContributionJobContextManager.getContributionJobContext;

import java.util.UUID;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.innreach.domain.service.ContributionValidationService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import org.folio.innreach.domain.service.InstanceTransformationService;
import org.folio.innreach.dto.BibInfo;
import org.folio.innreach.dto.Instance;
import org.folio.innreach.external.dto.InnReachResponse;
import org.folio.innreach.external.service.InnReachContributionService;

@Log4j2
@Service
@RequiredArgsConstructor
public class InstanceContributor {

  @Qualifier("contributionRetryTemplate")
  private final RetryTemplate retryTemplate;
  private final InnReachContributionService contributionService;
  private final InstanceTransformationService instanceTransformationService;
  private final ContributionValidationService validationService;

  public void contributeInstance(Instance instance) {
    var centralServerId = getContributionJobContext().getCentralServerId();
    var valid = validationService.isEligibleForContribution(centralServerId, instance);
    if (!valid) {
      return;
    }

    var bibId = instance.getHrid();

    log.info("Contributing bib {}", bibId);

    var bib = instanceTransformationService.getBibInfo(centralServerId, instance);

    retryTemplate.execute(r -> contribute(centralServerId, bibId, bib));

    retryTemplate.execute(r -> verifyContribution(centralServerId, bibId));

    log.info("Finished contribution of bib {}", bibId);
  }

  private InnReachResponse contribute(UUID centralServerId, String bibId, BibInfo bib) {
    var response = contributionService.contributeBib(centralServerId, bibId, bib);
    Assert.isTrue(response.isOk(), "Unexpected contribution response: " + response);
    return response;
  }

  private InnReachResponse verifyContribution(UUID centralServerId, String bibId) {
    var response = contributionService.lookUpBib(centralServerId, bibId);
    Assert.isTrue(response.isOk(), "Unexpected verification response: " + response);
    return response;
  }

}
