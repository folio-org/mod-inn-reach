package org.folio.innreach.fixture;

import lombok.experimental.UtilityClass;
import org.jeasy.random.EasyRandom;
import org.jeasy.random.EasyRandomParameters;

import org.folio.innreach.domain.dto.folio.circulation.RequestDTO;

@UtilityClass
public class RequestFixture {
  private static final EasyRandom requestRandom;

  static {
    EasyRandomParameters params = new EasyRandomParameters();
    requestRandom = new EasyRandom(params);
  }

  public static RequestDTO createRequestDTO() {
    return requestRandom.nextObject(RequestDTO.class);
  }
}
