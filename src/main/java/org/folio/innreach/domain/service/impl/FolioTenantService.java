package org.folio.innreach.domain.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import org.folio.innreach.batch.contribution.service.ContributionJobRunner;

@Log4j2
@Service
@RequiredArgsConstructor
public class FolioTenantService {

  private final SystemUserService systemUserService;
  private final ContributionJobRunner contributionJobRunner;

  public void initializeTenant() {
    systemUserService.prepareSystemUser();
    contributionJobRunner.cancelJobs();
  }

}
