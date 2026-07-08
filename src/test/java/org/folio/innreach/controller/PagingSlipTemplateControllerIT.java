package org.folio.innreach.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.AFTER_TEST_METHOD;
import static org.springframework.test.context.jdbc.SqlMergeMode.MergeMode.MERGE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.folio.innreach.fixture.TestUtil.deserializeFromJsonFile;

import java.util.UUID;
import org.folio.innreach.dto.PagingSlipTemplateDTO;
import org.folio.innreach.dto.PagingSlipTemplatesDTO;
import org.folio.innreach.external.client.InnReachAuthClient;
import org.folio.innreach.external.dto.AccessTokenDTO;
import org.folio.innreach.it.base.BaseTenantIT;
import org.folio.innreach.mapper.PagingSlipTemplateMapper;
import org.folio.innreach.repository.PagingSlipTemplateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlMergeMode;

@Sql(
  scripts = {
    "classpath:db/paging-slip-template/clear-paging-slip-template.sql",
    "classpath:db/central-server/clear-central-server-tables.sql"},
  executionPhase = AFTER_TEST_METHOD
)
@SqlMergeMode(MERGE)
class PagingSlipTemplateControllerIT extends BaseTenantIT {

  private static final UUID PRE_POPULATED_CENTRAL_SERVER_ID = UUID.fromString("edab6baf-c696-42b1-89bb-1bbb8759b0d2");
  private static final UUID PRE_POPULATED_PAGING_SLIP_TEMPLATE_ID = UUID.fromString("a731991d-310d-43c6-938a-626ff9b8d6b6");

  @MockitoBean
  private InnReachAuthClient innReachAuthClient;

  @Autowired
  private PagingSlipTemplateRepository repository;
  @Autowired
  private PagingSlipTemplateMapper mapper;

  @BeforeEach
  void init() {
    when(innReachAuthClient.getAccessToken(any(), any())).thenReturn(ResponseEntity.ok(new AccessTokenDTO()));
  }

  @Test
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/paging-slip-template/pre-populate-paging-slip-template.sql"
  })
  void shouldGetExistingTemplate() throws Exception {
    var result = mockMvc.perform(get("/inn-reach/central-servers/{centralServerId}/paging-slip-template",
        PRE_POPULATED_CENTRAL_SERVER_ID)
        .headers(defaultHeaders()))
      .andExpect(status().isOk())
      .andReturn();

    var templateDTO = fromJson(result, PagingSlipTemplateDTO.class);

    assertEquals(PRE_POPULATED_PAGING_SLIP_TEMPLATE_ID, templateDTO.getId());
    assertEquals(PRE_POPULATED_CENTRAL_SERVER_ID, templateDTO.getCentralServerId());
    assertEquals("description", templateDTO.getDescription());
    assertEquals("template", templateDTO.getTemplate());
  }

  @Test
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/paging-slip-template/pre-populate-paging-slip-template.sql"
  })
  void shouldGetAllExistingTemplates() throws Exception {
    var result = mockMvc.perform(get("/inn-reach/central-servers/paging-slip-template")
        .headers(defaultHeaders()))
      .andExpect(status().isOk())
      .andReturn();

    var templates = fromJson(result, PagingSlipTemplatesDTO.class);

    assertEquals(1, templates.getTotalRecords());
    var template = templates.getPagingSlipTemplates().get(0);

    assertEquals(PRE_POPULATED_PAGING_SLIP_TEMPLATE_ID, template.getId());
    assertEquals(PRE_POPULATED_CENTRAL_SERVER_ID, template.getCentralServerId());
    assertEquals("description", template.getDescription());
    assertEquals("template", template.getTemplate());
  }

  @Test
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql"
  })
  void shouldCreatePagingSlipTemplate() throws Exception {
    var templateDTO = deserializeFromJsonFile(
      "/paging-slip-template/create-paging-slip-template-request.json", PagingSlipTemplateDTO.class);

    mockMvc.perform(put("/inn-reach/central-servers/{centralServerId}/paging-slip-template",
        PRE_POPULATED_CENTRAL_SERVER_ID)
        .content(asJsonString(templateDTO))
        .headers(defaultHeaders())
        .contentType(APPLICATION_JSON))
      .andExpect(status().isNoContent());

    var created = repository.fetchOneByCentralServerId(PRE_POPULATED_CENTRAL_SERVER_ID);

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
  void shouldUpdateExistingTemplate() throws Exception {
    var existing = mapper.toDTO(repository.fetchOneByCentralServerId(PRE_POPULATED_CENTRAL_SERVER_ID).get());
    existing.setDescription("new description");
    existing.setTemplate("new template");

    mockMvc.perform(put("/inn-reach/central-servers/{centralServerId}/paging-slip-template",
        PRE_POPULATED_CENTRAL_SERVER_ID)
        .content(asJsonString(existing))
        .headers(defaultHeaders())
        .contentType(APPLICATION_JSON))
      .andExpect(status().isNoContent());

    var updated = mapper.toDTO(
      repository.fetchOneByCentralServerId(PRE_POPULATED_CENTRAL_SERVER_ID).get()
    );

    assertEquals(existing.getDescription(), updated.getDescription());
    assertEquals(existing.getTemplate(), updated.getTemplate());
    assertEquals(PRE_POPULATED_CENTRAL_SERVER_ID, updated.getCentralServerId());
  }

  @Test
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/paging-slip-template/pre-populate-paging-slip-template.sql"
  })
  void shouldDeleteExistingTemplate() throws Exception {
    mockMvc.perform(put("/inn-reach/central-servers/{centralServerId}/paging-slip-template",
        PRE_POPULATED_CENTRAL_SERVER_ID)
        .headers(defaultHeaders()))
      .andExpect(status().isNoContent());

    var deleted = repository.fetchOneByCentralServerId(PRE_POPULATED_CENTRAL_SERVER_ID);

    assertFalse(deleted.isPresent());
  }

  @Test
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql"
  })
  void return404HttpCodeWhenPagingSLipTemplateNotFound() throws Exception {
    mockMvc.perform(get("/inn-reach/central-servers/{centralServerId}/paging-slip-template",
        PRE_POPULATED_CENTRAL_SERVER_ID)
        .headers(defaultHeaders()))
      .andExpect(status().isNotFound());
  }
}
