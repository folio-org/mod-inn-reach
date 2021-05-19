package org.folio.innreach.repository;

import org.folio.innreach.domain.entity.recordscontribution.ContributionCriteriaConfiguration;
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
import static org.junit.jupiter.api.Assertions.assertThrows;

@DataJpaTest
//@ActiveProfiles("local")
@ActiveProfiles("sav")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Sql(scripts = "classpath:db/pre-populate-central-server.sql")
class CentralServerRepositoryWithContributionConfigurationIT {

  private static final String PRE_POPULATED_CENTRAL_SERVER_ID = "edab6baf-c696-42b1-89bb-1bbb8759b0d2";
  private static final String PRE_POPULATED_CENTRAL_SERVER_CODE = "q1w2e";

  @Autowired
  private CentralServerRepository centralServerRepository;

  @Test
  void saveCentralServer_when_suchCentralServerDoesNotExist() {
    var centralServer = CentralServerFixture.createCentralServer();
    centralServer.setCentralServerCredentials(CentralServerCredentialsFixture.createCentralServerCredentials());
    centralServer.setLocalServerCredentials(LocalServerCredentialsFixture.createLocalServerCredentials());

    centralServer.addAgency(LocalAgencyFixture.createLocalAgency());
    centralServer.addAgency(LocalAgencyFixture.createLocalAgency());
    centralServer.addAgency(LocalAgencyFixture.createLocalAgency());

    var savedCentralServer = centralServerRepository.save(centralServer);

    assertNotNull(savedCentralServer);
    assertNotNull(savedCentralServer.getId());
    assertNotNull(savedCentralServer.getCentralServerCredentials());
    assertNotNull(savedCentralServer.getLocalServerCredentials());
  }

  @Test
  void getCentralServer_when_centralServerExists() {
    var centralServerById = centralServerRepository.getOne(UUID.fromString(PRE_POPULATED_CENTRAL_SERVER_ID));

    assertNotNull(centralServerById.getLocalServerCredentials().getLocalServerKey());
    assertNotNull(centralServerById.getCentralServerCredentials().getCentralServerKey());
    assertNotNull(centralServerById.getAgencies());
    assertFalse(centralServerById.getAgencies().isEmpty());
    assertFalse(centralServerById.getAgencies().get(0).getFolioLibrariesIds().isEmpty());
  }

  @Test
  void throwException_when_saveCentralServerWithoutRequiredData() {
    var centralServer = CentralServerFixture.createCentralServer();

    assertThrows(Exception.class, () -> centralServerRepository.save(centralServer));
  }

  @Test
  void throwException_when_suchCentralServerAlreadyExists() {
    var centralServer = CentralServerFixture.createCentralServer();
    centralServer.setLocalServerCode("q1w2e");
    centralServer.setCentralServerCredentials(CentralServerCredentialsFixture.createCentralServerCredentials());
    centralServer.setContributionCriteriaConfiguration(new ContributionCriteriaConfiguration());
    centralServerRepository.save(centralServer);

    assertThrows(Exception.class, () -> centralServerRepository.save(CentralServerFixture.createCentralServer()));
  }

}
