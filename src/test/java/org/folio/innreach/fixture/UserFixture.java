package org.folio.innreach.fixture;

import static java.util.UUID.randomUUID;
import static org.jeasy.random.FieldPredicates.named;

import lombok.experimental.UtilityClass;
import org.jeasy.random.EasyRandom;
import org.jeasy.random.EasyRandomParameters;

import org.folio.innreach.domain.dto.folio.User;

@UtilityClass
public class UserFixture {
  private static final EasyRandom userRandom;

  static {
    EasyRandomParameters params = new EasyRandomParameters()
      .randomize(named("id"), () -> randomUUID().toString())
      .randomize(named("active"), () -> true);
    userRandom = new EasyRandom(params);
  }

  public static User createUser() {
    return userRandom.nextObject(User.class);
  }
}
