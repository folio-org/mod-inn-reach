package org.folio.innreach.controller;

import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.samePropertyValuesAs;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.AFTER_TEST_METHOD;
import static org.springframework.test.context.jdbc.SqlMergeMode.MergeMode.MERGE;

import static org.folio.innreach.controller.ControllerTestUtils.collectFieldNames;
import static org.folio.innreach.controller.ControllerTestUtils.createValidationError;
import static org.folio.innreach.fixture.TestUtil.deserializeFromJsonFile;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlMergeMode;

import org.folio.innreach.controller.base.BaseControllerTest;
import org.folio.innreach.domain.entity.LibraryMapping;
import org.folio.innreach.dto.Error;
import org.folio.innreach.dto.LibraryMappingDTO;
import org.folio.innreach.dto.LibraryMappingsDTO;
import org.folio.innreach.dto.ValidationErrorsDTO;
import org.folio.innreach.mapper.LibraryMappingMapper;
import org.folio.innreach.repository.LibraryMappingRepository;

@Sql(
    scripts = {
        "classpath:db/lib-mapping/clear-library-mapping-table.sql",
        "classpath:db/inn-reach-location/clear-inn-reach-location-tables.sql",
        "classpath:db/central-server/clear-central-server-tables.sql"},
    executionPhase = AFTER_TEST_METHOD
)
@SqlMergeMode(MERGE)
class LibraryMappingControllerTest extends BaseControllerTest {

  private static final String PRE_POPULATED_CENTRAL_SERVER_ID = "edab6baf-c696-42b1-89bb-1bbb8759b0d2";
  private static final String PRE_POPULATED_MAPPING2_ID = "07f97157-9cf9-44f2-b7aa-82e1f649cc83";
  private static final String PRE_POPULATED_LIBRARY2_ID = "5cc5fe97-6bce-4bbb-9a97-c0aff0851748";
  private static final String PRE_POPULATED_INN_REACH_lOCATION1_ID = "a1c1472f-67ec-4938-b5a8-f119e51ab79b";


  @Autowired
  private TestRestTemplate testRestTemplate;
  @Autowired
  private LibraryMappingRepository repository;
  @Autowired
  private LibraryMappingMapper mapper;


  @Test
  @Sql(scripts = {
      "classpath:db/central-server/pre-populate-central-server.sql",
      "classpath:db/inn-reach-location/pre-populate-inn-reach-location-code.sql",
      "classpath:db/lib-mapping/pre-populate-library-mapping.sql"
  })
  void shouldGetAllExistingMappings() {
    var responseEntity = testRestTemplate.getForEntity(baseMappingURL(), LibraryMappingsDTO.class);

    assertTrue(responseEntity.getStatusCode().is2xxSuccessful());
    assertTrue(responseEntity.hasBody());

    var response = responseEntity.getBody();
    assertNotNull(response);

    var mappings = response.getLibraryMappings();

    List<LibraryMapping> dbMappings = repository.findAll();

    assertEquals(dbMappings.size(), response.getTotalRecords());
    assertThat(mappings, containsInAnyOrder(mapper.toDTOs(dbMappings).toArray()));
  }

  @Test
  @Sql(scripts = {
      "classpath:db/central-server/pre-populate-central-server.sql"
  })
  void shouldGetEmptyMappingsWith0TotalIfNotSet() {
    var responseEntity = testRestTemplate.getForEntity(baseMappingURL(), LibraryMappingsDTO.class);

    assertTrue(responseEntity.getStatusCode().is2xxSuccessful());
    assertTrue(responseEntity.hasBody());

    var response = responseEntity.getBody();
    assertNotNull(response);

    var mappings = response.getLibraryMappings();

    assertEquals(0, response.getTotalRecords());
    assertThat(mappings, is(empty()));
  }

  @Test
  @Sql(scripts = {
      "classpath:db/central-server/pre-populate-central-server.sql",
      "classpath:db/inn-reach-location/pre-populate-inn-reach-location-code.sql",
      "classpath:db/lib-mapping/pre-populate-library-mapping.sql"
  })
  void shouldApplyLimitAndOffsetWhenGettingAllExistingMappings() {
    var responseEntity = testRestTemplate.getForEntity(baseMappingURL() + "?offset={offset}&limit={limit}",
        LibraryMappingsDTO.class, Map.of("offset", 1, "limit", 1));

    assertTrue(responseEntity.getStatusCode().is2xxSuccessful());
    assertTrue(responseEntity.hasBody());

    var response = responseEntity.getBody();
    assertNotNull(response);

    var expectedMapping = findMapping(PRE_POPULATED_MAPPING2_ID);

    assertEquals(3, response.getTotalRecords());
    assertEquals(singletonList(expectedMapping), response.getLibraryMappings());
  }

  @Test
  void return400WhenGetAllExistingMappingsIfLimitAndOffsetInvalid() {
    var responseEntity = testRestTemplate.getForEntity(baseMappingURL() + "?offset={offset}&limit={limit}",
        ValidationErrorsDTO.class, Map.of("offset", -1, "limit", -1));

    assertEquals(BAD_REQUEST, responseEntity.getStatusCode());

    var errors = responseEntity.getBody();
    assertNotNull(errors);
    assertEquals(BAD_REQUEST.value(), errors.getCode());
    assertThat(collectFieldNames(errors), containsInAnyOrder(containsString("offset"), containsString("limit")));
  }

  @Test
  @Sql(scripts = {
      "classpath:db/central-server/pre-populate-central-server.sql",
      "classpath:db/inn-reach-location/pre-populate-inn-reach-location-code.sql"
  })
  void shouldCreateNewMappings() {
    var newMappings = deserializeFromJsonFile("/library-mapping/create-library-mappings-request.json",
        LibraryMappingsDTO.class);

    var responseEntity = testRestTemplate.exchange(baseMappingURL(), HttpMethod.PUT, new HttpEntity<>(newMappings),
        Void.class);
    
    assertEquals(HttpStatus.NO_CONTENT, responseEntity.getStatusCode());
    assertFalse(responseEntity.hasBody());

    var created = mapper.toDTOs(repository.findAll());
    var expected = newMappings.getLibraryMappings();

    assertEquals(expected.size(), created.size());
    assertThat(created, containsInAnyOrder(
        samePropertyValuesAs(expected.get(0), "id", "metadata"),
        samePropertyValuesAs(expected.get(1), "id", "metadata")
    ));
  }

  @Test
  @Sql(scripts = {
      "classpath:db/central-server/pre-populate-central-server.sql",
      "classpath:db/inn-reach-location/pre-populate-inn-reach-location-code.sql"
  })
  void return400WhenCreatingNewMappingsAndLibraryIdIsNull() {
    var newMappings = deserializeFromJsonFile("/library-mapping/create-library-mappings-request.json",
        LibraryMappingsDTO.class);
    newMappings.getLibraryMappings().get(0).setLibraryId(null);

    var responseEntity = testRestTemplate.exchange(baseMappingURL(), HttpMethod.PUT, new HttpEntity<>(newMappings),
        ValidationErrorsDTO.class);

    assertEquals(BAD_REQUEST, responseEntity.getStatusCode());

    assertNotNull(responseEntity.getBody());
    assertThat(responseEntity.getBody().getValidationErrors(),
        contains(createValidationError("libraryMappings[0].libraryId", "must not be null")));
  }

  @Test
  @Sql(scripts = {
      "classpath:db/central-server/pre-populate-central-server.sql",
      "classpath:db/inn-reach-location/pre-populate-inn-reach-location-code.sql"
  })
  void return400WhenCreatingNewMappingsAndInnReachLocationIdIsNull() {
    var newMappings = deserializeFromJsonFile("/library-mapping/create-library-mappings-request.json",
        LibraryMappingsDTO.class);
    newMappings.getLibraryMappings().get(0).setInnReachLocationId(null);

    var responseEntity = testRestTemplate.exchange(baseMappingURL(), HttpMethod.PUT, new HttpEntity<>(newMappings),
        ValidationErrorsDTO.class);

    assertEquals(BAD_REQUEST, responseEntity.getStatusCode());

    assertNotNull(responseEntity.getBody());
    assertThat(responseEntity.getBody().getValidationErrors(),
        contains(createValidationError("libraryMappings[0].innReachLocationId", "must not be null")));
  }

  @Test
  @Sql(scripts = {
      "classpath:db/central-server/pre-populate-central-server.sql",
      "classpath:db/inn-reach-location/pre-populate-inn-reach-location-code.sql",
      "classpath:db/lib-mapping/pre-populate-library-mapping.sql"
  })
  void return400WhenCreatingNewMappingsAndLibraryIdAlreadyMapped() {
    var newMappings = deserializeFromJsonFile("/library-mapping/create-library-mappings-request.json",
        LibraryMappingsDTO.class);
    newMappings.getLibraryMappings().get(0).setLibraryId(UUID.fromString(PRE_POPULATED_LIBRARY2_ID));

    var existing = mapper.toDTOs(repository.findAll());
    newMappings.getLibraryMappings().addAll(existing);

    var responseEntity = testRestTemplate.exchange(baseMappingURL(), HttpMethod.PUT, new HttpEntity<>(newMappings),
        Error.class);

    assertEquals(BAD_REQUEST, responseEntity.getStatusCode());
    assertNotNull(responseEntity.getBody());
    assertThat(responseEntity.getBody().getMessage(), containsString("constraint [unq_library_mapping_server_lib]"));
  }

  @Test
  @Disabled("review update mechanism")
  @Sql(scripts = {
      "classpath:db/central-server/pre-populate-central-server.sql",
      "classpath:db/inn-reach-location/pre-populate-inn-reach-location-code.sql",
      "classpath:db/lib-mapping/pre-populate-library-mapping.sql"
  })
  void shouldUpdateExistingMappings() {
    var existing = mapper.toDTOCollection(repository.findAll());
    UUID innReachLocationId = UUID.fromString("a1c1472f-67ec-4938-b5a8-f119e51ab79b");
    existing.getLibraryMappings().forEach(mp -> mp.setInnReachLocationId(innReachLocationId));

    var responseEntity = testRestTemplate.exchange(baseMappingURL(), HttpMethod.PUT, new HttpEntity<>(existing),
        Void.class);

    assertEquals(HttpStatus.NO_CONTENT, responseEntity.getStatusCode());
    assertFalse(responseEntity.hasBody());

    var updated = mapper.toDTOs(repository.findAll());
    var expected = existing.getLibraryMappings();

    assertEquals(expected.size(), updated.size());
    assertTrue(updated.stream()
        .filter(mp -> !mp.getInnReachLocationId().equals(innReachLocationId))
        .findFirst()
        .isEmpty());
  }

  private static String baseMappingURL() {
    return baseMappingURL(PRE_POPULATED_CENTRAL_SERVER_ID);
  }

  private static String baseMappingURL(String serverId) {
    return "/inn-reach/central-servers/" + serverId + "/libraries/location-mappings";
  }

  private LibraryMappingDTO findMapping(String id) {
    var entity = repository.findById(UUID.fromString(id)).get();

    return mapper.toDTO(entity);
  }

}
