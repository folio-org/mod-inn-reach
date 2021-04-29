package org.folio.innreach.repository;

import org.folio.innreach.fixture.CentralServerCredentialsFixture;
import org.folio.innreach.fixture.CentralServerFixture;
import org.folio.innreach.fixture.LocalAgencyFixture;
import org.folio.innreach.fixture.LocalServerCredentialsFixture;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@DataJpaTest
@ActiveProfiles("local")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Sql(scripts = "classpath:db/pre-populate-central-server.sql")
class CentralServerRepositoryIT {

  private static final String PRE_POPULATED_CENTRAL_SERVER_ID = "edab6baf-c696-42b1-89bb-1bbb8759b0d2";
  private static final String PRE_POPULATED_CENTRAL_SERVER_CODE = "q1w2e";

  @Autowired
  private CentralServerRepository centralServerRepository;

  @Test
  void saveCentralServer_when_centralServerIsNew() {
    var centralServer = CentralServerFixture.createCentralServer();
    centralServer.setCentralServerCredentials(CentralServerCredentialsFixture.createCentralServerCredentials());
    centralServer.setLocalServerCredentials(LocalServerCredentialsFixture.createLocalServerCredentials());

    centralServer.addLocalAgency(LocalAgencyFixture.createLocalAgency());
    centralServer.addLocalAgency(LocalAgencyFixture.createLocalAgency());
    centralServer.addLocalAgency(LocalAgencyFixture.createLocalAgency());

    var savedCentralServer = centralServerRepository.save(centralServer);

    assertNotNull(savedCentralServer);
    assertNotNull(savedCentralServer.getId());
    assertNotNull(savedCentralServer.getCentralServerCredentials());
    assertNotNull(savedCentralServer.getLocalServerCredentials());
  }

  @Test
  void getCentralServer_when_centralServerExists() {
    var centralServer = centralServerRepository.getOne(UUID.fromString(PRE_POPULATED_CENTRAL_SERVER_ID));

    assertNotNull(centralServer.getLocalServerCredentials().getLocalServerKey());
    assertNotNull(centralServer.getCentralServerCredentials().getCentralServerKey());
    assertNotNull(centralServer.getLocalAgencies());
    assertFalse(centralServer.getLocalAgencies().isEmpty());
    assertFalse(centralServer.getLocalAgencies().get(0).getFolioLibraryIds().isEmpty());
  }

}
