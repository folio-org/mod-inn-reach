package org.folio.innreach.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.AFTER_TEST_METHOD;
import static org.springframework.test.context.jdbc.SqlMergeMode.MergeMode.MERGE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.folio.innreach.fixture.TestUtil.deserializeFromJsonFile;

import java.util.UUID;
import org.folio.innreach.external.client.InnReachAuthClient;
import org.folio.innreach.external.dto.AccessTokenDTO;
import org.folio.innreach.it.base.BaseTenantIT;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlMergeMode;
import org.folio.innreach.domain.entity.base.AuditableUser;
import org.folio.innreach.dto.InnReachLocationDTO;
import org.folio.innreach.dto.InnReachLocationsDTO;

@Sql(
  scripts = "classpath:db/inn-reach-location/clear-inn-reach-location-tables.sql",
  executionPhase = AFTER_TEST_METHOD
)
@SqlMergeMode(MERGE)
class InnReachLocationControllerIT extends BaseTenantIT {

	private static final String PRE_POPULATED_LOCATION1_ID = "a1c1472f-67ec-4938-b5a8-f119e51ab79b";
	private static final AuditableUser PRE_POPULATED_USER = AuditableUser.SYSTEM;

  @MockitoBean
  private InnReachAuthClient innReachAuthClient;

  @BeforeEach
  void init() {
    when(innReachAuthClient.getAccessToken(any(), any())).thenReturn(ResponseEntity.ok(new AccessTokenDTO()));
  }

	@Test
	void return200HttpCode_and_createdInnReachLocation_when_createInnReachLocation() throws Exception {
    var innReachLocationDTO = deserializeFromJsonFile("/inn-reach-location/create-inn-reach-location-request.json",
        InnReachLocationDTO.class);

    mockMvc.perform(post("/inn-reach/locations")
        .content(asJsonString(innReachLocationDTO))
        .headers(defaultHeaders())
        .contentType(APPLICATION_JSON))
      .andExpect(status().isCreated());
  }

	@Test
	void return400HttpCode_when_createInnReachLocation_and_requestDataIsInvalid() throws Exception {
    var innReachLocationDTO = deserializeFromJsonFile("/inn-reach-location/create-inn-reach-location-request.json",
        InnReachLocationDTO.class);
    innReachLocationDTO.setCode("qwerty123");

    mockMvc.perform(post("/inn-reach/locations")
        .content(asJsonString(innReachLocationDTO))
        .headers(defaultHeaders())
        .contentType(APPLICATION_JSON))
      .andExpect(status().isBadRequest());
  }

  @Test
  @Sql(scripts = "classpath:db/inn-reach-location/pre-populate-inn-reach-location-code.sql")
	void return200HttpStatus_and_innReachLocation_when_innReachLocationExists() throws Exception {
    var result = mockMvc.perform(get("/inn-reach/locations/{locationId}", PRE_POPULATED_LOCATION1_ID)
        .headers(defaultHeaders()))
      .andExpect(status().isOk())
      .andReturn();

    var innReachLocationDTO = fromJson(result, InnReachLocationDTO.class);
    assertNotNull(innReachLocationDTO);

    assertEquals(UUID.fromString(PRE_POPULATED_LOCATION1_ID), innReachLocationDTO.getId());

    var metadata = innReachLocationDTO.getMetadata();

    assertNotNull(metadata);
    assertEquals(PRE_POPULATED_USER.getName(), metadata.getCreatedByUsername());
  }

	@Test
	void return404HttpCode_when_innReachLocationDoesNotExist() throws Exception {
    mockMvc.perform(get("/inn-reach/locations/{locationId}", UUID.randomUUID().toString())
        .headers(defaultHeaders()))
      .andExpect(status().isNotFound());
  }

	@Test
	@Sql(scripts = "classpath:db/inn-reach-location/pre-populate-inn-reach-location-code.sql")
	void return200HttpCode_and_allInReachLocations_when_innReachLocationsExist() throws Exception {
    var result = mockMvc.perform(get("/inn-reach/locations")
        .headers(defaultHeaders()))
      .andExpect(status().isOk())
      .andReturn();

    var innReachLocationsDTO = fromJson(result, InnReachLocationsDTO.class);

    assertNotNull(innReachLocationsDTO);
    assertNotNull(innReachLocationsDTO.getLocations());
    assertFalse(innReachLocationsDTO.getLocations().isEmpty());
  }

  @Test
  @Sql(scripts = "classpath:db/inn-reach-location/pre-populate-inn-reach-location-code.sql")
	void return200HttpCode_and_updatedInnReachLocation_when_innReachLocationsExist() throws Exception {
    var innReachLocationDTO = deserializeFromJsonFile("/inn-reach-location/update-inn-reach-location-request.json",
        InnReachLocationDTO.class);

    mockMvc.perform(put("/inn-reach/locations/{locationId}", PRE_POPULATED_LOCATION1_ID)
        .content(asJsonString(innReachLocationDTO))
        .headers(defaultHeaders())
        .contentType(APPLICATION_JSON))
      .andExpect(status().isNoContent());
	}

	@Test
	void return404HttpCode_when_updatableInnReachLocationDoesNotExist() throws Exception {
    var innReachLocationDTO = deserializeFromJsonFile("/inn-reach-location/update-inn-reach-location-request.json",
        InnReachLocationDTO.class);

    mockMvc.perform(put("/inn-reach/locations/{locationId}", UUID.randomUUID().toString())
        .content(asJsonString(innReachLocationDTO))
        .headers(defaultHeaders())
        .contentType(APPLICATION_JSON))
      .andExpect(status().isNotFound());
  }

	@Test
  @Sql(scripts = "classpath:db/inn-reach-location/pre-populate-inn-reach-location-code.sql")
	void return204HttpCode_when_deleteInnReachLocation() throws Exception {
    mockMvc.perform(delete("/inn-reach/locations/{locationId}", PRE_POPULATED_LOCATION1_ID)
        .headers(defaultHeaders()))
      .andExpect(status().isNoContent());
  }

	@Test
	void return404HttpCode_when_deletableInnReachLocationDoesNotExist() throws Exception {
    mockMvc.perform(delete("/inn-reach/locations/{locationId}", UUID.randomUUID().toString())
        .headers(defaultHeaders()))
      .andExpect(status().isNotFound());
  }

}
