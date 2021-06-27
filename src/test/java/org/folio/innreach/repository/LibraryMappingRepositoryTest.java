package org.folio.innreach.repository;

import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import static org.folio.innreach.fixture.MappingFixture.createLibraryMapping;
import static org.folio.innreach.fixture.MappingFixture.refCentralServer;
import static org.folio.innreach.fixture.MappingFixture.refInnReachLocation;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.jpa.JpaSystemException;
import org.springframework.test.context.jdbc.Sql;

import org.folio.innreach.domain.entity.LibraryMapping;

@Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/inn-reach-location/pre-populate-inn-reach-location-code.sql",
    "classpath:db/lib-mapping/pre-populate-library-mapping.sql"
})
class LibraryMappingRepositoryTest extends BaseRepositoryTest {

  private static final String PRE_POPULATED_MAPPING1_ID = "2d57c159-8b05-4c3b-97d9-7ddf0ead17a3";
  private static final String PRE_POPULATED_MAPPING2_ID = "07f97157-9cf9-44f2-b7aa-82e1f649cc83";
  private static final String PRE_POPULATED_MAPPING3_ID = "b089157b-1c72-43e2-acd6-d6f2b312ba8c";

  private static final UUID PRE_POPULATED_LIBRARY1_UUID = fromString("ffbef66a-12f5-480e-9ea2-499a406bdf27");
  private static final UUID PRE_POPULATED_IR_LOCATION1_UUID = fromString("a1c1472f-67ec-4938-b5a8-f119e51ab79b");
  private static final UUID PRE_POPULATED_IR_LOCATION2_UUID = fromString("26f7c8c5-f090-4742-b7c7-e08ed1cc4e67");
  private static final UUID PRE_POPULATED_CENTRAL_SERVER_UUID = fromString("edab6baf-c696-42b1-89bb-1bbb8759b0d2");

  private static final String PRE_POPULATED_USER = "admin";


  @Autowired
  private LibraryMappingRepository repository;


  @Test
  void shouldFindAllExistingMappings() {
    var mappings = repository.findAll();

    assertEquals(3, mappings.size());

    List<String> ids = mappings.stream()
        .map(mapping -> mapping.getId().toString())
        .collect(toList());

    assertEquals(ids, List.of(PRE_POPULATED_MAPPING1_ID, PRE_POPULATED_MAPPING2_ID, PRE_POPULATED_MAPPING3_ID));
  }

  @Test
  void shouldGetExistingMapping() {
    var mapping = repository.getOne(fromString(PRE_POPULATED_MAPPING1_ID));

    assertEquals(PRE_POPULATED_LIBRARY1_UUID, mapping.getLibraryId());
    assertEquals(PRE_POPULATED_IR_LOCATION1_UUID, mapping.getInnReachLocation().getId());
    assertEquals(PRE_POPULATED_CENTRAL_SERVER_UUID, mapping.getCentralServer().getId());

    assertEquals(PRE_POPULATED_USER, mapping.getCreatedBy());
    assertNotNull(mapping.getCreatedDate());
    assertEquals(PRE_POPULATED_USER, mapping.getLastModifiedBy());
    assertNotNull(mapping.getLastModifiedDate());
  }

  @Test
  void shouldSaveNewMapping() {
    var newMapping = createLibraryMapping();

    var saved = repository.saveAndFlush(newMapping);

    LibraryMapping found = repository.getOne(saved.getId());
    assertEquals(newMapping.getId(), found.getId());
    assertEquals(saved.getLibraryId(), found.getLibraryId());
    assertEquals(saved.getCentralServer().getId(), found.getCentralServer().getId());
    assertEquals(saved.getInnReachLocation().getId(), found.getInnReachLocation().getId());
  }

  @Test
  void throwExceptionWhenSavingWithoutId() {
    var mapping = createLibraryMapping();
    mapping.setId(null);

    assertThrows(JpaSystemException.class, () -> repository.saveAndFlush(mapping));
  }

  @Test
  void shouldUpdateLibraryIdAndIRLocationId() {
    var mapping = repository.getOne(fromString(PRE_POPULATED_MAPPING1_ID));

    UUID newLibraryId = randomUUID();
    UUID newIRLocationId = PRE_POPULATED_IR_LOCATION2_UUID;
    mapping.setLibraryId(newLibraryId);
    mapping.setInnReachLocation(refInnReachLocation(newIRLocationId));

    repository.saveAndFlush(mapping);

    var saved = repository.getOne(mapping.getId());

    assertEquals(newLibraryId, saved.getLibraryId());
    assertEquals(newIRLocationId, saved.getInnReachLocation().getId());
  }

  @Test
  void shouldDeleteExistingMapping() {
    UUID id = fromString(PRE_POPULATED_MAPPING1_ID);

    repository.deleteById(id);

    Optional<LibraryMapping> deleted = repository.findById(id);
    assertTrue(deleted.isEmpty());
  }

  @Test
  void throwExceptionWhenSavingWithoutLibraryId() {
    var mapping = createLibraryMapping();
    mapping.setLibraryId(null);

    assertThrows(DataIntegrityViolationException.class, () -> repository.saveAndFlush(mapping));
  }

  @Test
  void throwExceptionWhenSavingWithInvalidCentralServerReference() {
    var mapping = createLibraryMapping();

    mapping.setCentralServer(refCentralServer(randomUUID()));

    var ex = assertThrows(DataIntegrityViolationException.class, () -> repository.saveAndFlush(mapping));
    assertThat(ex.getMessage(), containsString("constraint [fk_library_mapping_central_server]"));
  }

  @Test
  void throwExceptionWhenSavingWithInvalidInnReachLocationReference() {
    var mapping = createLibraryMapping();

    mapping.setInnReachLocation(refInnReachLocation(randomUUID()));

    var ex = assertThrows(DataIntegrityViolationException.class, () -> repository.saveAndFlush(mapping));
    assertThat(ex.getMessage(), containsString("constraint [fk_library_mapping_ir_location]"));
  }

  @Test
  void throwExceptionWhenNewLibraryMappingExistsForTheServer() {
    var existing = repository.getOne(fromString(PRE_POPULATED_MAPPING1_ID));

    var mapping = createLibraryMapping();
    mapping.setLibraryId(existing.getLibraryId());
    mapping.setCentralServer(existing.getCentralServer());

    var ex = assertThrows(DataIntegrityViolationException.class, () -> repository.saveAndFlush(mapping));
    assertThat(ex.getMessage(), containsString("constraint [unq_library_mapping_server_lib]"));
  }

}