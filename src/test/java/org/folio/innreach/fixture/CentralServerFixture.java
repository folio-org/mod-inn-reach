package org.folio.innreach.fixture;

import org.folio.innreach.domain.entity.CentralServer;

import static org.folio.innreach.fixture.FixtureUtil.randomUUIDString;

public class CentralServerFixture {

  public static CentralServer createCentralServer() {
    var centralServer = new CentralServer();
    centralServer.setName(randomUUIDString());
    centralServer.setDescription("folio central server");
    centralServer.setLocalServerCode(randomUUIDString().substring(0, 5));
    centralServer.setCentralServerAddress("https://centralserveraddress");
    centralServer.setLoanTypeId(randomUUIDString());
    return centralServer;
  }
}
