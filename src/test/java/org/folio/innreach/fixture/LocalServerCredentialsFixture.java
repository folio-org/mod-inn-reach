package org.folio.innreach.fixture;

import org.folio.innreach.domain.entity.LocalServerCredentials;

import static org.folio.innreach.fixture.FixtureUtil.randomUUIDString;

public class LocalServerCredentialsFixture {

  public static LocalServerCredentials createLocalServerCredentials() {
    var localServerCredentials = new LocalServerCredentials();
    localServerCredentials.setLocalServerKey(randomUUIDString());
    localServerCredentials.setLocalServerSecret(randomUUIDString());
    localServerCredentials.setCentralServerSecretSalt(randomUUIDString());

    return localServerCredentials;
  }
}
