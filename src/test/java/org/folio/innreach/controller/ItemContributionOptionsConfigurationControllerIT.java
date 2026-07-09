package org.folio.innreach.controller;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.AFTER_TEST_METHOD;
import static org.springframework.test.context.jdbc.SqlMergeMode.MergeMode.MERGE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.folio.innreach.fixture.TestUtil.deserializeFromJsonFile;

import java.util.UUID;
import org.folio.innreach.dto.ItemContributionOptionsConfigurationDTO;
import org.folio.innreach.it.base.BaseTenantIT;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlMergeMode;
import org.folio.innreach.dto.Error;

@Sql(
  scripts = {
    "classpath:db/itm-contrib-opt-conf/clear-itm-contrib-opt-conf-tables.sql",
    "classpath:db/central-server/clear-central-server-tables.sql"
  },
  executionPhase = AFTER_TEST_METHOD
)
@SqlMergeMode(MERGE)
class ItemContributionOptionsConfigurationControllerIT extends BaseTenantIT {
  private static final String PRE_POPULATED_ITM_CONTRIB_OPT_CONF_ID = "20e4363c-b6c2-4da2-ac68-7dffbd18e3ce";
  private static final String PRE_POPULATED_CENTRAL_SERVER_ID = "edab6baf-c696-42b1-89bb-1bbb8759b0d2";

  @Test
  @Sql(scripts = {"classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/itm-contrib-opt-conf/pre-populate-itm-contrib-opt-conf.sql"
  })
  void return200HttpCode_and_itmContribOptConfById_when_getForOneItmContribOptConf() throws Exception {
    var result = mockMvc.perform(get("/inn-reach/central-servers/{centralServerId}/item-contribution-options",
        PRE_POPULATED_CENTRAL_SERVER_ID)
        .headers(defaultHeaders()))
      .andExpect(status().isOk())
      .andReturn();

    var itmContribOptConfDTO = fromJson(result, ItemContributionOptionsConfigurationDTO.class);

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
  void return200HttpCode_and_createdItmContribOptConfEntity_when_createItmContribOptConf() throws Exception {
    var itmContribOptConfDTO = deserializeFromJsonFile(
      "/item-contribution-options/create-itm-contrib-opt-conf-request.json", ItemContributionOptionsConfigurationDTO.class);

    var result = mockMvc.perform(post("/inn-reach/central-servers/{centralServerId}/item-contribution-options",
        PRE_POPULATED_CENTRAL_SERVER_ID)
        .content(asJsonString(itmContribOptConfDTO))
        .headers(defaultHeaders())
        .contentType(APPLICATION_JSON))
      .andExpect(status().isCreated())
      .andReturn();

    var createdItmContribOptConf = fromJson(result, ItemContributionOptionsConfigurationDTO.class);

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
  void return204HttpCode_when_updateItmContribOptConf() throws Exception {
    var itmContribOptConfDTO = deserializeFromJsonFile(
      "/item-contribution-options/update-itm-contrib-opt-conf-request.json", ItemContributionOptionsConfigurationDTO.class);

    mockMvc.perform(put("/inn-reach/central-servers/{centralServerId}/item-contribution-options",
        PRE_POPULATED_CENTRAL_SERVER_ID)
        .content(asJsonString(itmContribOptConfDTO))
        .headers(defaultHeaders())
        .contentType(APPLICATION_JSON))
      .andExpect(status().isNoContent());
  }

  @Test
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql"
  })
  void return400HttpCode_when_createItmContribOptConfRequestDataIsInvalid() throws Exception {
    var itmContribOptConfDTO = deserializeFromJsonFile(
      "/item-contribution-options/create-itm-contrib-opt-conf-invalid-request.json", ItemContributionOptionsConfigurationDTO.class);

    mockMvc.perform(post("/inn-reach/central-servers/{centralServerId}/item-contribution-options",
        PRE_POPULATED_CENTRAL_SERVER_ID)
        .content(asJsonString(itmContribOptConfDTO))
        .headers(defaultHeaders())
        .contentType(APPLICATION_JSON))
      .andExpect(status().is4xxClientError());
  }

  @Test
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql"
  })
  void return404HttpCode_when_itmContribOptConfByIdNotFound() throws Exception {
    mockMvc.perform(get("/inn-reach/central-servers/{centralServerId}/item-contribution-options",
        PRE_POPULATED_CENTRAL_SERVER_ID)
        .headers(defaultHeaders()))
      .andExpect(status().isNotFound());
  }

  @Test
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/itm-contrib-opt-conf/pre-populate-itm-contrib-opt-conf.sql"
  })
  void return409HttpCode_when_creatingItmContribOptConfWhenItmContribOptConfExists() throws Exception {
    var itmContribOptConfDTO = deserializeFromJsonFile(
      "/item-contribution-options/create-itm-contrib-opt-conf-request.json", ItemContributionOptionsConfigurationDTO.class);

    var result = mockMvc.perform(post("/inn-reach/central-servers/{centralServerId}/item-contribution-options",
        PRE_POPULATED_CENTRAL_SERVER_ID)
        .content(asJsonString(itmContribOptConfDTO))
        .headers(defaultHeaders())
        .contentType(APPLICATION_JSON))
      .andExpect(status().isConflict())
      .andReturn();

    var error = fromJson(result, Error.class);
    assertNotNull(error);
    assertThat(error.getMessage(), containsString("constraint [unq_central_server_id]"));
  }
}
