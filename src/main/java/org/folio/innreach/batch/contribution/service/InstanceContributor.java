package org.folio.innreach.batch.contribution.service;

import static org.folio.innreach.batch.contribution.ContributionJobContextManager.getContributionJobContext;

import java.util.UUID;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import org.folio.innreach.domain.service.InstanceTransformationService;
import org.folio.innreach.dto.BibInfo;
import org.folio.innreach.dto.Instance;
import org.folio.innreach.external.service.InnReachContributionService;

@Log4j2
@Service
@RequiredArgsConstructor
public class InstanceContributor {

  private final InnReachContributionService contributionService;
  private final InstanceTransformationService instanceTransformationService;

  public void contributeInstance(Instance instance) {
    var bibId = instance.getHrid();

    log.info("Contributing bib {}", bibId);

    var centralServerId = getContributionJobContext().getCentralServerId();

    var bib = instanceTransformationService.getBibInfo(centralServerId, instance);

    contribute(centralServerId, bibId, bib);

    verifyContribution(centralServerId, bibId);

    log.info("Finished contribution of bib {}", bibId);
  }

  private void contribute(UUID centralServerId, String bibId, BibInfo bib) {
    var response = contributionService.contributeBib(centralServerId, bibId, bib);
    Assert.isTrue(response.isOk(), "Unexpected contribution response: " + response);
  }

  private void verifyContribution(UUID centralServerId, String bibId) {
    var response = contributionService.lookUpBib(centralServerId, bibId);
    Assert.isTrue(response.isOk(), "Unexpected verification response: " + response);
  }

}
