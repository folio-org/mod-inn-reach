package org.folio.innreach.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.AFTER_TEST_METHOD;

import static org.folio.innreach.fixture.TestUtil.deserializeFromJsonFile;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.jdbc.Sql;

import org.folio.innreach.controller.base.BaseControllerTest;
import org.folio.innreach.dto.CentralPatronTypeMappingsDTO;
import org.folio.innreach.repository.CentralPatronTypeMappingRepository;

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
class CentralPatronTypeMappingControllerTest extends BaseControllerTest {

  private static final String PRE_POPULATED_CENTRAL_SERVER_ID = "edab6baf-c696-42b1-89bb-1bbb8759b0d2";

  @Autowired
  private TestRestTemplate testRestTemplate;

  @Autowired
  private CentralPatronTypeMappingRepository repository;

  @Test
  void getAllExistingMappings() {
    var responseEntity = testRestTemplate.getForEntity(
      "/inn-reach/central-servers/{centralServerId}/central-patron-type-mappings", CentralPatronTypeMappingsDTO.class,
      PRE_POPULATED_CENTRAL_SERVER_ID);

    assertTrue(responseEntity.getStatusCode().is2xxSuccessful());
    assertTrue(responseEntity.hasBody());

    var response = responseEntity.getBody();

    assertNotNull(response);

    var mappings = response.getCentralPatronTypeMappings();

    assertEquals(1, mappings.size());
  }

  @Test
  void updateAllExistingMappings() {
    var centralPatronTypeMappingsDTO = deserializeFromJsonFile(
      "/central-patron-type-mappings/update-central-patron-type-mappings-request.json", CentralPatronTypeMappingsDTO.class);

    centralPatronTypeMappingsDTO.getCentralPatronTypeMappings().get(0).setId(null);
    centralPatronTypeMappingsDTO.getCentralPatronTypeMappings().get(1).setId(null);

    var responseEntity = testRestTemplate.exchange(
      "/inn-reach/central-servers/{centralServerId}/central-patron-type-mappings", HttpMethod.PUT,
      new HttpEntity<>(centralPatronTypeMappingsDTO), Void.class, PRE_POPULATED_CENTRAL_SERVER_ID);

    assertEquals(HttpStatus.NO_CONTENT, responseEntity.getStatusCode());

    var updCentralPatronTypeMappings = repository.findAll();

    assertEquals(centralPatronTypeMappingsDTO.getCentralPatronTypeMappings().size(), updCentralPatronTypeMappings.size());
  }

}
