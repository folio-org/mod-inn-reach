package org.folio.innreach.fixture;

import org.folio.innreach.domain.entity.CentralServerCredentials;

import static org.folio.innreach.fixture.TestUtil.randomUUIDString;

public class CentralServerCredentialsFixture {

  public static CentralServerCredentials createCentralServerCredentials() {
    var centralServerCredentials = new CentralServerCredentials();
    centralServerCredentials.setCentralServerKey(randomUUIDString());
    centralServerCredentials.setCentralServerSecret(randomUUIDString());

    return centralServerCredentials;
  }
}
