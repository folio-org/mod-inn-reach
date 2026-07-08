package org.folio.innreach.controller;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.samePropertyValuesAs;
import static org.junit.jupiter.api.Assertions.assertEquals;
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

import static org.folio.innreach.controller.ControllerTestUtils.createValidationError;
import static org.folio.innreach.fixture.AgencyLocationMappingFixture.deserializeMapping;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;

import org.assertj.core.api.Assertions;
import org.folio.innreach.external.client.InnReachAuthClient;
import org.folio.innreach.external.dto.AccessTokenDTO;
import org.folio.innreach.it.base.BaseTenantIT;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlMergeMode;

import org.folio.innreach.domain.entity.AgencyLocationMapping;
import org.folio.innreach.dto.AgencyLocationAcMappingDTO;
import org.folio.innreach.dto.AgencyLocationLscMappingDTO;
import org.folio.innreach.dto.AgencyLocationMappingDTO;
import org.folio.innreach.dto.ValidationErrorsDTO;
import org.folio.innreach.mapper.AgencyLocationMappingMapper;
import org.folio.innreach.repository.AgencyLocationMappingRepository;

@Sql(
  scripts = {
    "classpath:db/agency-loc-mapping/clear-agency-location-mapping.sql",
    "classpath:db/central-server/clear-central-server-tables.sql"},
  executionPhase = AFTER_TEST_METHOD
)
@SqlMergeMode(MERGE)
class AgencyMappingControllerIT extends BaseTenantIT {

  private static final UUID PRE_POPULATED_CENTRAL_SERVER_ID = UUID.fromString("edab6baf-c696-42b1-89bb-1bbb8759b0d2");
  private static final UUID PRE_POPULATED_LOCATION2_ID = UUID.fromString("2eda63ce-6b5d-45a6-8481-f83bc77c2a14");
  private static final String PRE_POPULATED_AGENCY_CODE = "5east";
  private static final String PRE_POPULATED_AGENCY2_CODE = "5main";

  @MockitoBean
  private InnReachAuthClient innReachAuthClient;

  @BeforeEach
  void init() {
    when(innReachAuthClient.getAccessToken(any(), any())).thenReturn(ResponseEntity.ok(new AccessTokenDTO()));
  }

  @Autowired
  private AgencyLocationMappingRepository repository;
  @Autowired
  private AgencyLocationMappingMapper mapper;

  @Test
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/agency-loc-mapping/pre-populate-agency-location-mapping.sql"
  })
  void shouldGetExistingMappingForCentralServer() throws Exception {
    var result = mockMvc.perform(get(baseMappingURL())
        .headers(defaultHeaders()))
      .andExpect(status().is2xxSuccessful())
      .andReturn();

    var response = fromJson(result, AgencyLocationMappingDTO.class);
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
  void shouldReturn404IfNoMappingFound() throws Exception {
    mockMvc.perform(get(baseMappingURL())
        .headers(defaultHeaders()))
      .andExpect(status().isNotFound());
  }

  @Test
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql"
  })
  void shouldCreateNewMappings() throws Exception {
    var newMapping = deserializeMapping();
    var newLsMappings = newMapping.getLocalServers();
    var newAcMappings = getAllAcMappings(newLsMappings);

    mockMvc.perform(put(baseMappingURL())
        .content(asJsonString(newMapping))
        .headers(defaultHeaders())
        .contentType(APPLICATION_JSON))
      .andExpect(status().isNoContent());

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
  void return400WhenCreatingNewMappingWithNullLocationId() throws Exception {
    var newMapping = deserializeMapping();

    newMapping.setLocationId(null);

    var result = mockMvc.perform(put(baseMappingURL())
        .content(asJsonString(newMapping))
        .headers(defaultHeaders())
        .contentType(APPLICATION_JSON))
      .andExpect(status().isBadRequest())
      .andReturn();

    var body = fromJson(result, ValidationErrorsDTO.class);
    assertNotNull(body);
    assertThat(body.getValidationErrors(),
      contains(createValidationError("locationId", "must not be null")));
  }

  @Test
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql"
  })
  void return400WhenCreatingNewMappingWithNullLocalServerCode() throws Exception {
    var newMapping = deserializeMapping();

    newMapping.getLocalServers().get(0).setLocalCode(null);

    var result = mockMvc.perform(put(baseMappingURL())
        .content(asJsonString(newMapping))
        .headers(defaultHeaders())
        .contentType(APPLICATION_JSON))
      .andExpect(status().isBadRequest())
      .andReturn();

    var body = fromJson(result, ValidationErrorsDTO.class);
    assertNotNull(body);
    assertThat(body.getValidationErrors(),
      contains(createValidationError("localServers[0].localCode", "must not be null")));
  }

  @Test
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/agency-loc-mapping/pre-populate-agency-location-mapping.sql"
  })
  void shouldUpdateExistingMapping() throws Exception {
    var existing = mapper.toDTO(fetchDbEntity());

    existing.setLocationId(PRE_POPULATED_LOCATION2_ID);

    existing.getLocalServers()
      .stream()
      .peek(m -> m.setLocationId(PRE_POPULATED_LOCATION2_ID))
      .flatMap(m -> m.getAgencyCodeMappings().stream())
      .forEach(am -> am.setLocationId(PRE_POPULATED_LOCATION2_ID));

    mockMvc.perform(put(baseMappingURL())
        .content(asJsonString(existing))
        .headers(defaultHeaders())
        .contentType(APPLICATION_JSON))
      .andExpect(status().isNoContent());

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
  void shouldDeleteLocalServerMappings() throws Exception {
    var existing = mapper.toDTO(fetchDbEntity());
    existing.getLocalServers().clear();

    mockMvc.perform(put(baseMappingURL())
        .content(asJsonString(existing))
        .headers(defaultHeaders())
        .contentType(APPLICATION_JSON))
      .andExpect(status().isNoContent());

    var updated = mapper.toDTO(fetchDbEntity());
    var updatedLsMappings = updated.getLocalServers();

    assertTrue(updatedLsMappings.isEmpty());
  }

  @Test
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/agency-loc-mapping/pre-populate-agency-location-mapping.sql"
  })
  void shouldCreateUpdateAndDeleteMappingsAtTheSameTime() throws Exception {
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

    mockMvc.perform(put(baseMappingURL())
        .content(asJsonString(existing))
        .headers(defaultHeaders())
        .contentType(APPLICATION_JSON))
      .andExpect(status().isNoContent());

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
      .toList();
  }

  private static <T> Optional<T> findInCollection(Collection<T> mappings, Predicate<T> filter) {
    return mappings.stream().filter(filter).findFirst();
  }

}
