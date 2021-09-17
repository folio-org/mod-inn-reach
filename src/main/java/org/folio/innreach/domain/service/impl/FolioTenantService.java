package org.folio.innreach.domain.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import org.folio.innreach.batch.contribution.service.ContributionJobRunner;
import org.folio.spring.FolioExecutionContext;

@Log4j2
@Service
@RequiredArgsConstructor
public class FolioTenantService {

  private final SystemUserService systemUserService;
  private final ContributionJobRunner contributionJobRunner;
  private final FolioExecutionContext context;

  public void initializeTenant() {
    systemUserService.prepareSystemUser();
    contributionJobRunner.restart(context.getTenantId());
  }

}
