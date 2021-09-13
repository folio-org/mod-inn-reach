package org.folio.innreach.batch.contribution.service;

import static org.apache.commons.collections4.ListUtils.emptyIfNull;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.Base64Utils;

import org.folio.innreach.batch.contribution.ContributionJobContext;
import org.folio.innreach.domain.service.ContributionCriteriaConfigurationService;
import org.folio.innreach.domain.service.MARCRecordTransformationService;
import org.folio.innreach.domain.service.impl.TenantScopedExecutionService;
import org.folio.innreach.dto.ContributionCriteriaDTO;
import org.folio.innreach.dto.Instance;
import org.folio.innreach.dto.TransformedMARCRecordDTO;
import org.folio.innreach.external.dto.BibContributionRequest;
import org.folio.innreach.external.service.InnReachContributionService;

@Log4j2
@JobScope
@Service
@RequiredArgsConstructor
public class InstanceContributor implements ItemWriter<Instance> {

  private static final String MARC_BIB_FORMAT = "ISO2709";

  private final TenantScopedExecutionService tenantScopedExecutionService;
  private final MARCRecordTransformationService marcRecordTransformationService;
  private final ContributionCriteriaConfigurationService contributionConfigService;
  private final InnReachContributionService contributionService;

  private final ContributionJobContext jobContext;

  @Override
  public void write(List<? extends Instance> items) {
    if (items.isEmpty()) {
      return;
    }
    tenantScopedExecutionService.executeTenantScoped(
      jobContext.getTenantId(),
      () -> {
        items.forEach(item -> contributeInstance(jobContext.getCentralServerId(), item));
        return null;
      }
    );
  }

  private void contributeInstance(UUID centralServerId, Instance instance) {
    var marc = marcRecordTransformationService.transformRecord(centralServerId, instance.getId());

    var bibId = instance.getHrid();

    var bib = BibContributionRequest.builder()
      .bibId(bibId)
      .marc21BibFormat(MARC_BIB_FORMAT)
      .marc21BibData(getEncodedMARCRecord(marc))
      .titleHoldCount(0)
      .itemCount(0)
      .suppress(suppress(centralServerId, instance))
      .build();

    contribute(centralServerId, bibId, bib);

    verifyContribution(centralServerId, bibId);
  }

  private void contribute(UUID centralServerId, String bibId, BibContributionRequest bib) {
    var response = contributionService.contributeBib(centralServerId, bibId, bib);
    Assert.isTrue(response.isOk(), "Unexpected contribution response: " + response);
  }

  private void verifyContribution(UUID centralServerId, String bibId) {
    var response = contributionService.lookUpBib(centralServerId, bibId);
    Assert.isTrue(response.isOk(), "Unexpected verification response: " + response);
  }

  private String getEncodedMARCRecord(TransformedMARCRecordDTO marc) {
    byte[] contentBytes = marc.getContent().getBytes(StandardCharsets.UTF_8);
    return Base64Utils.encodeToString(contentBytes);
  }

  private Character suppress(UUID centralServerId, Instance instance) {
    var config = getContributionConfig(centralServerId);
    if (config == null) {
      return null;
    }

    var statisticalCodeIds = emptyIfNull(instance.getStatisticalCodeIds());

    var excludedId = config.getDoNotContributeId();
    if (statisticalCodeIds.contains(excludedId)) {
      return 'n';
    }

    var suppressId = config.getContributeButSuppressId();
    if (statisticalCodeIds.contains(suppressId)) {
      return 'y';
    }

    var systemOwnedId = config.getContributeAsSystemOwnedId();
    if (statisticalCodeIds.contains(systemOwnedId)) {
      return 'g';
    }

    return null;
  }

  private ContributionCriteriaDTO getContributionConfig(UUID centralServerId) {
    ContributionCriteriaDTO config;
    try {
      config = contributionConfigService.getCriteria(centralServerId);
    } catch (Exception e) {
      log.warn("Unable to load contribution config for central server = {}", centralServerId, e);
      return null;
    }
    return config;
  }

}
