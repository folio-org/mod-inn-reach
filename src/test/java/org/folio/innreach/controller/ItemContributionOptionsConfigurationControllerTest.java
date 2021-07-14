package org.folio.innreach.controller;

import org.folio.innreach.controller.base.BaseControllerTest;
import org.folio.innreach.dto.ItemContributionOptionsConfigurationDTO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlMergeMode;

import static org.folio.innreach.fixture.TestUtil.deserializeFromJsonFile;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.AFTER_TEST_METHOD;
import static org.springframework.test.context.jdbc.SqlMergeMode.MergeMode.MERGE;

@Sql(
  scripts = {
    "classpath:db/itm-contrib-opt-conf/clear-itm-contrib-opt-conf-tables.sql",
    "classpath:db/central-server/clear-central-server-tables.sql"
  },
  executionPhase = AFTER_TEST_METHOD
)
@SqlMergeMode(MERGE)
class ItemContributionOptionsConfigurationControllerTest extends BaseControllerTest {
  private static final String PRE_POPULATED_ITEM_CONTRIBUTION_OPTIONS_CONFIGURATION_ID = "edab6baf-c696-42b1-89bb-1bbb8759b0d2";

  @Autowired
  private TestRestTemplate testRestTemplate;

  @Test
  @Sql(scripts = {"classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/itm-contrib-opt-conf/pre-populate-itm-contrib-opt-conf.sql"
  })
  void return200HttpCode_and_itemContributionOptionsConfigurationById_when_getForOneItemContributionOptionsConfiguration() {
    var responseEntity = testRestTemplate.getForEntity(
      "/inn-reach/central-servers/{centralServerId}/contribution-options", ItemContributionOptionsConfigurationDTO.class, PRE_POPULATED_ITEM_CONTRIBUTION_OPTIONS_CONFIGURATION_ID);

    assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
    assertTrue(responseEntity.hasBody());

    var itemContributionOptionsConfigurationDTO = responseEntity.getBody();

    assertNotNull(itemContributionOptionsConfigurationDTO.getCentralServerId());
    assertEquals(PRE_POPULATED_ITEM_CONTRIBUTION_OPTIONS_CONFIGURATION_ID, itemContributionOptionsConfigurationDTO.getCentralServerId().toString());
    assertNotNull(itemContributionOptionsConfigurationDTO.getNotAvailableItemStatuses());
    assertNotNull(itemContributionOptionsConfigurationDTO.getNonLendableLoanTypes());
    assertNotNull(itemContributionOptionsConfigurationDTO.getNonLendableLocations());
    assertNotNull(itemContributionOptionsConfigurationDTO.getNonLendableMaterialTypes());
  }

  @Test
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql"
  })
  void return200HttpCode_and_createdItemContributionOptionsConfigurationEntity_when_createItemContributionOptionsConfiguration() {
    var itemContributionOptionsConfigurationDTO = deserializeFromJsonFile(
      "/item-contribution-options/create-item-contribution-options-configuration-request.json", ItemContributionOptionsConfigurationDTO.class);

    var responseEntity = testRestTemplate.postForEntity(
      "/inn-reach/central-servers/contribution-options", itemContributionOptionsConfigurationDTO, ItemContributionOptionsConfigurationDTO.class);

    assertEquals(HttpStatus.CREATED, responseEntity.getStatusCode());
    assertTrue(responseEntity.hasBody());

    var createdItemContributionOptionsConfiguration = responseEntity.getBody();

    assertNotNull(createdItemContributionOptionsConfiguration);
    assertNotNull(createdItemContributionOptionsConfiguration.getCentralServerId());
    assertEquals(itemContributionOptionsConfigurationDTO.getNotAvailableItemStatuses(),
      createdItemContributionOptionsConfiguration.getNotAvailableItemStatuses());
    assertEquals(itemContributionOptionsConfigurationDTO.getNonLendableLoanTypes(),
      createdItemContributionOptionsConfiguration.getNonLendableLoanTypes());
    assertEquals(itemContributionOptionsConfigurationDTO.getNonLendableLocations(),
      createdItemContributionOptionsConfiguration.getNonLendableLocations());
    assertEquals(itemContributionOptionsConfigurationDTO.getNonLendableMaterialTypes(),
      createdItemContributionOptionsConfiguration.getNonLendableMaterialTypes());
  }

  @Test
  @Sql(scripts = {"classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/itm-contrib-opt-conf/pre-populate-itm-contrib-opt-conf.sql"
  })
  void return204HttpCode_when_updateItemContributionOptionsConfiguration() {
    var itemContributionOptionsConfigurationDTO = deserializeFromJsonFile(
      "/item-contribution-options/update-item-contribution-options-configuration-request.json", ItemContributionOptionsConfigurationDTO.class);

    var responseEntity = testRestTemplate.exchange(
      "/inn-reach/central-servers/{centralServerId}/contribution-options", HttpMethod.PUT, new HttpEntity<>(itemContributionOptionsConfigurationDTO),
      ItemContributionOptionsConfigurationDTO.class, PRE_POPULATED_ITEM_CONTRIBUTION_OPTIONS_CONFIGURATION_ID);

    assertEquals(HttpStatus.NO_CONTENT, responseEntity.getStatusCode());
  }

  @Test
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql"
  })
  void return400HttpCode_when_requestDataIsInvalid() {
    var itemContributionOptionsConfigurationDTO = deserializeFromJsonFile(
      "/item-contribution-options/create-item-contribution-options-configuration-invalid-request.json", ItemContributionOptionsConfigurationDTO.class);

    var responseEntity = testRestTemplate.postForEntity(
      "/inn-reach/central-servers/contribution-options", itemContributionOptionsConfigurationDTO, ItemContributionOptionsConfigurationDTO.class);

    assertTrue(responseEntity.getStatusCode().is4xxClientError());
    assertTrue(responseEntity.hasBody());
  }

  @Test
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql"
  })
  void return404HttpCode_when_itemContributionOptionsConfigurationByIdNotFound() {
    var responseEntity = testRestTemplate.getForEntity(
      "/inn-reach/central-servers/{centralServerId}/contribution-options", ItemContributionOptionsConfigurationDTO.class, PRE_POPULATED_ITEM_CONTRIBUTION_OPTIONS_CONFIGURATION_ID);

    assertEquals(HttpStatus.NOT_FOUND, responseEntity.getStatusCode());
  }
}
