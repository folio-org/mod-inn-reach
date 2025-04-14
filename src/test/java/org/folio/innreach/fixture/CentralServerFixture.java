package org.folio.innreach.fixture;

import static org.folio.innreach.fixture.CentralServerCredentialsFixture.createCentralServerCredentials;
import static org.folio.innreach.fixture.LocalAgencyFixture.createLocalAgency;
import static org.folio.innreach.fixture.LocalServerCredentialsFixture.createLocalServerCredentials;
import static org.folio.innreach.fixture.TestUtil.randomFiveCharacterCode;
import static org.folio.innreach.fixture.TestUtil.randomUUIDString;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.folio.innreach.domain.dto.CentralServerConnectionDetailsDTO;
import org.folio.innreach.domain.entity.CentralServer;
import org.folio.innreach.domain.entity.base.AuditableUser;
import org.folio.innreach.dto.CentralServerDTO;
import org.folio.innreach.dto.LocalAgencyDTO;

public class CentralServerFixture {

  private static final String CENTRAL_SERVER_ADDRESS = "https://centralserveraddress.com";

  public static CentralServer createCentralServer() {
    var centralServer = new CentralServer();
    centralServer.setName(randomUUIDString());
    centralServer.setDescription("folio central server");
    centralServer.setCentralServerCode(randomFiveCharacterCode());
    centralServer.setLocalServerCode(randomFiveCharacterCode());
    centralServer.setCentralServerAddress(CENTRAL_SERVER_ADDRESS);
    centralServer.setLoanTypeId(UUID.randomUUID());
    centralServer.addLocalAgency(createLocalAgency());
    centralServer.addLocalAgency(createLocalAgency());
    centralServer.setCentralServerCredentials(createCentralServerCredentials());
    centralServer.setLocalServerCredentials(createLocalServerCredentials());
    centralServer.setCreatedDate(OffsetDateTime.now());
    centralServer.setCreatedBy(AuditableUser.SYSTEM);
    centralServer.setCheckPickupLocation(false);

    return centralServer;
  }

  public static CentralServerDTO createCentralServerDTO() {
    return new CentralServerDTO()
      .name("name")
      .description("description")
      .localServerCode(randomFiveCharacterCode())
      .centralServerAddress(CENTRAL_SERVER_ADDRESS)
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
      .localServerKey(UUID.randomUUID())
      .localServerSecret(randomUUIDString());
  }

  public static CentralServerConnectionDetailsDTO createCentralServerConnectionDetailsDTO() {
    return CentralServerConnectionDetailsDTO.builder()
      .id(UUID.randomUUID())
      .connectionUrl(CENTRAL_SERVER_ADDRESS)
      .localCode(randomFiveCharacterCode())
      .key(randomUUIDString())
      .secret(randomUUIDString())
      .build();
  }
}
