package org.folio.innreach.fixture;

import static org.jeasy.random.FieldPredicates.named;

import java.time.OffsetDateTime;

import lombok.experimental.UtilityClass;
import org.jeasy.random.EasyRandom;
import org.jeasy.random.EasyRandomParameters;
import org.jeasy.random.randomizers.text.StringRandomizer;

import org.folio.innreach.domain.entity.InnReachLocation;

@UtilityClass
public class InnReachLocationFixture {

  private static final EasyRandom locationRandom;

  static {
    EasyRandomParameters params = new EasyRandomParameters()
        .randomize(named("code"), TestUtil::randomFiveCharacterCode)
        .randomize(named("description"), new StringRandomizer(255))
        .randomize(named("createdBy"), () -> "admin")
        .randomize(named("createdDate"), OffsetDateTime::now)
        .excludeField(named("lastModifiedBy"))
        .excludeField(named("lastModifiedDate"));

    locationRandom = new EasyRandom(params);
  }
  
  public static InnReachLocation createInnReachLocation() {
    return locationRandom.nextObject(InnReachLocation.class);
  }
}