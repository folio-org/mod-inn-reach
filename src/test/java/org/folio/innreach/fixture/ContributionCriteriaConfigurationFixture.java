package org.folio.innreach.fixture;

import static org.jeasy.random.FieldPredicates.named;

import java.time.OffsetDateTime;
import java.util.Date;
import java.util.UUID;

import lombok.experimental.UtilityClass;
import org.jeasy.random.EasyRandom;
import org.jeasy.random.EasyRandomParameters;

import org.folio.innreach.domain.entity.ContributionCriteriaConfiguration;
import org.folio.innreach.dto.ContributionCriteriaDTO;
import org.folio.innreach.dto.Metadata;

@UtilityClass
public class ContributionCriteriaConfigurationFixture {
  public static final UUID CENTRAL_SERVER_UUID = UUID.fromString("91f89c86-2004-4f0e-807a-0c8247f293a6");

  public static final String[] EXCLUDED_LOCATION_IDs = {
    "efb089d4-4416-4892-ab81-bdfa00e4a2c3",
    "48a8dbb5-0a67-410f-b976-1b5cc7f97957",
    "f9ca8394-32ba-4fe5-825b-3a879fef65dd"
  };
  public static final String[] STATISTICAL_CODE_IDs = {
    "1d891eea-ef83-4952-93dc-9ca7fb9cb63e",
    "cc87119b-6c32-44b3-9c7d-24a7a3945edb",
    "db56b705-7c23-4fa1-9282-1a98b660c509"
  };

  private static final EasyRandom contributionCriteriaRandom;

  static {
    EasyRandomParameters params = new EasyRandomParameters()
      .randomize(named("centralServerId"), TestUtil::randomFiveCharacterCode)
      .randomize(named("createdBy"), () -> "admin")
      .randomize(named("createdDate"), OffsetDateTime::now)
      .excludeField(named("lastModifiedBy"))
      .excludeField(named("lastModifiedDate"))
      .excludeField(named("metadata"));

    contributionCriteriaRandom = new EasyRandom(params);
  }

  public static ContributionCriteriaConfiguration createRandomContributionCriteriaConfiguration() {
    return contributionCriteriaRandom.nextObject(ContributionCriteriaConfiguration.class);
  }

  public static ContributionCriteriaConfiguration createTestContributionCriteriaConfiguration(UUID centralServerId) {
    ContributionCriteriaConfiguration configuration = new ContributionCriteriaConfiguration();
    configuration.setCentralServerId(centralServerId == null ? CENTRAL_SERVER_UUID : centralServerId);
    for (int i = 0; i < EXCLUDED_LOCATION_IDs.length; i++) {
      ContributionCriteriaExcludedLocation excludedLocation = new ContributionCriteriaExcludedLocation();
      excludedLocation.setExcludedLocationId(UUID.fromString(EXCLUDED_LOCATION_IDs[i]));
      configuration.addExcludedLocation(excludedLocation);
    }
    for (int i = 0; i < STATISTICAL_CODE_IDs.length; i++) {
      ContributionCriteriaStatisticalCodeBehavior statisticalCodeBehavior = new ContributionCriteriaStatisticalCodeBehavior();
      statisticalCodeBehavior.setContributionBehavior(ContributionBehavior.values()[i]);
      statisticalCodeBehavior.setStatisticalCodeId(UUID.fromString(STATISTICAL_CODE_IDs[i]));
      configuration.addStatisticalCodeBehavior(statisticalCodeBehavior);
    }
    return configuration;
  }

  private ContributionCriteriaDTO stubContributionCriteriaDTO(UUID centralServerId) {
    var res = new ContributionCriteriaDTO();
    res.setCentralServerId(centralServerId);
    res.setContributeAsSystemOwnedId(UUID.randomUUID());
    res.setContributeButSuppressId(UUID.randomUUID());
    res.setDoNotContributeId(UUID.randomUUID());
    res.setMetadata(stubMetaData());
    res.addLocationIdsItem(UUID.randomUUID());
    res.addLocationIdsItem(UUID.randomUUID());
    res.addLocationIdsItem(UUID.randomUUID());
    return res;
  }

  private Metadata stubMetaData() {
    var metaData = new Metadata();
    metaData.setCreatedByUserId(UUID.randomUUID().toString());
    metaData.setUpdatedByUserId(UUID.randomUUID().toString());
    metaData.setCreatedDate(new Date());
    metaData.setUpdatedDate(new Date());
    return metaData;
  }

}
