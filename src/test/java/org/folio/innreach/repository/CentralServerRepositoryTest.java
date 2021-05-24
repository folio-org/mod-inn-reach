package org.folio.innreach.repository;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.jdbc.Sql;

import org.folio.innreach.fixture.CentralServerCredentialsFixture;
import org.folio.innreach.fixture.CentralServerFixture;
import org.folio.innreach.fixture.LocalAgencyFixture;
import org.folio.innreach.fixture.LocalServerCredentialsFixture;


@Sql(scripts = "classpath:db/pre-populate-central-server.sql")
class CentralServerRepositoryTest extends BaseRepositoryTest {

  private static final String PRE_POPULATED_CENTRAL_SERVER_ID = "edab6baf-c696-42b1-89bb-1bbb8759b0d2";
  private static final String PRE_POPULATED_CENTRAL_SERVER_CODE = "abc12";

  @Autowired
  private CentralServerRepository centralServerRepository;

  @Test
  void saveCentralServer_when_suchCentralServerDoesNotExist() {
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
    var centralServerById = centralServerRepository.getOne(UUID.fromString(PRE_POPULATED_CENTRAL_SERVER_ID));

    assertNotNull(centralServerById.getLocalServerCredentials().getLocalServerKey());
    assertNotNull(centralServerById.getCentralServerCredentials().getCentralServerKey());
    assertNotNull(centralServerById.getLocalAgencies());
    assertFalse(centralServerById.getLocalAgencies().isEmpty());
    assertFalse(centralServerById.getLocalAgencies().get(0).getFolioLibraryIds().isEmpty());
  }

  @Test
  void throwException_when_saveCentralServerWithoutRequiredData() {
    var centralServer = CentralServerFixture.createCentralServer();
    centralServer.setLocalServerCode(null);

    assertThrows(DataIntegrityViolationException.class, () -> centralServerRepository.saveAndFlush(centralServer));
  }

  @Test
  void throwException_when_suchCentralServerAlreadyExists() {
    var centralServer = CentralServerFixture.createCentralServer();
    centralServer.setLocalServerCode(PRE_POPULATED_CENTRAL_SERVER_CODE);

    assertThrows(DataIntegrityViolationException.class, () -> centralServerRepository.saveAndFlush(centralServer));
  }

}
