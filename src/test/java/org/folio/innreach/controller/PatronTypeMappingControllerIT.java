package org.folio.innreach.controller;

import static java.util.UUID.fromString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.samePropertyValuesAs;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.AFTER_TEST_METHOD;
import static org.springframework.test.context.jdbc.SqlMergeMode.MergeMode.MERGE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.folio.innreach.fixture.TestUtil.deserializeFromJsonFile;
import static org.folio.innreach.fixture.TestUtil.randomInteger;
import static org.folio.innreach.util.ListUtils.mapItems;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

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
import org.folio.innreach.domain.entity.PatronTypeMapping;
import org.folio.innreach.dto.Error;
import org.folio.innreach.dto.PatronTypeMappingDTO;
import org.folio.innreach.dto.PatronTypeMappingsDTO;
import org.folio.innreach.external.client.InnReachAuthClient;
import org.folio.innreach.external.dto.AccessTokenDTO;
import org.folio.innreach.mapper.PatronTypeMappingMapper;
import org.folio.innreach.repository.PatronTypeMappingRepository;

@Sql(
  scripts = {
    "classpath:db/patron-type-mapping/clear-patron-type-mapping-tables.sql",
    "classpath:db/central-server/clear-central-server-tables.sql"},
  executionPhase = AFTER_TEST_METHOD
)
@SqlMergeMode(MERGE)
class PatronTypeMappingControllerIT extends BaseTenantIT {
  private static final String PRE_POPULATED_CENTRAL_SERVER_ID = "edab6baf-c696-42b1-89bb-1bbb8759b0d2";
  private static final String PRE_POPULATED_PATRON_TYPE_MAPPING_ID1 = "5c39c67f-1373-4ec9-b356-fb71aba3e659";
  private static final String PRE_POPULATED_PATRON_TYPE_MAPPING_ID2 = "1af0b16e-24bc-44cb-9c9a-ca07167e41d4";
  private static final String PRE_POPULATED_PATRON_GROUP_ID1 = "54e17c4c-e315-4d20-8879-efc694dea1ce";

  @MockitoBean
  private InnReachAuthClient innReachAuthClient;

  @BeforeEach
  void init() {
    when(innReachAuthClient.getAccessToken(any(), any())).thenReturn(ResponseEntity.ok(new AccessTokenDTO()));
  }

  @Autowired
  private PatronTypeMappingRepository repository;
  @Autowired
  private PatronTypeMappingMapper mapper;

  @Test
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/patron-type-mapping/pre-populate-patron-type-mapping.sql"
  })
  void shouldGetAllExistingMappings() throws Exception {
    var mvcResult = mockMvc.perform(get(
        "/inn-reach/central-servers/{centralServerId}/patron-type-mappings", PRE_POPULATED_CENTRAL_SERVER_ID)
        .headers(defaultHeaders()))
      .andExpect(status().is2xxSuccessful())
      .andReturn();

    var response = fromJson(mvcResult, PatronTypeMappingsDTO.class);
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
  void shouldUpdateAllExistingMappings() throws Exception {
    var existing = mapper.toDTOCollection(repository.findAll());
    existing.getPatronTypeMappings().forEach(m -> m.setPatronType(randomInteger(256)));

    mockMvc.perform(put(
        "/inn-reach/central-servers/{centralServerId}/patron-type-mappings", PRE_POPULATED_CENTRAL_SERVER_ID)
        .content(asJsonString(existing))
        .headers(defaultHeaders())
        .contentType(APPLICATION_JSON))
      .andExpect(status().isNoContent());

    var updated = mapper.toDTOs(repository.findAll());
    var expected = existing.getPatronTypeMappings();

    assertEquals(expected.size(), updated.size());
    assertThat(mapItems(expected, PatronTypeMappingDTO::getPatronType),
      containsInAnyOrder(updated.stream().map(PatronTypeMappingDTO::getPatronType).toArray()));
  }

  @Test
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/patron-type-mapping/pre-populate-patron-type-mapping.sql"
  })
  void shouldCreateUpdateAndDeleteMappingsAtTheSameTime() throws Exception {
    var mappings = mapper.toDTOCollection(repository.findAll());
    List<PatronTypeMappingDTO> em = mappings.getPatronTypeMappings();

    em.removeIf(idEqualsTo(fromString(PRE_POPULATED_PATRON_TYPE_MAPPING_ID1)));         // to delete
    Integer patronType = randomInteger(256);
    findInList(em, fromString(PRE_POPULATED_PATRON_TYPE_MAPPING_ID2))   // to update
      .ifPresent(mapping -> mapping.setPatronType(patronType));

    var newMappings = deserializeFromJsonFile("/patron-type-mapping/create-patron-type-mappings-request.json",
      PatronTypeMappingsDTO.class);
    em.addAll(newMappings.getPatronTypeMappings());       // to insert

    mockMvc.perform(put(
        "/inn-reach/central-servers/{centralServerId}/patron-type-mappings", PRE_POPULATED_CENTRAL_SERVER_ID)
        .content(asJsonString(mappings))
        .headers(defaultHeaders())
        .contentType(APPLICATION_JSON))
      .andExpect(status().isNoContent());

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
  void return409WhenUpdatingMappingAndPatronTypeIdAlreadyMapped() throws Exception {
    var existing = mapper.toDTOCollection(repository.findAll());
    existing.getPatronTypeMappings().forEach(m -> m.setPatronGroupId(
      UUID.fromString(PRE_POPULATED_PATRON_GROUP_ID1)));

    var mvcResult = mockMvc.perform(put(
        "/inn-reach/central-servers/{centralServerId}/patron-type-mappings", PRE_POPULATED_CENTRAL_SERVER_ID)
        .content(asJsonString(existing))
        .headers(defaultHeaders())
        .contentType(APPLICATION_JSON))
      .andExpect(status().isConflict())
      .andReturn();
    fromJson(mvcResult, Error.class);
  }

  @Test
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/patron-type-mapping/pre-populate-patron-type-mapping.sql"
  })
  void return409WhenUpdatingMappingWithInvalidPatronType() throws Exception {
    var newMapping = deserializeFromJsonFile("/patron-type-mapping/update-patron-type-mappings-invalid-request.json",
      PatronTypeMappingsDTO.class);

    var mvcResult = mockMvc.perform(put(
        "/inn-reach/central-servers/{centralServerId}/patron-type-mappings", PRE_POPULATED_CENTRAL_SERVER_ID)
        .content(asJsonString(newMapping))
        .headers(defaultHeaders())
        .contentType(APPLICATION_JSON))
      .andExpect(status().isBadRequest())
      .andReturn();
    fromJson(mvcResult, Error.class);
  }

  private static Predicate<PatronTypeMappingDTO> idEqualsTo(UUID id) {
    return mapping -> Objects.equals(mapping.getId(), id);
  }

  private static Optional<PatronTypeMappingDTO> findInList(List<PatronTypeMappingDTO> mappings, UUID id) {
    return mappings.stream().filter(idEqualsTo(id)).findFirst();
  }
}
