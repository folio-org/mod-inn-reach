package org.folio.innreach.fixture;

import static org.jeasy.random.FieldPredicates.named;

import java.util.UUID;

import lombok.experimental.UtilityClass;
import org.jeasy.random.EasyRandom;
import org.jeasy.random.EasyRandomParameters;

import org.folio.innreach.dto.AuthenticationRequest;

@UtilityClass
public class AuthenticationRequestFixture {

  private static final EasyRandom authRequestRandom;

  static {
    EasyRandomParameters params = new EasyRandomParameters()
      .randomize(named("localServerCode"), TestUtil::randomFiveCharacterCode)
      .randomize(named("key"), UUID::randomUUID)
      .randomize(named("secret"), UUID::randomUUID);

    authRequestRandom = new EasyRandom(params);
  }

  public static AuthenticationRequest createAuthenticationRequest() {
    return authRequestRandom.nextObject(AuthenticationRequest.class);
  }
}
