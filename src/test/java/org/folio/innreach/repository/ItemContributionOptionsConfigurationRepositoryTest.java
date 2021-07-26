package org.folio.innreach.repository;

import static org.folio.innreach.fixture.TestUtil.refCentralServer;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import static org.folio.innreach.fixture.ItemContributionOptionsConfigurationFixture.createItmContribOptConf;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.jdbc.Sql;

import org.folio.innreach.domain.entity.ItemContributionOptionsConfiguration;

class ItemContributionOptionsConfigurationRepositoryTest extends BaseRepositoryTest {
  private static final String PRE_POPULATED_ITM_CONTRIB_OPT_CONFIG_ID = "20e4363c-b6c2-4da2-ac68-7dffbd18e3ce";
  private static final String PRE_POPULATED_CENTRAL_SERVER_ID = "edab6baf-c696-42b1-89bb-1bbb8759b0d2";

  @Autowired
  private ItemContributionOptionsConfigurationRepository repository;

  @Test
  @Sql(scripts = {"classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/itm-contrib-opt-conf/pre-populate-itm-contrib-opt-conf.sql"})
  void getItmContribOptConf_when_itmContribOptConfExists() {
    var itmContribOptConfById = repository.getOne(UUID.fromString(PRE_POPULATED_ITM_CONTRIB_OPT_CONFIG_ID));

    assertNotNull(itmContribOptConfById);
    assertEquals(UUID.fromString(PRE_POPULATED_ITM_CONTRIB_OPT_CONFIG_ID), itmContribOptConfById.getId());
  }

  @Test
  @Sql(scripts = {"classpath:db/central-server/pre-populate-central-server.sql"})
  void saveItmContribOptConf_when_itmContribOptConfDoesNotExists() {
    var itmContribOptConf = createItmContribOptConf();
    itmContribOptConf.setCentralServer(refCentralServer(UUID.fromString(PRE_POPULATED_CENTRAL_SERVER_ID)));
    var savedItmContribOptConf = repository.save(itmContribOptConf);

    assertNotNull(savedItmContribOptConf);
    assertNotNull(savedItmContribOptConf.getCentralServer().getId());
    assertEquals(itmContribOptConf.getNonLendableLoanTypes(), savedItmContribOptConf.getNonLendableLoanTypes());
    assertEquals(itmContribOptConf.getNonLendableLocations(), savedItmContribOptConf.getNonLendableLocations());
    assertEquals(itmContribOptConf.getNonLendableMaterialTypes(), savedItmContribOptConf.getNonLendableMaterialTypes());
    assertEquals(itmContribOptConf.getNotAvailableItemStatuses(), savedItmContribOptConf.getNotAvailableItemStatuses());
  }

  @Test
  @Sql(scripts = {"classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/itm-contrib-opt-conf/pre-populate-itm-contrib-opt-conf.sql"})
  void updateItmContribOptConf_when_itmContribOptConfDataIsValid() {
    var savedItmContribOptConf = repository.getOne(UUID.fromString(PRE_POPULATED_ITM_CONTRIB_OPT_CONFIG_ID));

    List<UUID> updatedNonLendableLoanTypes = savedItmContribOptConf.getNonLendableLoanTypes();
    updatedNonLendableLoanTypes.add(UUID.randomUUID());
    List<UUID> updatedNonLendableLocations = savedItmContribOptConf.getNonLendableLocations();
    updatedNonLendableLocations.add(UUID.randomUUID());
    List<UUID> updatedNonLendableMaterialTypes = savedItmContribOptConf.getNonLendableMaterialTypes();
    updatedNonLendableMaterialTypes.add(UUID.randomUUID());
    List<String> updatedNotAvailableItemStatuses = savedItmContribOptConf.getNotAvailableItemStatuses();
    updatedNotAvailableItemStatuses.clear();

    savedItmContribOptConf.setNonLendableLoanTypes(updatedNonLendableLoanTypes);
    savedItmContribOptConf.setNonLendableLocations(updatedNonLendableLocations);
    savedItmContribOptConf.setNonLendableMaterialTypes(updatedNonLendableMaterialTypes);
    savedItmContribOptConf.setNotAvailableItemStatuses(updatedNotAvailableItemStatuses);

    var updatedItmContribOptConf = repository.save(savedItmContribOptConf);

    assertEquals(savedItmContribOptConf.getCentralServer().getId(), updatedItmContribOptConf.getCentralServer().getId());
    assertEquals(updatedNonLendableLoanTypes, updatedItmContribOptConf.getNonLendableLoanTypes());
    assertEquals(updatedNonLendableLocations, updatedItmContribOptConf.getNonLendableLocations());
    assertEquals(updatedNonLendableMaterialTypes, updatedItmContribOptConf.getNonLendableMaterialTypes());
    assertEquals(updatedNotAvailableItemStatuses, updatedItmContribOptConf.getNotAvailableItemStatuses());
  }

  @Test
  @Sql(scripts = {"classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/itm-contrib-opt-conf/pre-populate-itm-contrib-opt-conf.sql"})
  void deleteItmContribOptConf_when_itmContribOptConfExists() {
    UUID id = UUID.fromString(PRE_POPULATED_ITM_CONTRIB_OPT_CONFIG_ID);
    repository.deleteById(id);

    Optional<ItemContributionOptionsConfiguration> deletedItmContribOptConf = repository.findById(id);
    assertTrue(deletedItmContribOptConf.isEmpty());
  }

  @Test
  @Sql(scripts = {"classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/itm-contrib-opt-conf/pre-populate-itm-contrib-opt-conf.sql"})
  void throwException_when_updatingItmContribOptConfWithExcludedItemStatus() {
    var itmContribOptConf = repository.getOne(UUID.fromString(PRE_POPULATED_ITM_CONTRIB_OPT_CONFIG_ID));
    itmContribOptConf.getNotAvailableItemStatuses().add("Available");

    assertThrows(DataIntegrityViolationException.class, () -> repository.saveAndFlush(itmContribOptConf));
  }
}
