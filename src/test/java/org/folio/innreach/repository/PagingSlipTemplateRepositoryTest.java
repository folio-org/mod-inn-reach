package org.folio.innreach.repository;

import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import static org.folio.innreach.fixture.MappingFixture.refCentralServer;
import static org.folio.innreach.fixture.PagingSlipTemplateFixture.createPagingSlipTemplate;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.jdbc.Sql;

import org.folio.innreach.domain.entity.PagingSlipTemplate;
import org.folio.innreach.domain.entity.base.AuditableUser;
import org.folio.innreach.fixture.TestUtil;

class PagingSlipTemplateRepositoryTest extends BaseRepositoryTest {
  private static final String PRE_POPULATED_CENTRAL_SERVER_ID = "edab6baf-c696-42b1-89bb-1bbb8759b0d2";
  private static final String PRE_POPULATED_PAGING_SLIP_TEMPLATE_ID = "a731991d-310d-43c6-938a-626ff9b8d6b6";
  private static final AuditableUser PRE_POPULATED_USER = AuditableUser.SYSTEM;

  @Autowired
  private PagingSlipTemplateRepository repository;

  @Test
  @Sql(scripts = {"classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/paging-slip-template/pre-populate-paging-slip-template.sql"})
  void shouldFindTemplateByCentralServerId() {
    var template = repository.fetchOneByCentralServerId(UUID.fromString(PRE_POPULATED_CENTRAL_SERVER_ID)).get();

    assertNotNull(template);
    assertEquals(UUID.fromString(PRE_POPULATED_PAGING_SLIP_TEMPLATE_ID), template.getId());
    assertEquals("description", template.getDescription());
    assertEquals("template", template.getTemplate());

    assertEquals(PRE_POPULATED_USER, template.getCreatedBy());
    assertNotNull(template.getCreatedDate());
  }

  @Test
  @Sql(scripts = {"classpath:db/central-server/pre-populate-central-server.sql"})
  void shouldSaveNewTemplate() {
    var newTemplate = createPagingSlipTemplate();
    newTemplate.setCentralServer(refCentralServer());

    var saved = repository.saveAndFlush(newTemplate);

    assertNotNull(saved);
    assertNotNull(saved.getId());
    assertEquals(newTemplate.getDescription(), saved.getDescription());
    assertEquals(newTemplate.getTemplate(), saved.getTemplate());
  }

  @Test
  @Sql(scripts = {"classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/paging-slip-template/pre-populate-paging-slip-template.sql"})
  void shouldUpdateExistingTemplate() {
    var template = repository.fetchOneByCentralServerId(UUID.fromString(PRE_POPULATED_CENTRAL_SERVER_ID)).get();

    var newDescription = "new description";
    var newTemplate = "new template";
    template.setDescription(newDescription);
    template.setTemplate(newTemplate);

    repository.saveAndFlush(template);

    var saved = repository.fetchOneByCentralServerId(UUID.fromString(PRE_POPULATED_CENTRAL_SERVER_ID)).get();

    assertEquals(newDescription, saved.getDescription());
    assertEquals(newTemplate, saved.getTemplate());
  }

  @Test
  @Sql(scripts = {"classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/paging-slip-template/pre-populate-paging-slip-template.sql"})
  void shouldDeleteExistingTemplate() {
    UUID id = fromString(PRE_POPULATED_PAGING_SLIP_TEMPLATE_ID);

    repository.deleteById(id);

    Optional<PagingSlipTemplate> deleted = repository.findById(id);
    assertTrue(deleted.isEmpty());
  }

  @Test
  @Sql(scripts = {"classpath:db/central-server/pre-populate-central-server.sql"})
  void throwExceptionWhenSavingWithoutTemplate() {
    var template = createPagingSlipTemplate();
    template.setTemplate(null);

    assertThrows(DataIntegrityViolationException.class, () -> repository.saveAndFlush(template));
  }

  @Test
  @Sql(scripts = {"classpath:db/central-server/pre-populate-central-server.sql"})
  void throwExceptionWhenSavingWithInvalidCentralServerReference() {
    var template = createPagingSlipTemplate();

    template.setCentralServer(TestUtil.refCentralServer(randomUUID()));

    var ex = assertThrows(DataIntegrityViolationException.class, () -> repository.saveAndFlush(template));
    assertThat(ex.getMessage(), containsString("constraint [fk_paging_slip_template_central_server]"));
  }

  @Test
  @Sql(scripts = {"classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/paging-slip-template/pre-populate-paging-slip-template.sql"})
  void throwExceptionWhenSavingTemplateForServerThatAlreadyHasOne() {
    var newTemplate = createPagingSlipTemplate();
    newTemplate.setCentralServer(refCentralServer());

    var ex = assertThrows(DataIntegrityViolationException.class, () -> repository.saveAndFlush(newTemplate));
    assertThat(ex.getMessage(), containsString("constraint [paging_slip_template_central_server_id_key]"));
  }
}
