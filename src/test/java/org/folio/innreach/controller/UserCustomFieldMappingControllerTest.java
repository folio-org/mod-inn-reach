package org.folio.innreach.controller;

import org.folio.innreach.controller.base.BaseControllerTest;
import org.folio.innreach.domain.entity.UserCustomFieldMapping;
import org.folio.innreach.dto.Error;
import org.folio.innreach.dto.UserCustomFieldMappingDTO;
import org.folio.innreach.dto.UserCustomFieldMappingsDTO;
import org.folio.innreach.mapper.UserCustomFieldMappingMapper;
import org.folio.innreach.repository.UserCustomFieldMappingRepository;
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
import static org.folio.innreach.fixture.TestUtil.randomFiveCharacterCode;
import static org.hamcrest.MatcherAssert.assertThat;
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

@Sql(
  scripts = {
    "classpath:db/user-custom-field-mapping/clear-user-custom-field-mapping.sql",
    "classpath:db/central-server/clear-central-server-tables.sql"},
  executionPhase = AFTER_TEST_METHOD
)
@SqlMergeMode(MERGE)
class UserCustomFieldMappingControllerTest extends BaseControllerTest {

  private static final String PRE_POPULATED_CENTRAL_SERVER_ID = "edab6baf-c696-42b1-89bb-1bbb8759b0d2";
  private static final String PRE_POPULATED_CUSTOM_FIELD_ID = "43a175e3-d876-4235-8a51-56de9fce3247";
  private static final String PRE_POPULATED_CUSTOM_FIELD_VALUE = "qwerty";

  private static final String PRE_POPULATED_MAPPING_ID1 = "555392b2-9b33-4199-b5eb-73e842c9d5b0";
  private static final String PRE_POPULATED_MAPPING_ID2 = "b23ee9d7-7857-492b-bc89-dd9f37315555";

  @Autowired
  private TestRestTemplate testRestTemplate;
  @Autowired
  private UserCustomFieldMappingRepository repository;
  @Autowired
  private UserCustomFieldMappingMapper mapper;

  @Test
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/central-server/pre-populate-another-central-server.sql",
    "classpath:db/user-custom-field-mapping/pre-populate-user-custom-field-mapping.sql",
    "classpath:db/user-custom-field-mapping/pre-populate-another-user-custom-field-mapping.sql"
  })
  void shouldGetAllExistingMappingsForOneCustomField() {
    var responseEntity = testRestTemplate.getForEntity(
      "/inn-reach/central-servers/{centralServerId}/user-custom-field-mappings/{customFieldId}",
      UserCustomFieldMappingsDTO.class, PRE_POPULATED_CENTRAL_SERVER_ID, PRE_POPULATED_CUSTOM_FIELD_ID);

    assertTrue(responseEntity.getStatusCode().is2xxSuccessful());
    assertTrue(responseEntity.hasBody());

    var response = responseEntity.getBody();
    assertNotNull(response);

    var mappings = response.getUserCustomFieldMappings();

    List<UserCustomFieldMapping> dbMappings = repository.findByCentralServerIdAndCustomFieldId(
      UUID.fromString(PRE_POPULATED_CENTRAL_SERVER_ID), UUID.fromString(PRE_POPULATED_CUSTOM_FIELD_ID));

    assertEquals(dbMappings.size(), response.getTotalRecords());
    assertThat(mappings, containsInAnyOrder(mapper.toDTOs(dbMappings).toArray()));
  }

  @Test
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql"
  })
  void shouldGetEmptyMappingsWith0TotalIfNotSet() {
    var responseEntity = testRestTemplate.getForEntity(
      "/inn-reach/central-servers/{centralServerId}/user-custom-field-mappings/{customFieldId}",
      UserCustomFieldMappingsDTO.class, PRE_POPULATED_CENTRAL_SERVER_ID, PRE_POPULATED_CUSTOM_FIELD_ID);

    assertTrue(responseEntity.getStatusCode().is2xxSuccessful());
    assertTrue(responseEntity.hasBody());

    var response = responseEntity.getBody();
    assertNotNull(response);

    var mappings = response.getUserCustomFieldMappings();

    assertEquals(0, response.getTotalRecords());
    assertThat(mappings, is(empty()));
  }

  @Test
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/user-custom-field-mapping/pre-populate-user-custom-field-mapping.sql"
  })
  void shouldUpdateAllExistingMappingsForOneCustomField() {
    var existing = mapper.toDTOCollection(repository.findByCentralServerIdAndCustomFieldId(
      UUID.fromString(PRE_POPULATED_CENTRAL_SERVER_ID), UUID.fromString(PRE_POPULATED_CUSTOM_FIELD_ID)
    ));
    existing.getUserCustomFieldMappings().forEach(m -> m.setAgencyCode(randomFiveCharacterCode()));

    var responseEntity = testRestTemplate.exchange(
      "/inn-reach/central-servers/{centralServerId}/user-custom-field-mappings/{customFieldId}",
      HttpMethod.PUT, new HttpEntity<>(existing), Void.class, PRE_POPULATED_CENTRAL_SERVER_ID,
      PRE_POPULATED_CUSTOM_FIELD_ID);

    assertEquals(HttpStatus.NO_CONTENT, responseEntity.getStatusCode());
    assertFalse(responseEntity.hasBody());

    var updated = mapper.toDTOs(repository.findByCentralServerIdAndCustomFieldId(
      UUID.fromString(PRE_POPULATED_CENTRAL_SERVER_ID), UUID.fromString(PRE_POPULATED_CUSTOM_FIELD_ID)));
    var expected = existing.getUserCustomFieldMappings();

    assertEquals(expected.size(), updated.size());
    assertThat(expected.stream().map(UserCustomFieldMappingDTO::getAgencyCode).collect(Collectors.toList()),
      containsInAnyOrder(updated.stream().map(UserCustomFieldMappingDTO::getAgencyCode).toArray()));
  }

  @Test
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/user-custom-field-mapping/pre-populate-user-custom-field-mapping.sql"
  })
  void shouldCreateUpdateAndDeleteMappingsAtTheSameTime() {
    var mappings = mapper.toDTOCollection(repository.findByCentralServerIdAndCustomFieldId(
      UUID.fromString(PRE_POPULATED_CENTRAL_SERVER_ID), UUID.fromString(PRE_POPULATED_CUSTOM_FIELD_ID)
    ));
    List<UserCustomFieldMappingDTO> em = mappings.getUserCustomFieldMappings();

    em.removeIf(idEqualsTo(fromString(PRE_POPULATED_MAPPING_ID1)));         // to delete
    var agencyCode = randomFiveCharacterCode();
    findInList(em, fromString(PRE_POPULATED_MAPPING_ID2))   // to update
      .ifPresent(mapping -> mapping.setAgencyCode(agencyCode));

    var newMappings = deserializeFromJsonFile("/user-custom-field-mapping/create-user-custom-field-mappings-request.json",
      UserCustomFieldMappingsDTO.class);
    em.addAll(newMappings.getUserCustomFieldMappings());       // to insert

    var responseEntity = testRestTemplate.exchange(
      "/inn-reach/central-servers/{centralServerId}/user-custom-field-mappings/{customFieldId}", HttpMethod.PUT, new HttpEntity<>(mappings),
      Void.class, PRE_POPULATED_CENTRAL_SERVER_ID, PRE_POPULATED_CUSTOM_FIELD_ID);

    assertEquals(HttpStatus.NO_CONTENT, responseEntity.getStatusCode());
    assertFalse(responseEntity.hasBody());

    var stored = mapper.toDTOs(repository.findByCentralServerIdAndCustomFieldId(
      UUID.fromString(PRE_POPULATED_CENTRAL_SERVER_ID), UUID.fromString(PRE_POPULATED_CUSTOM_FIELD_ID)));

    assertEquals(em.size(), stored.size());
    // verify deleted
    assertTrue(findInList(stored, fromString(PRE_POPULATED_MAPPING_ID1)).isEmpty());
    // verify updated
    assertEquals(agencyCode,
      findInList(stored, fromString(PRE_POPULATED_MAPPING_ID2))
        .map(UserCustomFieldMappingDTO::getAgencyCode).get());
    // verify inserted
    assertThat(stored, hasItems(
      samePropertyValuesAs(newMappings.getUserCustomFieldMappings().get(0), "id", "metadata"),
      samePropertyValuesAs(newMappings.getUserCustomFieldMappings().get(1), "id", "metadata")
    ));
  }

  @Test
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/user-custom-field-mapping/pre-populate-user-custom-field-mapping.sql"
  })
  void return409WhenUpdatingMappingAndCustomFieldValueAlreadyMapped() {
    var existing = mapper.toDTOCollection(repository.findByCentralServerIdAndCustomFieldId(
      UUID.fromString(PRE_POPULATED_CENTRAL_SERVER_ID), UUID.fromString(PRE_POPULATED_CUSTOM_FIELD_ID)));
    existing.getUserCustomFieldMappings().forEach(m -> m.setCustomFieldValue((PRE_POPULATED_CUSTOM_FIELD_VALUE)));

    var responseEntity = testRestTemplate.exchange(
      "/inn-reach/central-servers/{centralServerId}/user-custom-field-mappings/{customFieldId}", HttpMethod.PUT, new HttpEntity<>(existing),
      Error.class, PRE_POPULATED_CENTRAL_SERVER_ID, PRE_POPULATED_CUSTOM_FIELD_ID);

    assertEquals(CONFLICT, responseEntity.getStatusCode());
    assertNotNull(responseEntity.getBody());
    assertThat(responseEntity.getBody().getMessage(), containsString("constraint [unq_custom_field_central_server]"));
  }

  @Test
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/user-custom-field-mapping/pre-populate-user-custom-field-mapping.sql"
  })
  void return409WhenUpdatingMappingWithInvalidAgencyCode() {
    var newMapping = deserializeFromJsonFile("/user-custom-field-mapping/update-user-custom-field-mappings-invalid-request.json",
      UserCustomFieldMappingsDTO.class);

    var responseEntity = testRestTemplate.exchange(
      "/inn-reach/central-servers/{centralServerId}/user-custom-field-mappings/{customFieldId}", HttpMethod.PUT, new HttpEntity<>(newMapping),
      Error.class, PRE_POPULATED_CENTRAL_SERVER_ID, PRE_POPULATED_CUSTOM_FIELD_ID);

    assertEquals(BAD_REQUEST, responseEntity.getStatusCode());
  }

  private static Predicate<UserCustomFieldMappingDTO> idEqualsTo(UUID id) {
    return mapping -> Objects.equals(mapping.getId(), id);
  }

  private static Optional<UserCustomFieldMappingDTO> findInList(List<UserCustomFieldMappingDTO> mappings, UUID id) {
    return mappings.stream().filter(idEqualsTo(id)).findFirst();
  }
}
