package org.folio.innreach.fixture;

import static org.jeasy.random.FieldPredicates.named;

import java.time.OffsetDateTime;

import lombok.experimental.UtilityClass;
import org.jeasy.random.EasyRandom;
import org.jeasy.random.EasyRandomParameters;

import org.folio.innreach.domain.entity.ContributionCriteriaConfiguration;
import org.folio.innreach.domain.entity.base.AuditableUser;

@UtilityClass
public class ContributionCriteriaConfigurationFixture {

  private static final EasyRandom contributionCriteriaRandom;

  static {
    EasyRandomParameters params = new EasyRandomParameters()
        .randomize(named("createdBy"), () -> AuditableUser.SYSTEM)
        .randomize(named("createdDate"), OffsetDateTime::now)
        .excludeField(named("centralServer"))
        .excludeField(named("updatedBy"))
        .excludeField(named("updatedDate"));

    contributionCriteriaRandom = new EasyRandom(params);
  }

  public static ContributionCriteriaConfiguration createContributionCriteriaConfiguration() {
    return contributionCriteriaRandom.nextObject(ContributionCriteriaConfiguration.class);
  }

}
