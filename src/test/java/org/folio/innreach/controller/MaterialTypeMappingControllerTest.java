package org.folio.innreach.controller;


import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.AFTER_TEST_METHOD;

import static org.folio.innreach.fixture.TestUtil.deserializeFromJsonFile;
import static org.folio.innreach.fixture.TestUtil.randomUUIDString;

import java.util.List;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.jdbc.Sql;

import org.folio.innreach.controller.base.BaseControllerTest;
import org.folio.innreach.domain.dto.CentralServerDTO;
import org.folio.innreach.domain.entity.MaterialTypeMapping;
import org.folio.innreach.dto.MaterialTypeMappingDTO;
import org.folio.innreach.dto.MaterialTypeMappingsDTO;
import org.folio.innreach.mapper.MaterialTypeMappingMapper;
import org.folio.innreach.repository.MaterialTypeMappingRepository;

@Sql(
  scripts = {
    "classpath:db/mtype-mapping/clear-material-type-mapping-table.sql",
    "classpath:db/central-server/clear-central-server-tables.sql"},
  executionPhase = AFTER_TEST_METHOD
)
class MaterialTypeMappingControllerTest extends BaseControllerTest {

  private static final String PRE_POPULATED_CENTRAL_SERVER_ID = "edab6baf-c696-42b1-89bb-1bbb8759b0d2";

  @Autowired
  private TestRestTemplate testRestTemplate;
  @Autowired
  private MaterialTypeMappingRepository repository;
  @Autowired
  private MaterialTypeMappingMapper mapper;


  @Test
  @Sql(scripts = {
      "classpath:db/central-server/pre-populate-central-server.sql",
      "classpath:db/mtype-mapping/pre-populate-material-type-mapping.sql"
  })
  void shouldGetAllExistingMappingsForCentralServer() {
    var responseEntity = testRestTemplate.getForEntity(baseMappingURL(), MaterialTypeMappingsDTO.class);

    assertTrue(responseEntity.getStatusCode().is2xxSuccessful());
    assertTrue(responseEntity.hasBody());

    var response = responseEntity.getBody();
    assertNotNull(response);

    var mappings = response.getMappings();

    List<MaterialTypeMapping> dbMappings = repository.findAll();

    assertEquals(dbMappings.size(), response.getTotalRecords());
    assertThat(mappings, containsInAnyOrder(entitiesToDTOs(dbMappings)));
  }

  @Test
  @Sql(scripts = {
      "classpath:db/central-server/pre-populate-central-server.sql",
  })
  void shouldGetEmptyMappingsWith0TotalIfNotSetForCentralServer() {
    var responseEntity = testRestTemplate.getForEntity(baseMappingURL(), MaterialTypeMappingsDTO.class);

    assertTrue(responseEntity.getStatusCode().is2xxSuccessful());
    assertTrue(responseEntity.hasBody());

    var response = responseEntity.getBody();
    assertNotNull(response);

    var mappings = response.getMappings();

    assertEquals(0, response.getTotalRecords());
    assertThat(mappings, is(empty()));
  }

  @Test
  @Disabled("review")
  @Sql(scripts = "classpath:db/central-server/clear-central-server-tables.sql")
  void return200HttpCode_and_createdCentralServerEntity_when_createCentralServer() {
    var centralServerRequestDTO = deserializeFromJsonFile(
      "/central-server/create-central-server-request.json", CentralServerDTO.class);

    var responseEntity = testRestTemplate.postForEntity(
      "/inn-reach/central-servers", centralServerRequestDTO, CentralServerDTO.class);

    assertTrue(responseEntity.getStatusCode().is2xxSuccessful());
    assertTrue(responseEntity.hasBody());

    var createdCentralServer = responseEntity.getBody();

    assertEquals(centralServerRequestDTO, createdCentralServer);
  }

  @Test
  @Disabled("review")
  @Sql(scripts = "classpath:db/central-server/clear-central-server-tables.sql")
  void return400HttpCode_when_requestDataIsInvalid() {
    var centralServerRequestDTO = deserializeFromJsonFile(
      "/central-server/create-central-server-invalid-request.json", CentralServerDTO.class);

    var responseEntity = testRestTemplate.postForEntity(
      "/inn-reach/central-servers", centralServerRequestDTO, CentralServerDTO.class);

    assertTrue(responseEntity.getStatusCode().is4xxClientError());
    assertTrue(responseEntity.hasBody());
  }

  @Test
  @Disabled("review")
  @Sql(scripts = {
    "classpath:db/central-server/clear-central-server-tables.sql",
    "classpath:db/central-server/pre-populate-central-server.sql"
  })
  void return200HttpCode_and_centralServerEntityById_when_getForOneCentralServer() {
    var centralServerDTO = deserializeFromJsonFile(
      "/central-server/create-central-server-request.json", CentralServerDTO.class);

    var responseEntity = testRestTemplate.getForEntity(
      "/inn-reach/central-servers/{centralServerId}", CentralServerDTO.class, PRE_POPULATED_CENTRAL_SERVER_ID);

    assertTrue(responseEntity.getStatusCode().is2xxSuccessful());
    assertTrue(responseEntity.hasBody());

    var centralServer = responseEntity.getBody();

    assertNotNull(centralServer);
    assertEquals(centralServerDTO, centralServer);
  }

  @Test
  @Disabled("review")
  @Sql(scripts = "classpath:db/central-server/clear-central-server-tables.sql")
  void return404HttpCode_when_centralServerByIdNotFound() {
    var responseEntity = testRestTemplate.getForEntity(
      "/inn-reach/central-servers/{centralServerId}", CentralServerDTO.class, randomUUIDString());

    assertEquals(HttpStatus.NOT_FOUND, responseEntity.getStatusCode());
  }

  @Test
  @Disabled("review")
  @Sql(scripts = {
    "classpath:db/central-server/clear-central-server-tables.sql",
    "classpath:db/central-server/pre-populate-central-server.sql"
  })
  void return200HttpCode_when_updateCentralServer() {
    var centralServerRequestDTO = deserializeFromJsonFile(
      "/central-server/update-central-server-request.json", CentralServerDTO.class);

    var responseEntity = testRestTemplate.exchange(
      "/inn-reach/central-servers/{centralServerId}", HttpMethod.PUT, new HttpEntity<>(centralServerRequestDTO),
      CentralServerDTO.class, PRE_POPULATED_CENTRAL_SERVER_ID);

    assertTrue(responseEntity.getStatusCode().is2xxSuccessful());
    assertTrue(responseEntity.hasBody());

    var updatedCentralServer = responseEntity.getBody();

    assertEquals(centralServerRequestDTO, updatedCentralServer);
  }

  @Test
  @Disabled("review")
  @Sql(scripts = "classpath:db/central-server/clear-central-server-tables.sql")
  void return404HttpCode_when_updatableCentralServerNotFound() {
    var centralServerRequestDTO = deserializeFromJsonFile(
      "/central-server/update-central-server-request.json", CentralServerDTO.class);

    var responseEntity = testRestTemplate.exchange(
      "/inn-reach/central-servers/{centralServerId}", HttpMethod.PUT, new HttpEntity<>(centralServerRequestDTO),
      CentralServerDTO.class, randomUUIDString());

    assertEquals(HttpStatus.NOT_FOUND, responseEntity.getStatusCode());
  }

  @Test
  @Disabled("review")
  @Sql(scripts = {
    "classpath:db/central-server/clear-central-server-tables.sql",
    "classpath:db/central-server/pre-populate-central-server.sql"
  })
  void return204HttpCode_when_deleteCentralServer() {
    var responseEntity = testRestTemplate.exchange(
      "/inn-reach/central-servers/{centralServerId}", HttpMethod.DELETE, HttpEntity.EMPTY,
      CentralServerDTO.class, PRE_POPULATED_CENTRAL_SERVER_ID);

    assertEquals(HttpStatus.NO_CONTENT, responseEntity.getStatusCode());
  }

  @Test
  @Disabled("review")
  @Sql(scripts = "classpath:db/central-server/clear-central-server-tables.sql")
  void return404HttpCode_when_deletableCentralServerNotFound() {
    var responseEntity = testRestTemplate.exchange(
      "/inn-reach/central-servers/{centralServerId}", HttpMethod.DELETE, HttpEntity.EMPTY,
      CentralServerDTO.class, randomUUIDString());

    assertEquals(HttpStatus.NOT_FOUND, responseEntity.getStatusCode());
  }

  private static String baseMappingURL() {
    return "/inn-reach/central-servers/" + PRE_POPULATED_CENTRAL_SERVER_ID + "/material-type-mappings";
  }

  private MaterialTypeMappingDTO[] entitiesToDTOs(List<MaterialTypeMapping> dbMappings) {
    MaterialTypeMappingDTO[] result = new MaterialTypeMappingDTO[dbMappings.size()];

    int i = 0;
    for (MaterialTypeMapping dbMapping : dbMappings) {
      result[i++] = mapper.mapToDTO(dbMapping);
    }

    return result;
  }

}
