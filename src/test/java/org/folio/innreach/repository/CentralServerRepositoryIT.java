package org.folio.innreach.repository;

import org.folio.innreach.fixture.CentralServerFixture;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DataJpaTest
@ActiveProfiles("local")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class CentralServerRepositoryIT {

  @Autowired
  private CentralServerRepository centralServerRepository;

  @Test
  public void should_saveCentralServerIntoDatabase_when_suchCentralServerDoesNotExistInDatabase() {
    var savedCentralServer = centralServerRepository.save(CentralServerFixture.createCentralServerEntity());

    assertNotNull(savedCentralServer);
  }

  @Test
  public void shouldThrowException_when_suchCentralServerAlreadyExistsInDatabase() {
    centralServerRepository.save(CentralServerFixture.createCentralServerEntity());
    centralServerRepository.flush();

    assertThrows(Exception.class, () -> centralServerRepository.save(CentralServerFixture.createCentralServerEntity()));
  }

  @Test
  public void should_returnCentralServer_when_centralServerEntityExistsInDatabase() {
    var savedCentralServer = centralServerRepository.save(CentralServerFixture.createCentralServerEntity());

    var centralServer = centralServerRepository.getOne(savedCentralServer.getId());

    assertNotNull(centralServer);
  }

}
