package org.folio.innreach.fixture;

import lombok.experimental.UtilityClass;
import org.folio.innreach.domain.entity.ContributionBehavior;
import org.folio.innreach.domain.entity.ContributionCriteriaConfiguration;
import org.folio.innreach.domain.entity.ContributionCriteriaExcludedLocation;
import org.folio.innreach.domain.entity.ContributionCriteriaStatisticalCodeBehavior;

import java.util.UUID;

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

  public static ContributionCriteriaConfiguration createTestContributionCriteriaConfiguration(UUID centralServerId) {
    ContributionCriteriaConfiguration configuration = new ContributionCriteriaConfiguration();
    configuration.setCentralServeId(centralServerId == null ? CENTRAL_SERVER_UUID : centralServerId);
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
}
