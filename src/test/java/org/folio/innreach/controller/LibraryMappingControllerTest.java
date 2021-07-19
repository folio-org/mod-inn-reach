package org.folio.innreach.controller;

import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.samePropertyValuesAs;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.AFTER_TEST_METHOD;
import static org.springframework.test.context.jdbc.SqlMergeMode.MergeMode.MERGE;

import static org.folio.innreach.controller.ControllerTestUtils.collectFieldNames;
import static org.folio.innreach.controller.ControllerTestUtils.createValidationError;
import static org.folio.innreach.fixture.TestUtil.deserializeFromJsonFile;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlMergeMode;

import org.folio.innreach.controller.base.BaseControllerTest;
import org.folio.innreach.domain.entity.LibraryMapping;
import org.folio.innreach.domain.service.CentralServerService;
import org.folio.innreach.dto.Error;
import org.folio.innreach.dto.LibraryMappingDTO;
import org.folio.innreach.dto.LibraryMappingsDTO;
import org.folio.innreach.dto.ValidationErrorsDTO;
import org.folio.innreach.external.service.InnReachLocationExternalService;
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
  private static final UUID PRE_POPULATED_MAPPING1_ID = UUID.fromString("07f97157-9cf9-44f2-b7aa-82e1f649cc83");
  private static final UUID PRE_POPULATED_MAPPING2_ID = UUID.fromString("2d57c159-8b05-4c3b-97d9-7ddf0ead17a3");
  private static final UUID PRE_POPULATED_LIBRARY2_ID = UUID.fromString("ffbef66a-12f5-480e-9ea2-499a406bdf27");
  private static final UUID PRE_POPULATED_INN_REACH_LOCATION1_ID = UUID.fromString(
      "26f7c8c5-f090-4742-b7c7-e08ed1cc4e67");


  @Autowired
  private TestRestTemplate testRestTemplate;
  @Autowired
  private LibraryMappingRepository repository;
  @Autowired
  private LibraryMappingMapper mapper;

  @MockBean
  private CentralServerService centralServerService;

  @MockBean
  private InnReachLocationExternalService innReachLocationExternalService;

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
  void return409WhenCreatingNewMappingsAndLibraryIdAlreadyMapped() {
    var newMappings = deserializeFromJsonFile("/library-mapping/create-library-mappings-request.json",
        LibraryMappingsDTO.class);
    newMappings.getLibraryMappings().get(0).setLibraryId(PRE_POPULATED_LIBRARY2_ID);

    var existing = mapper.toDTOs(repository.findAll());
    newMappings.getLibraryMappings().addAll(existing);

    var responseEntity = testRestTemplate.exchange(baseMappingURL(), HttpMethod.PUT, new HttpEntity<>(newMappings),
        Error.class);

    assertEquals(CONFLICT, responseEntity.getStatusCode());
    assertNotNull(responseEntity.getBody());
    assertThat(responseEntity.getBody().getMessage(), containsString("constraint [unq_library_mapping_server_lib]"));
  }

  @Test
  @Sql(scripts = {
      "classpath:db/central-server/pre-populate-central-server.sql",
      "classpath:db/inn-reach-location/pre-populate-inn-reach-location-code.sql",
      "classpath:db/lib-mapping/pre-populate-library-mapping.sql"
  })
  void shouldUpdateExistingMappings() {
    var existing = mapper.toDTOCollection(repository.findAll());
    UUID innReachLocationId = PRE_POPULATED_INN_REACH_LOCATION1_ID;
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

  @Test
  @Sql(scripts = {
      "classpath:db/central-server/pre-populate-central-server.sql",
      "classpath:db/inn-reach-location/pre-populate-inn-reach-location-code.sql",
      "classpath:db/lib-mapping/pre-populate-library-mapping.sql"
  })
  void shouldCreateUpdateAndDeleteMappingsAtTheSameTime() {
    var mappings = mapper.toDTOCollection(repository.findAll());
    List<LibraryMappingDTO> em = mappings.getLibraryMappings();

    em.removeIf(idEqualsTo(PRE_POPULATED_MAPPING1_ID));         // to delete
    findInList(em, PRE_POPULATED_MAPPING2_ID)   // to update
        .ifPresent(mapping -> mapping.setInnReachLocationId(PRE_POPULATED_INN_REACH_LOCATION1_ID));

    var newMappings = deserializeFromJsonFile("/library-mapping/create-library-mappings-request.json",
        LibraryMappingsDTO.class);
    em.addAll(newMappings.getLibraryMappings());                // to insert

    var responseEntity = testRestTemplate.exchange(baseMappingURL(), HttpMethod.PUT, new HttpEntity<>(mappings),
        Void.class);

    assertEquals(HttpStatus.NO_CONTENT, responseEntity.getStatusCode());
    assertFalse(responseEntity.hasBody());

    var stored = mapper.toDTOs(repository.findAll());

    assertEquals(em.size(), stored.size());
    // verify deleted
    assertTrue(findInList(stored, PRE_POPULATED_MAPPING1_ID).isEmpty());
    // verify updated
    assertEquals(PRE_POPULATED_INN_REACH_LOCATION1_ID,
        findInList(stored, PRE_POPULATED_MAPPING2_ID)
            .map(LibraryMappingDTO::getInnReachLocationId).get());
    // verify inserted
    assertThat(stored, hasItems(
        samePropertyValuesAs(newMappings.getLibraryMappings().get(0), "id", "metadata"),
        samePropertyValuesAs(newMappings.getLibraryMappings().get(1), "id", "metadata")
    ));
  }

  @Test
  @Sql(scripts = {
      "classpath:db/central-server/pre-populate-central-server.sql",
      "classpath:db/inn-reach-location/pre-populate-inn-reach-location-code.sql",
      "classpath:db/lib-mapping/pre-populate-library-mapping.sql"
  })
  void shouldDeleteAllMappingsIfEmptyCollectionGiven() {
    var mappings = new LibraryMappingsDTO();

    var responseEntity = testRestTemplate.exchange(baseMappingURL(), HttpMethod.PUT, new HttpEntity<>(mappings),
        Void.class);

    assertEquals(HttpStatus.NO_CONTENT, responseEntity.getStatusCode());
    assertFalse(responseEntity.hasBody());

    assertEquals(0, repository.count());
  }

  private static Predicate<LibraryMappingDTO> idEqualsTo(UUID id) {
    return mapping -> Objects.equals(mapping.getId(), id);
  }

  private static String baseMappingURL() {
    return baseMappingURL(PRE_POPULATED_CENTRAL_SERVER_ID);
  }

  private static String baseMappingURL(String serverId) {
    return "/inn-reach/central-servers/" + serverId + "/libraries/location-mappings";
  }

  private LibraryMappingDTO findMapping(UUID id) {
    var entity = repository.findById(id).get();

    return mapper.toDTO(entity);
  }

  private static Optional<LibraryMappingDTO> findInList(List<LibraryMappingDTO> mappings, UUID id) {
    return mappings.stream().filter(idEqualsTo(id)).findFirst();
  }

}
