package org.folio.innreach.controller;

import org.folio.innreach.controller.base.BaseControllerTest;
import org.folio.innreach.domain.entity.PatronTypeMapping;
import org.folio.innreach.dto.Error;
import org.folio.innreach.dto.PatronTypeMappingDTO;
import org.folio.innreach.dto.PatronTypeMappingsDTO;
import org.folio.innreach.dto.ValidationErrorsDTO;
import org.folio.innreach.mapper.PatronTypeMappingMapper;
import org.folio.innreach.repository.PatronTypeMappingRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlMergeMode;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static java.util.UUID.fromString;
import static org.folio.innreach.fixture.TestUtil.deserializeFromJsonFile;
import static org.folio.innreach.fixture.TestUtil.randomInteger;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.samePropertyValuesAs;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.AFTER_TEST_METHOD;
import static org.springframework.test.context.jdbc.SqlMergeMode.MergeMode.MERGE;

@Sql(
  scripts = {
    "classpath:db/patron-type-mapping/clear-patron-type-mapping-tables.sql",
    "classpath:db/central-server/clear-central-server-tables.sql"},
  executionPhase = AFTER_TEST_METHOD
)
@SqlMergeMode(MERGE)
class PatronTypeMappingControllerTest extends BaseControllerTest {
  private static final String PRE_POPULATED_CENTRAL_SERVER_ID = "edab6baf-c696-42b1-89bb-1bbb8759b0d2";
  private static final String PRE_POPULATED_PATRON_TYPE_MAPPING_ID1 = "5c39c67f-1373-4ec9-b356-fb71aba3e659";
  private static final String PRE_POPULATED_PATRON_TYPE_MAPPING_ID2 = "1af0b16e-24bc-44cb-9c9a-ca07167e41d4";
  private static final String PRE_POPULATED_PATRON_GROUP_ID1 = "54e17c4c-e315-4d20-8879-efc694dea1ce";

  @Autowired
  private TestRestTemplate testRestTemplate;
  @Autowired
  private PatronTypeMappingRepository repository;
  @Autowired
  private PatronTypeMappingMapper mapper;

  @Test
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/patron-type-mapping/pre-populate-patron-type-mapping.sql"
  })
  void shouldGetAllExistingMappings() {
    var responseEntity = testRestTemplate.getForEntity(
      "/inn-reach/central-servers/{centralServerId}/patron-type-mappings", PatronTypeMappingsDTO.class,
      PRE_POPULATED_CENTRAL_SERVER_ID);

    assertTrue(responseEntity.getStatusCode().is2xxSuccessful());
    assertTrue(responseEntity.hasBody());

    var response = responseEntity.getBody();
    assertNotNull(response);

    var mappings = response.getPatronTypeMappings();

    List<PatronTypeMapping> dbMappings = repository.findAll();

    assertEquals(dbMappings.size(), response.getTotalRecords());
    assertThat(mappings, containsInAnyOrder(mapper.toDTOs(dbMappings).toArray()));
  }

  @Test
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/patron-type-mapping/pre-populate-patron-type-mapping.sql"
  })
  void shouldGetSingleMappingById() {
    var responseEntity = testRestTemplate.getForEntity(
      "/inn-reach/central-servers/{centralServerId}/patron-type-mappings/{id}",
      PatronTypeMappingDTO.class, PRE_POPULATED_CENTRAL_SERVER_ID, PRE_POPULATED_PATRON_TYPE_MAPPING_ID1);

    assertTrue(responseEntity.getStatusCode().is2xxSuccessful());
    assertTrue(responseEntity.hasBody());

    var mapping = responseEntity.getBody();
    assertNotNull(mapping);

    var expected = findMapping(PRE_POPULATED_PATRON_TYPE_MAPPING_ID1);

    assertEquals(expected, mapping);
  }

  @Test
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql"
  })
  void shouldCreateNewMapping() {
    var newMapping = deserializeFromJsonFile("/patron-type-mapping/create-patron-type-mapping-request.json",
      PatronTypeMappingDTO.class);

    var responseEntity = testRestTemplate.postForEntity(
      "/inn-reach/central-servers/{centralServerId}/patron-type-mappings", newMapping, PatronTypeMappingDTO.class,
      PRE_POPULATED_CENTRAL_SERVER_ID);

    assertTrue(responseEntity.getStatusCode().is2xxSuccessful());
    assertTrue(responseEntity.hasBody());

    var created = responseEntity.getBody();

    assertThat(created, samePropertyValuesAs(newMapping, "id", "metadata"));
  }

  @Test
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/patron-type-mapping/pre-populate-patron-type-mapping.sql"
  })
  void shouldUpdateExistingMapping() {
    var mapping = deserializeFromJsonFile("/patron-type-mapping/update-patron-type-mapping-request.json",
      PatronTypeMappingDTO.class);

    var responseEntity = testRestTemplate.exchange(
      "/inn-reach/central-servers/{centralServerId}/patron-type-mappings/{id}", HttpMethod.PUT,
      new HttpEntity<>(mapping), PatronTypeMappingDTO.class, PRE_POPULATED_CENTRAL_SERVER_ID,
      PRE_POPULATED_PATRON_TYPE_MAPPING_ID1);

    assertEquals(HttpStatus.NO_CONTENT, responseEntity.getStatusCode());

    var expected = findMapping(PRE_POPULATED_PATRON_TYPE_MAPPING_ID1);

    assertThat(mapping, samePropertyValuesAs(expected, "id", "metadata"));
  }

  @Test
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/patron-type-mapping/pre-populate-patron-type-mapping.sql"
  })
  void shouldUpdateExistingMappings() {
    var existing = mapper.toDTOCollection(repository.findAll());
    existing.getPatronTypeMappings().forEach(m -> m.setPatronType(randomInteger(256)));

    var responseEntity = testRestTemplate.exchange(
      "/inn-reach/central-servers/{centralServerId}/patron-type-mappings", HttpMethod.PUT,
      new HttpEntity<>(existing), Void.class, PRE_POPULATED_CENTRAL_SERVER_ID);

    assertEquals(HttpStatus.NO_CONTENT, responseEntity.getStatusCode());
    assertFalse(responseEntity.hasBody());

    var updated = mapper.toDTOs(repository.findAll());
    var expected = existing.getPatronTypeMappings();

    assertEquals(expected.size(), updated.size());
    assertEquals(expected.stream().map(PatronTypeMappingDTO::getPatronType).collect(Collectors.toList()),
      updated.stream().map(PatronTypeMappingDTO::getPatronType).collect(Collectors.toList()));
  }

  @Test
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/patron-type-mapping/pre-populate-patron-type-mapping.sql"
  })
  void shouldCreateUpdateAndDeleteMappingsAtTheSameTime() {
    var mappings = mapper.toDTOCollection(repository.findAll());
    List<PatronTypeMappingDTO> em = mappings.getPatronTypeMappings();

    em.removeIf(idEqualsTo(fromString(PRE_POPULATED_PATRON_TYPE_MAPPING_ID1)));         // to delete
    Integer patronType = randomInteger(256);
    findInList(em, fromString(PRE_POPULATED_PATRON_TYPE_MAPPING_ID2))   // to update
      .ifPresent(mapping -> mapping.setPatronType(patronType));

    var newMappings = deserializeFromJsonFile("/patron-type-mapping/create-patron-type-mappings-request.json",
      PatronTypeMappingsDTO.class);
    em.addAll(newMappings.getPatronTypeMappings());       // to insert

    var responseEntity = testRestTemplate.exchange(
      "/inn-reach/central-servers/{centralServerId}/patron-type-mappings", HttpMethod.PUT, new HttpEntity<>(mappings),
      Void.class, PRE_POPULATED_CENTRAL_SERVER_ID);

    assertEquals(HttpStatus.NO_CONTENT, responseEntity.getStatusCode());
    assertFalse(responseEntity.hasBody());

    var stored = mapper.toDTOs(repository.findAll());

    assertEquals(em.size(), stored.size());
    // verify deleted
    assertTrue(findInList(stored, fromString(PRE_POPULATED_PATRON_TYPE_MAPPING_ID1)).isEmpty());
    // verify updated
    assertEquals(patronType,
      findInList(stored, fromString(PRE_POPULATED_PATRON_TYPE_MAPPING_ID2))
        .map(PatronTypeMappingDTO::getPatronType).get());
    // verify inserted
    assertThat(stored, hasItems(
      samePropertyValuesAs(newMappings.getPatronTypeMappings().get(0), "id", "metadata"),
      samePropertyValuesAs(newMappings.getPatronTypeMappings().get(1), "id", "metadata")
    ));
  }

  @Test
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/patron-type-mapping/pre-populate-patron-type-mapping.sql"
  })
  void shouldDeleteExistingMapping() {
    var responseEntity = testRestTemplate.exchange(
      "/inn-reach/central-servers/{centralServerId}/patron-type-mappings/{id}", HttpMethod.DELETE,
      HttpEntity.EMPTY, PatronTypeMappingDTO.class, PRE_POPULATED_CENTRAL_SERVER_ID,
      PRE_POPULATED_PATRON_TYPE_MAPPING_ID1);

    assertEquals(HttpStatus.NO_CONTENT, responseEntity.getStatusCode());

    var deleted = repository.findById(fromString(PRE_POPULATED_PATRON_TYPE_MAPPING_ID1));
    assertTrue(deleted.isEmpty());
  }

  @Test
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql"
  })
  void return404WhenMappingIsNotFoundById() {
    var responseEntity = testRestTemplate.getForEntity(
      "/inn-reach/central-servers/{centralServerId}/patron-type-mappings/{id}",
      PatronTypeMappingDTO.class, PRE_POPULATED_CENTRAL_SERVER_ID, PRE_POPULATED_PATRON_TYPE_MAPPING_ID1);

    assertEquals(HttpStatus.NOT_FOUND, responseEntity.getStatusCode());
  }

  @Test
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql"
  })
  void return400WhenCreatingNewMappingAndPatronTypeIsNull() {
    var newMapping = deserializeFromJsonFile("/patron-type-mapping/create-patron-type-mapping-request.json",
      PatronTypeMappingDTO.class);
    newMapping.setPatronType(null);

    var responseEntity = testRestTemplate.postForEntity(
      "/inn-reach/central-servers/{centralServerId}/patron-type-mappings", newMapping,
      ValidationErrorsDTO.class, PRE_POPULATED_CENTRAL_SERVER_ID);

    assertEquals(BAD_REQUEST, responseEntity.getStatusCode());
  }

  @Test
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/patron-type-mapping/pre-populate-patron-type-mapping.sql"
  })
  void return409WhenCreatingNewMappingAndPatronTypeIdAlreadyMapped() {
    var newMapping = deserializeFromJsonFile("/patron-type-mapping/create-patron-type-mapping-request.json",
      PatronTypeMappingDTO.class);
    newMapping.setPatronGroupId(fromString(PRE_POPULATED_PATRON_GROUP_ID1));

    var responseEntity = testRestTemplate.postForEntity(
      "/inn-reach/central-servers/{centralServerId}/patron-type-mappings", newMapping, Error.class,
      PRE_POPULATED_CENTRAL_SERVER_ID);

    assertEquals(CONFLICT, responseEntity.getStatusCode());
  }

  @Test
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql"
  })
  void return409WhenCreatingNewMappingWithInvalidPatronType() {
    var newMapping = deserializeFromJsonFile("/patron-type-mapping/create-patron-type-mapping-invalid-request.json",
      PatronTypeMappingDTO.class);

    var responseEntity = testRestTemplate.postForEntity(
      "/inn-reach/central-servers/{centralServerId}/patron-type-mappings", newMapping, Error.class,
      PRE_POPULATED_CENTRAL_SERVER_ID);

    assertEquals(CONFLICT, responseEntity.getStatusCode());
  }

  @Test
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql"
  })
  void return404IfMappingNotFoundWhenUpdating() {
    var mapping = deserializeFromJsonFile("/patron-type-mapping/update-patron-type-mapping-request.json",
      PatronTypeMappingDTO.class);

    var responseEntity = testRestTemplate.exchange(
      "/inn-reach/central-servers/{centralServerId}/patron-type-mappings/{id}", HttpMethod.PUT,
      new HttpEntity<>(mapping), PatronTypeMappingDTO.class, PRE_POPULATED_CENTRAL_SERVER_ID, UUID.randomUUID());

    assertEquals(HttpStatus.NOT_FOUND, responseEntity.getStatusCode());
  }

  @Test
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql",
  })
  void return404IfMappingNotFoundWhenDeleting() {
    var responseEntity = testRestTemplate.exchange(
      "/inn-reach/central-servers/{centralServerId}/patron-type-mappings/{id}", HttpMethod.DELETE,
      HttpEntity.EMPTY, PatronTypeMappingDTO.class, PRE_POPULATED_CENTRAL_SERVER_ID, UUID.randomUUID());

    assertEquals(HttpStatus.NOT_FOUND, responseEntity.getStatusCode());
  }

  private PatronTypeMappingDTO findMapping(String id) {
    var expectedEntity = repository.findById(fromString(id)).get();

    return mapper.toDTO(expectedEntity);
  }

  private static Predicate<PatronTypeMappingDTO> idEqualsTo(UUID id) {
    return mapping -> Objects.equals(mapping.getId(), id);
  }

  private static Optional<PatronTypeMappingDTO> findInList(List<PatronTypeMappingDTO> mappings, UUID id) {
    return mappings.stream().filter(idEqualsTo(id)).findFirst();
  }
}
