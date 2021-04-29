package org.folio.innreach.fixture;

import org.folio.innreach.domain.entity.LocalAgency;

import java.util.UUID;

import static org.folio.innreach.fixture.FixtureUtil.randomFiveCharacterCode;

public class LocalAgencyFixture {

  public static LocalAgency createLocalAgency() {
    var localAgency = new LocalAgency();
    localAgency.setCode(randomFiveCharacterCode());
    localAgency.getFolioLibraryIds().add(UUID.randomUUID());
    localAgency.getFolioLibraryIds().add(UUID.randomUUID());
    return localAgency;
  }
}
