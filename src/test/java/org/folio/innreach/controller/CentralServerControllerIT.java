package org.folio.innreach.controller;

import static org.folio.innreach.fixture.TestUtil.deserializeFromJsonFile;
import static org.folio.innreach.fixture.TestUtil.randomUUIDString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.AFTER_TEST_METHOD;
import static org.springframework.test.context.jdbc.SqlMergeMode.MergeMode.MERGE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.folio.innreach.dto.CentralServerDTO;
import org.folio.innreach.dto.CentralServersDTO;
import org.folio.innreach.it.base.BaseTenantIT;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlMergeMode;

@Sql(
  scripts = {
    "classpath:db/central-server/clear-central-server-tables.sql"},
  executionPhase = AFTER_TEST_METHOD
)
@SqlMergeMode(MERGE)
class CentralServerControllerIT extends BaseTenantIT {

  private static final String PRE_POPULATED_CENTRAL_SERVER_ID = "edab6baf-c696-42b1-89bb-1bbb8759b0d2";

  @Test
  void return200HttpCode_and_createdCentralServerEntity_when_createCentralServer() throws Exception {
    var centralServerRequestDTO = deserializeFromJsonFile(
      "/central-server/create-central-server-request.json", CentralServerDTO.class);
    centralServerRequestDTO.setCentralServerAddress(getWiremockUrl());

    var result = mockMvc.perform(post("/inn-reach/central-servers")
        .content(asJsonString(centralServerRequestDTO))
        .headers(defaultHeaders())
        .contentType(APPLICATION_JSON))
      .andExpect(status().is2xxSuccessful())
      .andReturn();

    var createdCentralServer = fromJson(result, CentralServerDTO.class);

    assertNotNull(createdCentralServer);
    assertFalse(createdCentralServer.getCheckPickupLocation());
  }

  @Test
  void return200HttpCode_and_createdCentralServerEntity_when_createCentralServerWithoutLocalServerCredentials() throws Exception {
    var centralServerRequestDTO = deserializeFromJsonFile(
      "/central-server/create-central-server-without-local-server-credentials-request.json", CentralServerDTO.class);
    centralServerRequestDTO.setCentralServerAddress(getWiremockUrl());

    var result = mockMvc.perform(post("/inn-reach/central-servers")
        .content(asJsonString(centralServerRequestDTO))
        .headers(defaultHeaders())
        .contentType(APPLICATION_JSON))
      .andExpect(status().is2xxSuccessful())
      .andReturn();

    var createdCentralServer = fromJson(result, CentralServerDTO.class);

    assertNotNull(createdCentralServer);
    assertNull(createdCentralServer.getLocalServerKey());
    assertNull(createdCentralServer.getLocalServerSecret());
    assertFalse(createdCentralServer.getCheckPickupLocation());
  }

  @Test
  void return400HttpCode_when_requestDataIsInvalid() throws Exception {
    var centralServerRequestDTO = deserializeFromJsonFile(
      "/central-server/create-central-server-invalid-request.json", CentralServerDTO.class);

    mockMvc.perform(post("/inn-reach/central-servers")
        .content(asJsonString(centralServerRequestDTO))
        .headers(defaultHeaders())
        .contentType(APPLICATION_JSON))
      .andExpect(status().is4xxClientError());
  }

  @Test
  @Sql(scripts = "classpath:db/central-server/pre-populate-central-server.sql")
  void return200HttpCode_and_allCentralServerEntities_when_getForAllCentralServers() throws Exception {
    var result = mockMvc.perform(get("/inn-reach/central-servers")
        .headers(defaultHeaders()))
      .andExpect(status().is2xxSuccessful())
      .andReturn();

    var centralServers = fromJson(result, CentralServersDTO.class);

    assertNotNull(centralServers);
    assertNotNull(centralServers.getCentralServers());
    assertEquals(1, centralServers.getCentralServers().size());
    assertEquals(1, centralServers.getTotalRecords());
  }

  @Test
  @Sql(scripts = "classpath:db/central-server/pre-populate-central-server.sql")
  void return200HttpCode_and_centralServerEntityById_when_getForOneCentralServer() throws Exception {
    var result = mockMvc.perform(get("/inn-reach/central-servers/{centralServerId}", PRE_POPULATED_CENTRAL_SERVER_ID)
        .headers(defaultHeaders()))
      .andExpect(status().is2xxSuccessful())
      .andReturn();

    var centralServer = fromJson(result, CentralServerDTO.class);

    assertNotNull(centralServer);
    assertFalse(centralServer.getCheckPickupLocation());
  }

  @Test
  void return404HttpCode_when_centralServerByIdNotFound() throws Exception {
    mockMvc.perform(get("/inn-reach/central-servers/{centralServerId}", randomUUIDString())
        .headers(defaultHeaders()))
      .andExpect(status().isNotFound());
  }

  @Test
  @Sql(scripts = "classpath:db/central-server/pre-populate-central-server.sql")
  void return200HttpCode_when_updateCentralServer() throws Exception {
    var centralServerRequestDTO = deserializeFromJsonFile(
      "/central-server/update-central-server-request.json", CentralServerDTO.class);
    centralServerRequestDTO.setCheckPickupLocation(true);

    mockMvc.perform(put("/inn-reach/central-servers/{centralServerId}", PRE_POPULATED_CENTRAL_SERVER_ID)
        .content(asJsonString(centralServerRequestDTO))
        .headers(defaultHeaders())
        .contentType(APPLICATION_JSON))
      .andExpect(status().is2xxSuccessful());

    var resultGet = mockMvc.perform(get("/inn-reach/central-servers/{centralServerId}", PRE_POPULATED_CENTRAL_SERVER_ID)
        .headers(defaultHeaders()))
      .andExpect(status().is2xxSuccessful())
      .andReturn();

    var centralServer = fromJson(resultGet, CentralServerDTO.class);

    assertNotNull(centralServer);
    assertTrue(centralServer.getCheckPickupLocation());
  }

  @Test
  void return404HttpCode_when_updatableCentralServerNotFound() throws Exception {
    var centralServerRequestDTO = deserializeFromJsonFile(
      "/central-server/update-central-server-request.json", CentralServerDTO.class);

    mockMvc.perform(put("/inn-reach/central-servers/{centralServerId}", randomUUIDString())
        .content(asJsonString(centralServerRequestDTO))
        .headers(defaultHeaders())
        .contentType(APPLICATION_JSON))
      .andExpect(status().isNotFound());
  }

  @Test
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql"
  })
  void return204HttpCode_when_deleteCentralServer() throws Exception {
    mockMvc.perform(delete("/inn-reach/central-servers/{centralServerId}", PRE_POPULATED_CENTRAL_SERVER_ID)
        .headers(defaultHeaders()))
      .andExpect(status().isNoContent());
  }

  @Test
  void return404HttpCode_when_deletableCentralServerNotFound() throws Exception {
    mockMvc.perform(delete("/inn-reach/central-servers/{centralServerId}", randomUUIDString())
        .headers(defaultHeaders()))
      .andExpect(status().isNotFound());
  }

  @Test
  @Sql(scripts = "classpath:db/central-server/pre-populate-central-server.sql")
  void return409HttpCode_when_createCentralServerWithUniqueViolation() throws Exception {
    var centralServerRequestDTO = deserializeFromJsonFile(
      "/central-server/create-central-server-request.json", CentralServerDTO.class);
    centralServerRequestDTO.setCentralServerAddress(getWiremockUrl());

    mockMvc.perform(post("/inn-reach/central-servers")
        .content(asJsonString(centralServerRequestDTO))
        .headers(defaultHeaders())
        .contentType(APPLICATION_JSON))
      .andExpect(status().isConflict());
  }

  @Test
  void return400HttpCode_when_createCentralServerWithDuplicateFolioLibraries() throws Exception {
    var centralServerRequestDTO = deserializeFromJsonFile(
      "/central-server/create-central-server-invalid-libraries-request.json", CentralServerDTO.class);
    centralServerRequestDTO.setCentralServerAddress(getWiremockUrl());

    mockMvc.perform(post("/inn-reach/central-servers")
        .content(asJsonString(centralServerRequestDTO))
        .headers(defaultHeaders())
        .contentType(APPLICATION_JSON))
      .andExpect(status().isBadRequest());
  }

  @Test
  @Sql(scripts = "classpath:db/central-server/pre-populate-central-server.sql")
  void return400HttpCode_when_updateCentralServerWithDuplicateFolioLibraries() throws Exception {
    var centralServerRequestDTO = deserializeFromJsonFile(
      "/central-server/update-central-server-invalid-libraries-request.json", CentralServerDTO.class);

    mockMvc.perform(put("/inn-reach/central-servers/{centralServerId}", PRE_POPULATED_CENTRAL_SERVER_ID)
        .content(asJsonString(centralServerRequestDTO))
        .headers(defaultHeaders())
        .contentType(APPLICATION_JSON))
      .andExpect(status().isBadRequest());
  }

  @Test
  void return200HttpCode_and_createdCentralServerEntity_when_createCentralServerWithPickupLocationCheckTrue() throws Exception {
    var centralServerRequestDTO = deserializeFromJsonFile(
      "/central-server/create-central-server-request.json", CentralServerDTO.class);
    centralServerRequestDTO.setCheckPickupLocation(true);
    centralServerRequestDTO.setCentralServerAddress(getWiremockUrl());

    var result = mockMvc.perform(post("/inn-reach/central-servers")
        .content(asJsonString(centralServerRequestDTO))
        .headers(defaultHeaders())
        .contentType(APPLICATION_JSON))
      .andExpect(status().is2xxSuccessful())
      .andReturn();

    var createdCentralServer = fromJson(result, CentralServerDTO.class);

    assertNotNull(createdCentralServer);
    assertTrue(createdCentralServer.getCheckPickupLocation());
  }
}
