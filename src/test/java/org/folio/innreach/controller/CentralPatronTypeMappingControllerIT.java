package org.folio.innreach.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.AFTER_TEST_METHOD;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.folio.innreach.fixture.TestUtil.deserializeFromJsonFile;

import org.folio.innreach.dto.CentralPatronTypeMappingsDTO;
import org.folio.innreach.external.client.InnReachAuthClient;
import org.folio.innreach.external.dto.AccessTokenDTO;
import org.folio.innreach.it.base.BaseTenantIT;
import org.folio.innreach.repository.CentralPatronTypeMappingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.jdbc.Sql;

@Sql(
  scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql",
      "classpath:db/central-patron-type-mapping/pre-populate-central-patron_type-mapping-table.sql"
  }
)
@Sql(
  scripts = {
    "classpath:db/central-patron-type-mapping/clear-central-patron-type-mapping-table.sql",
    "classpath:db/central-server/clear-central-server-tables.sql"
  },
  executionPhase = AFTER_TEST_METHOD
)
class CentralPatronTypeMappingControllerIT extends BaseTenantIT {

  private static final String PRE_POPULATED_CENTRAL_SERVER_ID = "edab6baf-c696-42b1-89bb-1bbb8759b0d2";

  @MockitoBean
  private InnReachAuthClient innReachAuthClient;

  @Autowired
  private CentralPatronTypeMappingRepository repository;

  @BeforeEach
  void init() {
    when(innReachAuthClient.getAccessToken(any(), any())).thenReturn(ResponseEntity.ok(new AccessTokenDTO()));
  }

  @Test
  void getAllExistingMappings() throws Exception {
    var result = mockMvc.perform(get("/inn-reach/central-servers/{centralServerId}/central-patron-type-mappings",
        PRE_POPULATED_CENTRAL_SERVER_ID)
        .headers(defaultHeaders()))
      .andExpect(status().is2xxSuccessful())
      .andReturn();

    var response = fromJson(result, CentralPatronTypeMappingsDTO.class);

    assertNotNull(response);

    var mappings = response.getCentralPatronTypeMappings();

    assertEquals(1, mappings.size());
  }

  @Test
  void updateAllExistingMappings() throws Exception {
    var centralPatronTypeMappingsDTO = deserializeFromJsonFile(
      "/central-patron-type-mappings/update-central-patron-type-mappings-request.json", CentralPatronTypeMappingsDTO.class);

    centralPatronTypeMappingsDTO.getCentralPatronTypeMappings().get(0).setId(null);
    centralPatronTypeMappingsDTO.getCentralPatronTypeMappings().get(1).setId(null);

    mockMvc.perform(put("/inn-reach/central-servers/{centralServerId}/central-patron-type-mappings",
        PRE_POPULATED_CENTRAL_SERVER_ID)
        .content(asJsonString(centralPatronTypeMappingsDTO))
        .headers(defaultHeaders())
        .contentType(APPLICATION_JSON))
      .andExpect(status().isNoContent());

    var updCentralPatronTypeMappings = repository.findAll();

    assertEquals(centralPatronTypeMappingsDTO.getCentralPatronTypeMappings().size(), updCentralPatronTypeMappings.size());
  }

}
