package org.folio.innreach.fixture;

import static org.folio.innreach.fixture.CentralServerCredentialsFixture.createCentralServerCredentials;
import static org.folio.innreach.fixture.LocalAgencyFixture.createLocalAgency;
import static org.folio.innreach.fixture.LocalServerCredentialsFixture.createLocalServerCredentials;
import static org.folio.innreach.fixture.TestUtil.randomFiveCharacterCode;
import static org.folio.innreach.fixture.TestUtil.randomUUIDString;

import java.util.List;
import java.util.UUID;

import org.folio.innreach.domain.entity.CentralServer;
import org.folio.innreach.dto.CentralServerDTO;
import org.folio.innreach.dto.LocalAgencyDTO;

public class CentralServerFixture {

  public static CentralServer createCentralServer() {
    var centralServer = new CentralServer();
    centralServer.setName(randomUUIDString());
    centralServer.setDescription("folio central server");
    centralServer.setLocalServerCode(randomFiveCharacterCode());
    centralServer.setCentralServerAddress("https://centralserveraddress");
    centralServer.setLoanTypeId(UUID.randomUUID());
    centralServer.addLocalAgency(createLocalAgency());
    centralServer.addLocalAgency(createLocalAgency());
    centralServer.setCentralServerCredentials(createCentralServerCredentials());
    centralServer.setLocalServerCredentials(createLocalServerCredentials());

    return centralServer;
  }

  public static CentralServerDTO createCentralServerDTO() {
    return new CentralServerDTO()
      .name("name")
      .description("description")
      .localServerCode(randomFiveCharacterCode())
      .centralServerAddress("http://centralserveraddress")
      .loanTypeId(UUID.randomUUID())
      .localAgencies(List.of(
        new LocalAgencyDTO()
          .code(randomFiveCharacterCode())
          .folioLibraryIds(List.of(UUID.randomUUID(), UUID.randomUUID())),
        new LocalAgencyDTO()
          .code(randomFiveCharacterCode())
          .folioLibraryIds(List.of(UUID.randomUUID(), UUID.randomUUID()))
      ))
      .centralServerKey(randomUUIDString())
      .centralServerSecret(randomUUIDString())
      .localServerKey(randomUUIDString())
      .localServerSecret(randomUUIDString());
  }
}
