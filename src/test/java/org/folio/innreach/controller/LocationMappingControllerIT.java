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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.AFTER_TEST_METHOD;
import static org.springframework.test.context.jdbc.SqlMergeMode.MergeMode.MERGE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import static org.folio.innreach.controller.ControllerTestUtils.collectFieldNames;
import static org.folio.innreach.controller.ControllerTestUtils.createValidationError;
import static org.folio.innreach.fixture.TestUtil.deserializeFromJsonFile;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;

import org.folio.innreach.it.base.BaseTenantIT;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlMergeMode;

import org.folio.innreach.domain.entity.LocationMapping;
import org.folio.innreach.domain.service.CentralServerService;
import org.folio.innreach.dto.Error;
import org.folio.innreach.dto.LocationMappingDTO;
import org.folio.innreach.dto.LocationMappingsDTO;
import org.folio.innreach.dto.ValidationErrorsDTO;
import org.folio.innreach.external.client.InnReachAuthClient;
import org.folio.innreach.external.dto.AccessTokenDTO;
import org.folio.innreach.external.service.InnReachLocationExternalService;
import org.folio.innreach.mapper.LocationMappingMapper;
import org.folio.innreach.repository.LocationMappingRepository;

@Sql(
  scripts = {
    "classpath:db/loc-mapping/clear-location-mapping-table.sql",
    "classpath:db/inn-reach-location/clear-inn-reach-location-tables.sql",
    "classpath:db/central-server/clear-central-server-tables.sql"},
  executionPhase = AFTER_TEST_METHOD
)
@SqlMergeMode(MERGE)
class LocationMappingControllerIT extends BaseTenantIT {

  private static final String PRE_POPULATED_CENTRAL_SERVER_ID = "edab6baf-c696-42b1-89bb-1bbb8759b0d2";
  private static final String PRE_POPULATED_LIBRARY_ID = "a0dd1106-3de8-4346-b0f4-b1ed0a4eaffd";
  private static final UUID PRE_POPULATED_MAPPING1_ID = UUID.fromString("ada69896-3954-45dc-92cb-04182afb2548");
  private static final UUID PRE_POPULATED_MAPPING2_ID = UUID.fromString("b4262548-3e38-424c-b3d9-509af233db5f");
  private static final UUID PRE_POPULATED_INN_REACH_LOCATION1_ID = UUID.fromString(
    "34c6a230-d264-44c5-90b3-6159ed2ebdc1");
  private static final UUID PRE_POPULATED_LOCATION2_ID = UUID.fromString("c8092f39-b969-418e-83ac-d73dd5ab9564");

  @MockitoBean
  private InnReachAuthClient innReachAuthClient;

  @BeforeEach
  void init() {
    when(innReachAuthClient.getAccessToken(any(), any())).thenReturn(ResponseEntity.ok(new AccessTokenDTO()));
  }

  @Autowired
  private LocationMappingRepository repository;
  @Autowired
  private LocationMappingMapper mapper;

  @MockitoBean
  private CentralServerService centralServerService;

  @MockitoBean
  private InnReachLocationExternalService innReachLocationExternalService;


  @Test
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/inn-reach-location/pre-populate-inn-reach-location-code.sql",
    "classpath:db/inn-reach-location/pre-populate-another-inn-reach-location-code.sql",
    "classpath:db/loc-mapping/pre-populate-location-mapping.sql",
    "classpath:db/loc-mapping/pre-populate-another-location-mapping.sql"
  })
  void shouldGetAllExistingMappingsForOneLibrary() throws Exception {
    var result = mockMvc.perform(get(baseMappingURL())
        .headers(defaultHeaders()))
      .andExpect(status().is2xxSuccessful())
      .andReturn();

    var response = fromJson(result, LocationMappingsDTO.class);
    assertNotNull(response);

    var mappings = response.getLocationMappings();

    List<LocationMapping> dbMappings = repository.findByCentralServerIdAndLibraryId(
      UUID.fromString(PRE_POPULATED_CENTRAL_SERVER_ID), UUID.fromString(PRE_POPULATED_LIBRARY_ID));

    assertEquals(dbMappings.size(), response.getTotalRecords());
    assertThat(mappings, containsInAnyOrder(mapper.toDTOs(dbMappings).toArray()));
  }

  @Test
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/inn-reach-location/pre-populate-inn-reach-location-code.sql",
    "classpath:db/inn-reach-location/pre-populate-another-inn-reach-location-code.sql",
    "classpath:db/loc-mapping/pre-populate-location-mapping.sql",
    "classpath:db/loc-mapping/pre-populate-another-location-mapping.sql"
  })
  void shouldGetAllExistingMappingsForAllLibraries() throws Exception {
    var result = mockMvc.perform(get(baseMappingURLForAllLibraries(PRE_POPULATED_CENTRAL_SERVER_ID))
        .headers(defaultHeaders()))
      .andExpect(status().is2xxSuccessful())
      .andReturn();

    var response = fromJson(result, List.class);
    assertNotNull(response);

    List<LocationMapping> dbMappings = repository.findByCentralServerId(UUID.fromString(PRE_POPULATED_CENTRAL_SERVER_ID));

    assertEquals(dbMappings.size(), response.size());
  }

  @Test
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql"
  })
  void shouldGetEmptyMappingsWith0TotalIfNotSet() throws Exception {
    var result = mockMvc.perform(get(baseMappingURL())
        .headers(defaultHeaders()))
      .andExpect(status().is2xxSuccessful())
      .andReturn();

    var response = fromJson(result, LocationMappingsDTO.class);
    assertNotNull(response);

    var mappings = response.getLocationMappings();

    assertEquals(0, response.getTotalRecords());
    assertThat(mappings, is(empty()));
  }

  @Test
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/inn-reach-location/pre-populate-inn-reach-location-code.sql",
    "classpath:db/loc-mapping/pre-populate-location-mapping.sql"
  })
  void shouldApplyLimitAndOffsetWhenGettingAllExistingMappings() throws Exception {
    var result = mockMvc.perform(get(baseMappingURL() + "?offset=1&limit=1")
        .headers(defaultHeaders()))
      .andExpect(status().is2xxSuccessful())
      .andReturn();

    var response = fromJson(result, LocationMappingsDTO.class);
    assertNotNull(response);

    var expectedMapping = findMapping(PRE_POPULATED_MAPPING2_ID);

    assertEquals(3, response.getTotalRecords());
    assertEquals(singletonList(expectedMapping), response.getLocationMappings());
  }

  @Test
  void return400WhenGetAllExistingMappingsIfLimitAndOffsetInvalid() throws Exception {
    var result = mockMvc.perform(get(baseMappingURL() + "?offset=-1&limit=-1")
        .headers(defaultHeaders()))
      .andExpect(status().isBadRequest())
      .andReturn();

    var errors = fromJson(result, ValidationErrorsDTO.class);
    assertNotNull(errors);
    assertEquals(BAD_REQUEST.value(), errors.getCode());
    assertThat(collectFieldNames(errors), containsInAnyOrder(containsString("offset"), containsString("limit")));
  }

  @Test
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/inn-reach-location/pre-populate-inn-reach-location-code.sql"
  })
  void shouldCreateNewMappings() throws Exception {
    var newMappings = deserializeFromJsonFile("/location-mapping/create-location-mappings-request.json",
      LocationMappingsDTO.class);

    mockMvc.perform(put(baseMappingURL())
        .content(asJsonString(newMappings))
        .headers(defaultHeaders())
        .contentType(APPLICATION_JSON))
      .andExpect(status().isNoContent());

    var created = mapper.toDTOs(repository.findByCentralServerIdAndLibraryId(
      UUID.fromString(PRE_POPULATED_CENTRAL_SERVER_ID), UUID.fromString(PRE_POPULATED_LIBRARY_ID)));
    var expected = newMappings.getLocationMappings();

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
  void return400WhenCreatingNewMappingsAndLocationIdIsNull() throws Exception {
    var newMappings = deserializeFromJsonFile("/location-mapping/create-location-mappings-request.json",
      LocationMappingsDTO.class);
    newMappings.getLocationMappings().get(0).setLocationId(null);

    var result = mockMvc.perform(put(baseMappingURL())
        .content(asJsonString(newMappings))
        .headers(defaultHeaders())
        .contentType(APPLICATION_JSON))
      .andExpect(status().isBadRequest())
      .andReturn();

    var body = fromJson(result, ValidationErrorsDTO.class);
    assertNotNull(body);
    assertThat(body.getValidationErrors(),
      contains(createValidationError("locationMappings[0].locationId", "must not be null")));
  }

  @Test
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/inn-reach-location/pre-populate-inn-reach-location-code.sql"
  })
  void shouldCreateNewMappingsWhenInnReachLocationIdIsNull() throws Exception {
    var newMappings = deserializeFromJsonFile("/location-mapping/create-location-mappings-request.json",
      LocationMappingsDTO.class);
    newMappings.getLocationMappings().get(0).setInnReachLocationId(null);

    mockMvc.perform(put(baseMappingURL())
        .content(asJsonString(newMappings))
        .headers(defaultHeaders())
        .contentType(APPLICATION_JSON))
      .andExpect(status().isNoContent());

    var created = mapper.toDTOs(repository.findByCentralServerIdAndLibraryId(
      UUID.fromString(PRE_POPULATED_CENTRAL_SERVER_ID), UUID.fromString(PRE_POPULATED_LIBRARY_ID)));
    var expected = newMappings.getLocationMappings().stream()
      .filter(mapping -> mapping.getInnReachLocationId() != null)
      .toList();

    assertEquals(expected.size(), created.size());
    assertThat(created, containsInAnyOrder(
      samePropertyValuesAs(expected.get(0), "id", "metadata")
    ));
  }

  @Test
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/inn-reach-location/pre-populate-inn-reach-location-code.sql",
    "classpath:db/loc-mapping/pre-populate-location-mapping.sql"
  })
  void return409WhenCreatingNewMappingsAndLocationIdAlreadyMapped() throws Exception {
    var newMappings = deserializeFromJsonFile("/location-mapping/create-location-mappings-request.json",
      LocationMappingsDTO.class);
    newMappings.getLocationMappings().get(0).setLocationId(PRE_POPULATED_LOCATION2_ID);

    var existing = mapper.toDTOs(repository.findByCentralServerIdAndLibraryId(
      UUID.fromString(PRE_POPULATED_CENTRAL_SERVER_ID), UUID.fromString(PRE_POPULATED_LIBRARY_ID)));
    newMappings.getLocationMappings().addAll(existing);

    var result = mockMvc.perform(put(baseMappingURL())
        .content(asJsonString(newMappings))
        .headers(defaultHeaders())
        .contentType(APPLICATION_JSON))
      .andExpect(status().isConflict())
      .andReturn();

    var body = fromJson(result, Error.class);
    assertNotNull(body);
    assertThat(body.getMessage(), containsString("constraint [unq_location_mapping_server_loc]"));
  }

  @Test
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/inn-reach-location/pre-populate-inn-reach-location-code.sql",
    "classpath:db/inn-reach-location/pre-populate-another-inn-reach-location-code.sql",
    "classpath:db/loc-mapping/pre-populate-location-mapping.sql",
    "classpath:db/loc-mapping/pre-populate-another-location-mapping.sql"
  })
  void shouldUpdateExistingMappings() throws Exception {
    var existing = mapper.toDTOCollection(repository.findByCentralServerIdAndLibraryId(
      UUID.fromString(PRE_POPULATED_CENTRAL_SERVER_ID), UUID.fromString(PRE_POPULATED_LIBRARY_ID)));
    UUID innReachLocationId = PRE_POPULATED_INN_REACH_LOCATION1_ID;
    existing.getLocationMappings().forEach(mp -> mp.setInnReachLocationId(innReachLocationId));

    mockMvc.perform(put(baseMappingURL())
        .content(asJsonString(existing))
        .headers(defaultHeaders())
        .contentType(APPLICATION_JSON))
      .andExpect(status().isNoContent());

    var updated = mapper.toDTOs(repository.findByCentralServerIdAndLibraryId(
      UUID.fromString(PRE_POPULATED_CENTRAL_SERVER_ID), UUID.fromString(PRE_POPULATED_LIBRARY_ID)));
    var expected = existing.getLocationMappings();

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
    "classpath:db/inn-reach-location/pre-populate-another-inn-reach-location-code.sql",
    "classpath:db/loc-mapping/pre-populate-location-mapping.sql",
    "classpath:db/loc-mapping/pre-populate-another-location-mapping.sql"
  })
  void shouldCreateUpdateAndDeleteMappingsAtTheSameTime() throws Exception {
    var mappings = mapper.toDTOCollection(repository.findByCentralServerIdAndLibraryId(
      UUID.fromString(PRE_POPULATED_CENTRAL_SERVER_ID), UUID.fromString(PRE_POPULATED_LIBRARY_ID)));
    List<LocationMappingDTO> em = mappings.getLocationMappings();

    em.removeIf(idEqualsTo(PRE_POPULATED_MAPPING1_ID));         // to delete
    findInList(em, PRE_POPULATED_MAPPING2_ID)   // to update
      .ifPresent(mapping -> mapping.setInnReachLocationId(PRE_POPULATED_INN_REACH_LOCATION1_ID));

    var newMappings = deserializeFromJsonFile("/location-mapping/create-location-mappings-request.json",
      LocationMappingsDTO.class);
    em.addAll(newMappings.getLocationMappings());                // to insert

    mockMvc.perform(put(baseMappingURL())
        .content(asJsonString(mappings))
        .headers(defaultHeaders())
        .contentType(APPLICATION_JSON))
      .andExpect(status().isNoContent());

    var stored = mapper.toDTOs(repository.findByCentralServerIdAndLibraryId(
      UUID.fromString(PRE_POPULATED_CENTRAL_SERVER_ID), UUID.fromString(PRE_POPULATED_LIBRARY_ID)));

    assertEquals(em.size(), stored.size());
    // verify deleted
    assertTrue(findInList(stored, PRE_POPULATED_MAPPING1_ID).isEmpty());
    // verify updated
    assertEquals(PRE_POPULATED_INN_REACH_LOCATION1_ID,
      findInList(stored, PRE_POPULATED_MAPPING2_ID)
        .map(LocationMappingDTO::getInnReachLocationId).get());
    // verify inserted
    assertThat(stored, hasItems(
      samePropertyValuesAs(newMappings.getLocationMappings().get(0), "id", "metadata"),
      samePropertyValuesAs(newMappings.getLocationMappings().get(1), "id", "metadata")
    ));
  }

  @Test
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/inn-reach-location/pre-populate-inn-reach-location-code.sql",
    "classpath:db/loc-mapping/pre-populate-location-mapping.sql"
  })
  void shouldDeleteAllMappingsIfEmptyCollectionGiven() throws Exception {
    var mappings = new LocationMappingsDTO();

    mockMvc.perform(put(baseMappingURL())
        .content(asJsonString(mappings))
        .headers(defaultHeaders())
        .contentType(APPLICATION_JSON))
      .andExpect(status().isNoContent());

    assertEquals(0, repository.count());
  }

  private static Predicate<LocationMappingDTO> idEqualsTo(UUID id) {
    return mapping -> Objects.equals(mapping.getId(), id);
  }

  private static String baseMappingURL() {
    return baseMappingURL(PRE_POPULATED_CENTRAL_SERVER_ID, PRE_POPULATED_LIBRARY_ID);
  }

  private static String baseMappingURLForAllLibraries(String serverId) {
    return "/inn-reach/central-servers/" + serverId + "/libraries/locations/location-mappings";
  }

  private static String baseMappingURL(String serverId, String libraryId) {
    return "/inn-reach/central-servers/" + serverId + "/libraries/" + libraryId + "/locations/location-mappings";
  }

  private LocationMappingDTO findMapping(UUID id) {
    var entity = repository.findById(id).get();

    return mapper.toDTO(entity);
  }

  private static Optional<LocationMappingDTO> findInList(List<LocationMappingDTO> mappings, UUID id) {
    return mappings.stream().filter(idEqualsTo(id)).findFirst();
  }

}
