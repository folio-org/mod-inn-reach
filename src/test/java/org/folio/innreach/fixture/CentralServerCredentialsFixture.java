package org.folio.innreach.fixture;

import org.folio.innreach.domain.entity.CentralServerCredentials;

import java.util.UUID;

public class CentralServerCredentialsFixture {

  public static CentralServerCredentials createCentralServerCredentialsEntity() {
    var centralServerCredentials = new CentralServerCredentials();
    centralServerCredentials.setCentralServerKey(UUID.randomUUID().toString());
    centralServerCredentials.setCentralServerSecret(UUID.randomUUID().toString());
    centralServerCredentials.setCentralServerSecretSalt(UUID.randomUUID().toString());
    centralServerCredentials.setLocalServerKey(UUID.randomUUID().toString());
    centralServerCredentials.setLocalServerSecret(UUID.randomUUID().toString());

    return centralServerCredentials;
  }
}
