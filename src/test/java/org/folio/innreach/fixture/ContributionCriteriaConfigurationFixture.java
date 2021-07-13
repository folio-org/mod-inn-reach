package org.folio.innreach.fixture;

import static org.jeasy.random.FieldPredicates.named;

import java.time.OffsetDateTime;

import lombok.experimental.UtilityClass;
import org.jeasy.random.EasyRandom;
import org.jeasy.random.EasyRandomParameters;

import org.folio.innreach.domain.entity.ContributionCriteriaConfiguration;

@UtilityClass
public class ContributionCriteriaConfigurationFixture {

  private static final EasyRandom contributionCriteriaRandom;

  static {
    EasyRandomParameters params = new EasyRandomParameters()
        .randomize(named("createdBy"), () -> "admin")
        .randomize(named("createdDate"), OffsetDateTime::now)
        .excludeField(named("centralServer"))
        .excludeField(named("lastModifiedBy"))
        .excludeField(named("lastModifiedDate"));

    contributionCriteriaRandom = new EasyRandom(params);
  }

  public static ContributionCriteriaConfiguration createContributionCriteriaConfiguration() {
    return contributionCriteriaRandom.nextObject(ContributionCriteriaConfiguration.class);
  }

}
