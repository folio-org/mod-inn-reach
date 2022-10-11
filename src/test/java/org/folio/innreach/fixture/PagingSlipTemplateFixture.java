package org.folio.innreach.fixture;

import static org.jeasy.random.FieldPredicates.named;

import java.time.OffsetDateTime;

import lombok.experimental.UtilityClass;
import org.jeasy.random.EasyRandom;
import org.jeasy.random.EasyRandomParameters;

import org.folio.innreach.domain.entity.PagingSlipTemplate;
import org.folio.innreach.domain.entity.base.AuditableUser;

@UtilityClass
public class PagingSlipTemplateFixture {
  private static final EasyRandom templateRandom;

  static {
    EasyRandomParameters params = new EasyRandomParameters()
      .randomize(named("createdBy"), () -> AuditableUser.SYSTEM)
      .randomize(named("createdDate"), OffsetDateTime::now)
      .excludeField(named("centralServer"))
      .excludeField(named("updatedBy"))
      .excludeField(named("updatedDate"));

    templateRandom = new EasyRandom(params);
  }

  public static PagingSlipTemplate createPagingSlipTemplate() {
    return templateRandom.nextObject(PagingSlipTemplate.class);
  }
}
