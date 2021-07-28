package org.folio.innreach.controller;

import org.assertj.core.api.Assertions;
import org.folio.innreach.controller.base.BaseControllerTest;
import org.folio.innreach.domain.entity.AgencyLocationMapping;
import org.folio.innreach.dto.AgencyLocationAcMappingDTO;
import org.folio.innreach.dto.AgencyLocationLscMappingDTO;
import org.folio.innreach.dto.AgencyLocationMappingDTO;
import org.folio.innreach.dto.ValidationErrorsDTO;
import org.folio.innreach.mapper.AgencyLocationMappingMapper;
import org.folio.innreach.repository.AgencyLocationMappingRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlMergeMode;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static org.folio.innreach.controller.ControllerTestUtils.createValidationError;
import static org.folio.innreach.fixture.TestUtil.deserializeFromJsonFile;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.samePropertyValuesAs;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.AFTER_TEST_METHOD;
import static org.springframework.test.context.jdbc.SqlMergeMode.MergeMode.MERGE;

@Sql(
  scripts = {
    "classpath:db/agency-loc-mapping/clear-agency-location-mapping.sql",
    "classpath:db/central-server/clear-central-server-tables.sql"},
  executionPhase = AFTER_TEST_METHOD
)
@SqlMergeMode(MERGE)
class AgencyMappingControllerTest extends BaseControllerTest {

  private static final UUID PRE_POPULATED_CENTRAL_SERVER_ID = UUID.fromString("edab6baf-c696-42b1-89bb-1bbb8759b0d2");
  private static final UUID PRE_POPULATED_LOCATION2_ID = UUID.fromString("2eda63ce-6b5d-45a6-8481-f83bc77c2a14");
  private static final String PRE_POPULATED_AGENCY_CODE = "5east";
  private static final String PRE_POPULATED_AGENCY2_CODE = "5main";

  @Autowired
  private TestRestTemplate testRestTemplate;
  @Autowired
  private AgencyLocationMappingRepository repository;
  @Autowired
  private AgencyLocationMappingMapper mapper;

  @Test
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/agency-loc-mapping/pre-populate-agency-location-mapping.sql"
  })
  void shouldGetExistingMappingForCentralServer() {
    var responseEntity = testRestTemplate.getForEntity(baseMappingURL(), AgencyLocationMappingDTO.class);

    assertTrue(responseEntity.getStatusCode().is2xxSuccessful());
    assertTrue(responseEntity.hasBody());

    var response = responseEntity.getBody();
    assertNotNull(response);

    var existing = fetchDbEntity();

    assertNotNull(existing);

    var existingLsMappings = mapper.toDTOs(existing.getLocalServerMappings()).toArray();
    assertThat(response.getLocalServers(), containsInAnyOrder(existingLsMappings));
  }

  @Test
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql"
  })
  void shouldReturn404IfNoMappingFound() {
    var responseEntity = testRestTemplate.getForEntity(baseMappingURL(), AgencyLocationMappingDTO.class);

    assertEquals(NOT_FOUND, responseEntity.getStatusCode());
  }

  @Test
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql"
  })
  void shouldCreateNewMappings() {
    var newMapping = deserializeMapping();
    var newLsMappings = newMapping.getLocalServers();
    var newAcMappings = getAllAcMappings(newLsMappings);

    var responseEntity = testRestTemplate
      .exchange(baseMappingURL(), HttpMethod.PUT, new HttpEntity<>(newMapping), Void.class);

    assertEquals(HttpStatus.NO_CONTENT, responseEntity.getStatusCode());
    assertFalse(responseEntity.hasBody());

    var createdEntity = fetchDbEntity();

    assertNotNull(createdEntity);

    var created = mapper.toDTO(createdEntity);

    assertThat(created, samePropertyValuesAs(newMapping, "id", "metadata", "localServers"));

    Assertions.assertThat(created.getLocalServers())
      .hasSize(newLsMappings.size())
      .usingElementComparatorOnFields("localCode", "locationId", "libraryId")
      .containsExactlyInAnyOrderElementsOf(newLsMappings);

    Assertions.assertThat(created.getLocalServers())
      .flatExtracting(AgencyLocationLscMappingDTO::getAgencyCodeMappings)
      .hasSize(newAcMappings.size())
      .usingElementComparatorOnFields("agencyCode", "locationId", "libraryId")
      .containsExactlyInAnyOrderElementsOf(newAcMappings);
  }

  @Test
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql"
  })
  void return400WhenCreatingNewMappingWithNullLocationId() {
    var newMapping = deserializeMapping();

    newMapping.setLocationId(null);

    var responseEntity =
      testRestTemplate.exchange(baseMappingURL(), HttpMethod.PUT, new HttpEntity<>(newMapping), ValidationErrorsDTO.class);

    assertEquals(BAD_REQUEST, responseEntity.getStatusCode());

    assertNotNull(responseEntity.getBody());
    assertThat(responseEntity.getBody().getValidationErrors(),
      contains(createValidationError("locationId", "must not be null")));
  }

  @Test
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql"
  })
  void return400WhenCreatingNewMappingWithNullLocalServerCode() {
    var newMapping = deserializeMapping();

    newMapping.getLocalServers().get(0).setLocalCode(null);

    var responseEntity = testRestTemplate.exchange(baseMappingURL(), HttpMethod.PUT, new HttpEntity<>(newMapping),
      ValidationErrorsDTO.class);

    assertEquals(BAD_REQUEST, responseEntity.getStatusCode());

    assertNotNull(responseEntity.getBody());
    assertThat(responseEntity.getBody().getValidationErrors(),
      contains(createValidationError("localServers[0].localCode", "must not be null")));
  }

  @Test
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/agency-loc-mapping/pre-populate-agency-location-mapping.sql"
  })
  void shouldUpdateExistingMapping() {
    var existing = mapper.toDTO(fetchDbEntity());

    existing.setLocationId(PRE_POPULATED_LOCATION2_ID);

    existing.getLocalServers()
      .stream()
      .peek(m -> m.setLocationId(PRE_POPULATED_LOCATION2_ID))
      .flatMap(m -> m.getAgencyCodeMappings().stream())
      .forEach(am -> am.setLocationId(PRE_POPULATED_LOCATION2_ID));

    var responseEntity =
      testRestTemplate.exchange(baseMappingURL(), HttpMethod.PUT, new HttpEntity<>(existing), Void.class);

    assertEquals(HttpStatus.NO_CONTENT, responseEntity.getStatusCode());
    assertFalse(responseEntity.hasBody());

    var updated = mapper.toDTO(fetchDbEntity());
    var updatedLsMappings = updated.getLocalServers();
    var existingLsMappings = existing.getLocalServers();

    assertEquals(existing.getLocationId(), updated.getLocationId());

    Assertions.assertThat(updatedLsMappings)
      .extracting(AgencyLocationLscMappingDTO::getLocationId)
      .hasSize(existingLsMappings.size())
      .containsOnly(PRE_POPULATED_LOCATION2_ID);

    Assertions.assertThat(updatedLsMappings)
      .flatExtracting(AgencyLocationLscMappingDTO::getAgencyCodeMappings)
      .extracting(AgencyLocationAcMappingDTO::getLocationId)
      .containsOnly(PRE_POPULATED_LOCATION2_ID);
  }

  @Test
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/agency-loc-mapping/pre-populate-agency-location-mapping.sql"
  })
  void shouldCreateUpdateAndDeleteMappingsAtTheSameTime() {
    var existing = mapper.toDTO(fetchDbEntity());
    var existingLsMappings = existing.getLocalServers();
    var existingLsMapping = existingLsMappings.get(0);

    // to delete
    existingLsMapping
      .getAgencyCodeMappings()
      .removeIf(agencyCodeEquals(PRE_POPULATED_AGENCY_CODE));

    // to update
    existingLsMapping.setLocationId(PRE_POPULATED_LOCATION2_ID);

    findInCollection(existingLsMapping.getAgencyCodeMappings(), agencyCodeEquals(PRE_POPULATED_AGENCY2_CODE))
      .ifPresent(am -> am.setLocationId(PRE_POPULATED_LOCATION2_ID));

    // to insert
    var newMapping = deserializeMapping();
    existingLsMappings.addAll(newMapping.getLocalServers());

    var responseEntity =
      testRestTemplate.exchange(baseMappingURL(), HttpMethod.PUT, new HttpEntity<>(existing), Void.class);

    assertEquals(HttpStatus.NO_CONTENT, responseEntity.getStatusCode());
    assertFalse(responseEntity.hasBody());

    var updated = mapper.toDTO(fetchDbEntity());
    var updatedLsMappings = updated.getLocalServers();

    // verify changes
    Assertions.assertThat(updatedLsMappings)
      .hasSize(existingLsMappings.size())
      .usingElementComparatorOnFields("localCode", "locationId", "libraryId")
      .containsExactlyInAnyOrderElementsOf(existingLsMappings);

    var existingAcMappings = getAllAcMappings(existingLsMappings);

    Assertions.assertThat(updatedLsMappings)
      .flatExtracting(AgencyLocationLscMappingDTO::getAgencyCodeMappings)
      .hasSize(existingAcMappings.size())
      .usingElementComparatorOnFields("agencyCode", "locationId", "libraryId")
      .containsExactlyInAnyOrderElementsOf(existingAcMappings);
  }

  private static String baseMappingURL() {
    return baseMappingURL(PRE_POPULATED_CENTRAL_SERVER_ID.toString());
  }

  private static String baseMappingURL(String serverId) {
    return "/inn-reach/central-servers/" + serverId + "/agency-mappings";
  }

  private AgencyLocationMappingDTO deserializeMapping() {
    return deserializeFromJsonFile(
      "/agency-mapping/create-agency-mappings-request.json", AgencyLocationMappingDTO.class);
  }

  private AgencyLocationMapping fetchDbEntity() {
    return repository.fetchOneByCsId(PRE_POPULATED_CENTRAL_SERVER_ID).orElse(null);
  }

  private Predicate<AgencyLocationAcMappingDTO> agencyCodeEquals(String agencyCode) {
    return m -> agencyCode.equals(m.getAgencyCode());
  }

  private List<AgencyLocationAcMappingDTO> getAllAcMappings(List<AgencyLocationLscMappingDTO> lsMappings) {
    return lsMappings
      .stream()
      .flatMap(m -> m.getAgencyCodeMappings().stream())
      .collect(Collectors.toList());
  }

  private static <T> Optional<T> findInCollection(Collection<T> mappings, Predicate<T> filter) {
    return mappings.stream().filter(filter).findFirst();
  }

}
