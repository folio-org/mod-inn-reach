package org.folio.innreach.fixture;

import static org.jeasy.random.FieldPredicates.named;

import java.time.OffsetDateTime;

import lombok.experimental.UtilityClass;
import org.jeasy.random.EasyRandom;
import org.jeasy.random.EasyRandomParameters;
import org.jeasy.random.randomizers.range.IntegerRangeRandomizer;

import org.folio.innreach.domain.entity.MaterialTypeMapping;

@UtilityClass
public class MaterialTypeMappingFixture {

  private static final EasyRandom mtypeRandom;

  static {
    EasyRandomParameters params = new EasyRandomParameters()
        .randomize(named("centralItemType"),new IntegerRangeRandomizer(0, 256))
        .randomize(named("createdBy"), () -> "admin")
        .randomize(named("createdDate"), OffsetDateTime::now)
        .excludeField(named("id"))
        .excludeField(named("centralServer"))
        .excludeField(named("lastModifiedBy"))
        .excludeField(named("lastModifiedDate"))
        .excludeField(named("metadata"));

    mtypeRandom = new EasyRandom(params);
  }

  public static MaterialTypeMapping createMaterialTypeMapping() {
    return mtypeRandom.nextObject(MaterialTypeMapping.class);
  }

}
