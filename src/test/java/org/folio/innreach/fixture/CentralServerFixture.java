package org.folio.innreach.fixture;

import org.folio.innreach.domain.entity.CentralServer;

public class CentralServerFixture {

  public static CentralServer createCentralServerEntity() {
    var centralServer = new CentralServer();
    centralServer.setName("folio");
    centralServer.setDescription("folio central server");
    centralServer.setLocalServerCode("qwe12");
    centralServer.setCentralServerAddress("https://centralserveraddress");

    return centralServer;
  }

  public static CentralServer createCentralServerEntityWithCredentials() {
    return new CentralServer(
      null,
      "folio",
      "folio central server",
      "qwe12",
      "https://centralserveraddress",
      CentralServerCredentialsFixture.createCentralServerCredentialsEntity()
    );
  }
}
