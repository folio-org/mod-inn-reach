package org.folio.innreach.fixture;

import org.folio.innreach.domain.entity.CentralServer;
import org.folio.innreach.domain.entity.CentralServerConfiguration;

public class CentralServerFixture {

  public static CentralServer createCentralServerEntity() {
    var serverConfiguration = new CentralServerConfiguration(
      "http://centralserveraddress",
      "centralServerKey",
      "centralServerSecret",
      "localServerKey",
      "localServerSecret"
    );

    return new CentralServer(null, "centralServer", null, "folio", serverConfiguration);
  }
}
