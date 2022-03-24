package org.folio.innreach.fixture;

import static org.jeasy.random.FieldPredicates.named;

import java.time.OffsetDateTime;

import lombok.experimental.UtilityClass;
import org.jeasy.random.EasyRandom;
import org.jeasy.random.EasyRandomParameters;

import org.folio.innreach.domain.entity.VisiblePatronFieldConfiguration;
import org.folio.innreach.domain.entity.base.AuditableUser;

@UtilityClass
public class VisiblePatronFieldConfigurationFixture {
  private static final EasyRandom configRandom;

  static {
    EasyRandomParameters params = new EasyRandomParameters()
      .randomize(named("createdBy"), () -> AuditableUser.SYSTEM)
      .randomize(named("createdDate"), OffsetDateTime::now)
      .excludeField(named("centralServer"))
      .excludeField(named("updatedBy"))
      .excludeField(named("updatedDate"));

    configRandom = new EasyRandom(params);
  }

  public static VisiblePatronFieldConfiguration createVisiblePatronFieldConfiguration() {
    return configRandom.nextObject(VisiblePatronFieldConfiguration.class);
  }
}
