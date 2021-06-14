package org.folio.innreach.fixture;

import static org.folio.innreach.fixture.TestUtil.randomUUIDString;

import org.folio.innreach.domain.entity.LocalServerCredentials;

public class LocalServerCredentialsFixture {

  public static LocalServerCredentials createLocalServerCredentials() {
    var localServerCredentials = new LocalServerCredentials();
    localServerCredentials.setLocalServerKey(randomUUIDString());
    localServerCredentials.setLocalServerSecret(randomUUIDString());

    return localServerCredentials;
  }
}
