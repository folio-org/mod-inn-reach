package org.folio.innreach.domain.service.impl;

import static org.apache.commons.collections4.ListUtils.emptyIfNull;

import java.util.List;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.ListUtils;
import org.springframework.stereotype.Service;

import org.folio.innreach.batch.contribution.service.ContributionJobRunner;
import org.folio.tenant.domain.dto.Parameter;
import org.folio.tenant.domain.dto.TenantAttributes;

@Log4j2
@Service
@RequiredArgsConstructor
public class FolioTenantService {

  public static final String LOAD_REF_DATA_PARAMETER = "loadReference";

  private final SystemUserService systemUserService;
  private final ContributionJobRunner contributionJobRunner;
  private final ReferenceDataLoader referenceDataLoader;

  public void initializeTenant(TenantAttributes tenantAttributes) {
    systemUserService.prepareSystemUser();
    contributionJobRunner.cancelJobs();
    if (shouldLoadRefData(emptyIfNull(tenantAttributes.getParameters()))) {
      referenceDataLoader.loadRefData();
    }
  }

  private boolean shouldLoadRefData(List<Parameter> parameters) {
    for (Parameter parameter : parameters) {
      if (LOAD_REF_DATA_PARAMETER.equals(parameter.getKey())) {
        return Boolean.parseBoolean(parameter.getValue());
      }
    }
    return false;
  }

}
