package org.folio.innreach.batch.contribution.service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.support.AbstractItemStreamItemWriter;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import org.folio.innreach.batch.contribution.ContributionJobContext;
import org.folio.innreach.domain.service.InstanceTransformationService;
import org.folio.innreach.domain.service.impl.TenantScopedExecutionService;
import org.folio.innreach.dto.BibInfo;
import org.folio.innreach.dto.Instance;
import org.folio.innreach.external.service.InnReachContributionService;

@Log4j2
@StepScope
@Service
@RequiredArgsConstructor
public class InstanceContributor extends AbstractItemStreamItemWriter<Instance> {

  public static final String INSTANCE_CONTRIBUTED_ID_CONTEXT = "contribution.instance.contributed-id";

  private final TenantScopedExecutionService tenantScopedExecutionService;
  private final InnReachContributionService contributionService;
  private final InstanceTransformationService instanceTransformationService;

  private final ContributionJobContext jobContext;

  private List<String> contributedInstanceIds = new ArrayList<>();

  @Override
  public void write(List<? extends Instance> instances) {
    if (instances.isEmpty()) {
      return;
    }
    tenantScopedExecutionService.runTenantScoped(jobContext.getTenantId(), () ->
      instances.forEach(instance -> contributeInstance(jobContext.getCentralServerId(), instance))
    );
  }

  @Override
  public void open(ExecutionContext executionContext) {
    if (executionContext.containsKey(INSTANCE_CONTRIBUTED_ID_CONTEXT)) {
      contributedInstanceIds = new ArrayList<>((List<String>) executionContext.get(INSTANCE_CONTRIBUTED_ID_CONTEXT));
    }
  }

  @Override
  public void update(ExecutionContext executionContext) {
    executionContext.put(INSTANCE_CONTRIBUTED_ID_CONTEXT, new ArrayList<>(contributedInstanceIds));
  }

  private void contributeInstance(UUID centralServerId, Instance instance) {
    var bibId = instance.getHrid();

    log.info("Contributing bib {}", bibId);

    var bib = instanceTransformationService.getBibInfo(centralServerId, instance);

    contribute(centralServerId, bibId, bib);

    verifyContribution(centralServerId, bibId);

    contributedInstanceIds.add(instance.getId().toString());

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
