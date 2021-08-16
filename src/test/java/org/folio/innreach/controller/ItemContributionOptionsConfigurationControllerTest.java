package org.folio.innreach.controller;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.AFTER_TEST_METHOD;
import static org.springframework.test.context.jdbc.SqlMergeMode.MergeMode.MERGE;

import static org.folio.innreach.fixture.TestUtil.deserializeFromJsonFile;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlMergeMode;

import org.folio.innreach.controller.base.BaseControllerTest;
import org.folio.innreach.dto.ItemContributionOptionsConfigurationDTO;

@Sql(
  scripts = {
    "classpath:db/itm-contrib-opt-conf/clear-itm-contrib-opt-conf-tables.sql",
    "classpath:db/central-server/clear-central-server-tables.sql"
  },
  executionPhase = AFTER_TEST_METHOD
)
@SqlMergeMode(MERGE)
class ItemContributionOptionsConfigurationControllerTest extends BaseControllerTest {
  private static final String PRE_POPULATED_ITM_CONTRIB_OPT_CONF_ID = "20e4363c-b6c2-4da2-ac68-7dffbd18e3ce";
  private static final String PRE_POPULATED_CENTRAL_SERVER_ID = "edab6baf-c696-42b1-89bb-1bbb8759b0d2";

  @Autowired
  private TestRestTemplate testRestTemplate;

  @Test
  @Sql(scripts = {"classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/itm-contrib-opt-conf/pre-populate-itm-contrib-opt-conf.sql"
  })
  void return200HttpCode_and_itmContribOptConfById_when_getForOneItmContribOptConf() {
    var responseEntity = testRestTemplate.getForEntity(
      "/inn-reach/central-servers/{centralServerId}/item-contribution-options", ItemContributionOptionsConfigurationDTO.class,
      PRE_POPULATED_CENTRAL_SERVER_ID);

    assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
    assertTrue(responseEntity.hasBody());

    var itmContribOptConfDTO = responseEntity.getBody();

    assertEquals(UUID.fromString(PRE_POPULATED_ITM_CONTRIB_OPT_CONF_ID), itmContribOptConfDTO.getId());
    assertNotNull(itmContribOptConfDTO.getNotAvailableItemStatuses());
    assertNotNull(itmContribOptConfDTO.getNonLendableLoanTypes());
    assertNotNull(itmContribOptConfDTO.getNonLendableLocations());
    assertNotNull(itmContribOptConfDTO.getNonLendableMaterialTypes());
  }

  @Test
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql"
  })
  void return200HttpCode_and_createdItmContribOptConfEntity_when_createItmContribOptConf() {
    var itmContribOptConfDTO = deserializeFromJsonFile(
      "/item-contribution-options/create-itm-contrib-opt-conf-request.json", ItemContributionOptionsConfigurationDTO.class);

    var responseEntity = testRestTemplate.postForEntity(
      "/inn-reach/central-servers/{centralServerId}/item-contribution-options", itmContribOptConfDTO, ItemContributionOptionsConfigurationDTO.class,
      PRE_POPULATED_CENTRAL_SERVER_ID);

    assertEquals(HttpStatus.CREATED, responseEntity.getStatusCode());
    assertTrue(responseEntity.hasBody());

    var createdItmContribOptConf = responseEntity.getBody();

    assertNotNull(createdItmContribOptConf);
    assertEquals(itmContribOptConfDTO.getNotAvailableItemStatuses(), createdItmContribOptConf.getNotAvailableItemStatuses());
    assertEquals(itmContribOptConfDTO.getNonLendableLoanTypes(), createdItmContribOptConf.getNonLendableLoanTypes());
    assertEquals(itmContribOptConfDTO.getNonLendableLocations(), createdItmContribOptConf.getNonLendableLocations());
    assertEquals(itmContribOptConfDTO.getNonLendableMaterialTypes(), createdItmContribOptConf.getNonLendableMaterialTypes());
  }

  @Test
  @Sql(scripts = {"classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/itm-contrib-opt-conf/pre-populate-itm-contrib-opt-conf.sql"
  })
  void return204HttpCode_when_updateItmContribOptConf() {
    var itmContribOptConfDTO = deserializeFromJsonFile(
      "/item-contribution-options/update-itm-contrib-opt-conf-request.json", ItemContributionOptionsConfigurationDTO.class);

    var responseEntity = testRestTemplate.exchange(
      "/inn-reach/central-servers/{centralServerId}/item-contribution-options", HttpMethod.PUT, new HttpEntity<>(itmContribOptConfDTO),
      ItemContributionOptionsConfigurationDTO.class, PRE_POPULATED_CENTRAL_SERVER_ID);

    assertEquals(HttpStatus.NO_CONTENT, responseEntity.getStatusCode());
  }

  @Test
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql"
  })
  void return400HttpCode_when_createItmContribOptConfRequestDataIsInvalid() {
    var itmContribOptConfDTO = deserializeFromJsonFile(
      "/item-contribution-options/create-itm-contrib-opt-conf-invalid-request.json", ItemContributionOptionsConfigurationDTO.class);

    var responseEntity = testRestTemplate.postForEntity(
      "/inn-reach/central-servers/{centralServerId}/item-contribution-options", itmContribOptConfDTO, ItemContributionOptionsConfigurationDTO.class,
      PRE_POPULATED_CENTRAL_SERVER_ID);

    assertTrue(responseEntity.getStatusCode().is4xxClientError());
    assertTrue(responseEntity.hasBody());
  }

  @Test
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql"
  })
  void return404HttpCode_when_itmContribOptConfByIdNotFound() {
    var responseEntity = testRestTemplate.getForEntity(
      "/inn-reach/central-servers/{centralServerId}/item-contribution-options", ItemContributionOptionsConfigurationDTO.class,
      PRE_POPULATED_CENTRAL_SERVER_ID);

    assertEquals(HttpStatus.NOT_FOUND, responseEntity.getStatusCode());
  }

  @Test
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/itm-contrib-opt-conf/pre-populate-itm-contrib-opt-conf.sql"
  })
  void return409HttpCode_when_creatingItmContribOptConfWhenItmContribOptConfExists() {
    var itmContribOptConfDTO = deserializeFromJsonFile(
      "/item-contribution-options/create-itm-contrib-opt-conf-request.json", ItemContributionOptionsConfigurationDTO.class);

    var responseEntity = testRestTemplate.postForEntity(
      "/inn-reach/central-servers/{centralServerId}/item-contribution-options", itmContribOptConfDTO, Error.class,
      PRE_POPULATED_CENTRAL_SERVER_ID);

    assertEquals(HttpStatus.CONFLICT, responseEntity.getStatusCode());
    assertNotNull(responseEntity.getBody());
    assertThat(responseEntity.getBody().getMessage(), containsString("constraint [unq_central_server_id]"));
  }
}
