package org.folio.innreach.controller;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.http.HttpStatus.CONFLICT;
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
import org.folio.innreach.dto.PagingSlipTemplateDTO;
import org.folio.innreach.mapper.PagingSlipTemplateMapper;
import org.folio.innreach.repository.PagingSlipTemplateRepository;

@Sql(
  scripts = {
    "classpath:db/paging-slip-template/clear-paging-slip-template.sql",
    "classpath:db/central-server/clear-central-server-tables.sql"},
  executionPhase = AFTER_TEST_METHOD
)
@SqlMergeMode(MERGE)
class PagingSlipTemplateControllerTest extends BaseControllerTest {

  private static final String PRE_POPULATED_CENTRAL_SERVER_ID = "edab6baf-c696-42b1-89bb-1bbb8759b0d2";
  private static final String PRE_POPULATED_PAGING_SLIP_TEMPLATE_ID = "a731991d-310d-43c6-938a-626ff9b8d6b6";

  @Autowired
  private TestRestTemplate testRestTemplate;
  @Autowired
  private PagingSlipTemplateRepository repository;
  @Autowired
  private PagingSlipTemplateMapper mapper;

  @Test
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/paging-slip-template/pre-populate-paging-slip-template.sql"
  })
  void shouldGetExistingTemplate(){
    var responseEntity = testRestTemplate.getForEntity(
      "/inn-reach/central-servers/{centralServerId}/paging-slip-template", PagingSlipTemplateDTO.class,
      PRE_POPULATED_CENTRAL_SERVER_ID);

    assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
    assertTrue(responseEntity.hasBody());

    var templateDTO = responseEntity.getBody();

    assertEquals(UUID.fromString(PRE_POPULATED_PAGING_SLIP_TEMPLATE_ID), templateDTO.getId());
    assertEquals("description", templateDTO.getDescription());
    assertEquals("template", templateDTO.getTemplate());
  }

  @Test
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql"
  })
  void shouldCreatePagingSlipTemplate() {
    var templateDTO = deserializeFromJsonFile(
      "/paging-slip-template/create-paging-slip-template-request.json", PagingSlipTemplateDTO.class);

    var responseEntity = testRestTemplate.postForEntity(
      "/inn-reach/central-servers/{centralServerId}/paging-slip-template",
      templateDTO, PagingSlipTemplateDTO.class, PRE_POPULATED_CENTRAL_SERVER_ID);

    assertEquals(HttpStatus.CREATED, responseEntity.getStatusCode());
    assertTrue(responseEntity.hasBody());

    var created = repository.fetchOneByCentralServerId(UUID.fromString(PRE_POPULATED_CENTRAL_SERVER_ID));

    assertTrue(created.isPresent());
    var createdTemplate = created.get();

    assertNotNull(createdTemplate.getId());
    assertEquals(templateDTO.getDescription(), createdTemplate.getDescription());
    assertEquals(templateDTO.getTemplate(), createdTemplate.getTemplate());
  }

  @Test
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/paging-slip-template/pre-populate-paging-slip-template.sql"
  })
  void shouldUpdateExistingTemplate() {
    var existing = mapper.toDTO(repository.fetchOneByCentralServerId(UUID.fromString(PRE_POPULATED_CENTRAL_SERVER_ID)).get());
    existing.setDescription("new description");
    existing.setTemplate("new template");

    var responseEntity = testRestTemplate.exchange(
      "/inn-reach/central-servers/{centralServerId}/paging-slip-template",
      HttpMethod.PUT, new HttpEntity<>(existing), Void.class, PRE_POPULATED_CENTRAL_SERVER_ID);

    assertEquals(HttpStatus.NO_CONTENT, responseEntity.getStatusCode());
    assertFalse(responseEntity.hasBody());

    var updated = mapper.toDTO(repository.fetchOneByCentralServerId(
      UUID.fromString(PRE_POPULATED_CENTRAL_SERVER_ID)).get());

    assertEquals(existing.getDescription(), updated.getDescription());
    assertEquals(existing.getTemplate(), updated.getTemplate());
  }

  @Test
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/paging-slip-template/pre-populate-paging-slip-template.sql"
  })
  void shouldDeleteExistingTemplate() {
    var responseEntity = testRestTemplate.exchange("/inn-reach/central-servers/{centralServerId}/paging-slip-template",
      HttpMethod.DELETE, HttpEntity.EMPTY, PagingSlipTemplateDTO.class, PRE_POPULATED_CENTRAL_SERVER_ID);

    assertEquals(HttpStatus.NO_CONTENT, responseEntity.getStatusCode());

    var deleted = repository.fetchOneByCentralServerId(
      UUID.fromString(PRE_POPULATED_CENTRAL_SERVER_ID));

    assertFalse(deleted.isPresent());
  }

  @Test
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/paging-slip-template/pre-populate-paging-slip-template.sql"
  })
  void return409WhenCreatingTemplateAndServerAlreadyHasOne() {
    var newTemplate = deserializeFromJsonFile("/paging-slip-template/create-paging-slip-template-request.json",
     PagingSlipTemplateDTO.class);

    var responseEntity = testRestTemplate.postForEntity(
      "/inn-reach/central-servers/{centralServerId}/paging-slip-template", newTemplate,
      Error.class, PRE_POPULATED_CENTRAL_SERVER_ID);

    assertEquals(CONFLICT, responseEntity.getStatusCode());
    assertNotNull(responseEntity.getBody());
    assertThat(responseEntity.getBody().getMessage(), containsString("constraint [paging_slip_template_central_server_id_key]"));
  }

  @Test
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql"
  })
  void return409WhenCreatingTemplateWithInvalidData() {
    var newTemplate = deserializeFromJsonFile("/paging-slip-template/create-paging-slip-template-invalid-request.json",
      PagingSlipTemplateDTO.class);

    var responseEntity = testRestTemplate.postForEntity(
      "/inn-reach/central-servers/{centralServerId}/paging-slip-template", newTemplate,
      Error.class, PRE_POPULATED_CENTRAL_SERVER_ID);

    assertEquals(CONFLICT, responseEntity.getStatusCode());
  }

  @Test
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql"
  })
  void return404HttpCodeWhenPagingSLipTemplateNotFound() {
    var responseEntity = testRestTemplate.getForEntity(
      "/inn-reach/central-servers/{centralServerId}/paging-slip-template",
      PagingSlipTemplateDTO.class, PRE_POPULATED_CENTRAL_SERVER_ID);

    assertEquals(HttpStatus.NOT_FOUND, responseEntity.getStatusCode());
  }
}
