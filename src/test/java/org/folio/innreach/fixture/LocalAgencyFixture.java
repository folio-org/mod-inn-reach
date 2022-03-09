package org.folio.innreach.fixture;

import org.folio.innreach.domain.entity.LocalAgency;

import java.util.UUID;

import static org.folio.innreach.fixture.TestUtil.randomFiveCharacterCode;

public class LocalAgencyFixture {

  public static LocalAgency createLocalAgency() {
    var localAgency = new LocalAgency();
    localAgency.setCode(randomFiveCharacterCode());
    localAgency.getFolioLibraries().add(new LocalAgency.FolioLibrary(UUID.randomUUID(), null));
    localAgency.getFolioLibraries().add(new LocalAgency.FolioLibrary(UUID.randomUUID(), null));
    return localAgency;
  }
}
