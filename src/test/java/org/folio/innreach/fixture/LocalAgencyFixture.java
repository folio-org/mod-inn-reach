package org.folio.innreach.fixture;

import org.folio.innreach.domain.entity.LocalAgency;

import static org.folio.innreach.fixture.FixtureUtil.randomUUIDString;

public class LocalAgencyFixture {

  public static LocalAgency createLocalAgency() {
    var localAgency = new LocalAgency();
    localAgency.setCode(randomUUIDString().substring(0, 5));
    localAgency.addFolioLibraryId(randomUUIDString());
    localAgency.addFolioLibraryId(randomUUIDString());
    return localAgency;
  }
}
