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
import org.folio.innreach.domain.service.ContributionValidationService;
import org.folio.innreach.domain.service.MARCRecordTransformationService;
import org.folio.innreach.domain.service.impl.TenantScopedExecutionService;
import org.folio.innreach.dto.Instance;
import org.folio.innreach.external.dto.Bib;
import org.folio.innreach.external.service.InnReachContributionService;

@Log4j2
@StepScope
@Service
@RequiredArgsConstructor
public class InstanceContributor extends AbstractItemStreamItemWriter<Instance> {

  public static final String INSTANCE_CONTRIBUTED_ID_CONTEXT = "contribution.instance.contributed-id";
  private static final String MARC_BIB_FORMAT = "ISO2709";

  private final TenantScopedExecutionService tenantScopedExecutionService;
  private final MARCRecordTransformationService marcRecordTransformationService;
  private final InnReachContributionService contributionService;
  private final ContributionValidationService validationService;

  private final ContributionJobContext jobContext;

  private List<UUID> contributedInstanceIds = new ArrayList<>();

  @Override
  public void write(List<? extends Instance> instances) {
    if (instances.isEmpty()) {
      return;
    }
    tenantScopedExecutionService.executeTenantScoped(
      jobContext.getTenantId(),
      () -> {
        instances.forEach(item -> contributeInstance(jobContext.getCentralServerId(), item));
        return null;
      }
    );
  }

  @Override
  public void update(ExecutionContext executionContext) {
    executionContext.put(INSTANCE_CONTRIBUTED_ID_CONTEXT, new ArrayList<>(contributedInstanceIds));
  }


  private void contributeInstance(UUID centralServerId, Instance instance) {
    var bibId = instance.getHrid();
    var suppressionStatus = validationService.getSuppressionStatus(centralServerId, instance.getStatisticalCodeIds());
    var marc = marcRecordTransformationService.transformRecord(centralServerId, instance.getId());

    var bib = Bib.builder()
      .bibId(bibId)
      .suppress(suppressionStatus)
      .marc21BibFormat(MARC_BIB_FORMAT)
      .marc21BibData(marc.getBase64rawContent())
      .titleHoldCount(0)
      .itemCount(0)
      .build();

    contribute(centralServerId, bibId, bib);

    verifyContribution(centralServerId, bibId);

    contributedInstanceIds.add(instance.getId());
  }

  private void contribute(UUID centralServerId, String bibId, Bib bib) {
    var response = contributionService.contributeBib(centralServerId, bibId, bib);
    Assert.isTrue(response.isOk(), "Unexpected contribution response: " + response);
  }

  private void verifyContribution(UUID centralServerId, String bibId) {
    var response = contributionService.lookUpBib(centralServerId, bibId);
    Assert.isTrue(response.isOk(), "Unexpected verification response: " + response);
  }

}
