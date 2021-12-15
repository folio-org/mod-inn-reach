package org.folio.innreach.fixture;

import lombok.experimental.UtilityClass;
import org.folio.innreach.domain.dto.folio.inventorystorage.ServicePointUserDTO;
import org.jeasy.random.EasyRandom;
import org.jeasy.random.EasyRandomParameters;

@UtilityClass
public class ServicePointUserFixture {
  private static final EasyRandom servicePointUserRandom;

  static {
    EasyRandomParameters params = new EasyRandomParameters();
    servicePointUserRandom = new EasyRandom(params);
  }

  public static ServicePointUserDTO createServicePointUserDTO() {
    return servicePointUserRandom.nextObject(ServicePointUserDTO.class);
  }
}
