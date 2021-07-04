package org.folio.innreach.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.jdbc.Sql;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Sql(scripts = {"classpath:db/central-server/pre-populate-central-server.sql",
  "classpath:db/inn-reach-location/pre-populate-inn-reach-location-code.sql",
  "classpath:db/itm-contrib-opt-conf/pre-populate-itm-contrib-opt-conf.sql"})
class ItemContributionOptionsConfigurationRepositoryTest extends BaseRepositoryTest {
  private static final String PRE_POPULATED_ITEM_CONTRIBUTION_OPTIONS_CONFIGURATION_ID = "edab6baf-c696-42b1-89bb-1bbb8759b0d2";
  private static final String PRE_POPULATED_CENTRAL_SERVER_ID = "edab6baf-c696-42b1-89bb-1bbb8759b0d2";

  @Autowired
  private ItemContributionOptionsConfigurationRepository itemContributionOptionsConfigurationRepository;

  @Test
  void getItmContribOptConf_when_itmContribOptConfExists() {
    var itmContribOptConfById = itemContributionOptionsConfigurationRepository.getOne(UUID.fromString(PRE_POPULATED_ITEM_CONTRIBUTION_OPTIONS_CONFIGURATION_ID));

    assertNotNull(itmContribOptConfById);
    assertEquals(UUID.fromString(PRE_POPULATED_ITEM_CONTRIBUTION_OPTIONS_CONFIGURATION_ID), itmContribOptConfById.getId());
    assertEquals(UUID.fromString(PRE_POPULATED_CENTRAL_SERVER_ID), itmContribOptConfById.getCentralServer().getId());
  }

  @Test
  void throwException_when_updatingItmContribOptConfWithExcludedItemStatus() {
    var itmContribOptConf = itemContributionOptionsConfigurationRepository.getOne(UUID.fromString(PRE_POPULATED_ITEM_CONTRIBUTION_OPTIONS_CONFIGURATION_ID));
    itmContribOptConf.getStatuses().add("Available");

    assertThrows(DataIntegrityViolationException.class, () -> itemContributionOptionsConfigurationRepository.saveAndFlush(itmContribOptConf));
  }
}
