package org.folio.innreach.controller;

import static java.util.Collections.singletonList;
import static java.util.UUID.fromString;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.AFTER_TEST_METHOD;
import static org.springframework.test.context.jdbc.SqlMergeMode.MergeMode.MERGE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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

import org.folio.innreach.domain.listener.KafkaCirculationEventListener;
import org.folio.innreach.domain.listener.KafkaInitialContributionEventListener;
import org.folio.innreach.domain.listener.KafkaInventoryEventListener;
import org.folio.innreach.external.client.InnReachAuthClient;
import org.folio.innreach.external.dto.AccessTokenDTO;
import org.folio.innreach.it.base.BaseTenantIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlMergeMode;

import org.folio.innreach.domain.entity.MaterialTypeMapping;
import org.folio.innreach.dto.Error;
import org.folio.innreach.dto.MaterialTypeMappingDTO;
import org.folio.innreach.dto.MaterialTypeMappingsDTO;
import org.folio.innreach.dto.ValidationErrorsDTO;
import org.folio.innreach.mapper.MaterialTypeMappingMapper;
import org.folio.innreach.repository.MaterialTypeMappingRepository;

@Sql(
  scripts = {
    "classpath:db/mtype-mapping/clear-material-type-mapping-table.sql",
    "classpath:db/central-server/clear-central-server-tables.sql"},
  executionPhase = AFTER_TEST_METHOD
)
@SqlMergeMode(MERGE)
class MaterialTypeMappingControllerTest extends BaseTenantIntegrationTest {

  private static final String PRE_POPULATED_CENTRAL_SERVER_ID = "edab6baf-c696-42b1-89bb-1bbb8759b0d2";
  private static final String PRE_POPULATED_MAPPING1_ID = "71bd0beb-28cb-40bb-9f40-87463d61a553";
  private static final String PRE_POPULATED_MAPPING2_ID = "d9985d0d-b121-4ccd-ac16-5ebd0ccccf7f";
  private static final String PRE_POPULATED_MAPPING3_ID = "6f783255-e0ee-42c2-aa84-669d8c70f107";
  private static final String PRE_POPULATED_MATERIAL_TYPE2_ID = "5ee11d91-f7e8-481d-b079-65d708582ccc";

  @MockitoBean
  private KafkaCirculationEventListener kafkaCirculationEventListener;
  @MockitoBean
  private KafkaInventoryEventListener kafkaInventoryEventListener;
  @MockitoBean
  private KafkaInitialContributionEventListener kafkaInitialContributionEventListener;
  @MockitoBean
  private InnReachAuthClient innReachAuthClient;

  @BeforeEach
  void init() {
    when(innReachAuthClient.getAccessToken(any(), any())).thenReturn(ResponseEntity.ok(new AccessTokenDTO()));
  }

  @Autowired
  private MaterialTypeMappingRepository repository;
  @Autowired
  private MaterialTypeMappingMapper mapper;


  @Test
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/mtype-mapping/pre-populate-material-type-mapping.sql"
  })
  void shouldGetAllExistingMappings() throws Exception {
    var result = mockMvc.perform(get(baseMappingURL())
        .headers(defaultHeaders()))
      .andExpect(status().is2xxSuccessful())
      .andReturn();

    var response = fromJson(result, MaterialTypeMappingsDTO.class);
    assertNotNull(response);

    var mappings = response.getMaterialTypeMappings();

    List<MaterialTypeMapping> dbMappings = repository.findAll();

    assertEquals(dbMappings.size(), response.getTotalRecords());
    assertThat(mappings, containsInAnyOrder(entitiesToDTOs(dbMappings)));
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

    var response = fromJson(result, MaterialTypeMappingsDTO.class);
    assertNotNull(response);

    var mappings = response.getMaterialTypeMappings();

    assertEquals(0, response.getTotalRecords());
    assertThat(mappings, is(empty()));
  }

  @Test
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/mtype-mapping/pre-populate-material-type-mapping.sql"
  })
  void shouldApplyLimitAndOffsetWhenGettingAllExistingMappings() throws Exception {
    var result = mockMvc.perform(get(baseMappingURL())
        .param("offset", "1")
        .param("limit", "1")
        .headers(defaultHeaders()))
      .andExpect(status().is2xxSuccessful())
      .andReturn();

    var response = fromJson(result, MaterialTypeMappingsDTO.class);
    assertNotNull(response);

    var expectedMapping = findMapping(PRE_POPULATED_MAPPING3_ID);

    assertEquals(4, response.getTotalRecords());
    assertEquals(singletonList(expectedMapping), response.getMaterialTypeMappings());
  }

  @Test
  void return400WhenGetAllExistingMappingsIfLimitAndOffsetInvalid() throws Exception {
    var result = mockMvc.perform(get(baseMappingURL())
        .param("offset", "-1")
        .param("limit", "-1")
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
    "classpath:db/mtype-mapping/pre-populate-material-type-mapping.sql"
  })
  void shouldGetSingleMappingById() throws Exception {
    var result = mockMvc.perform(get(baseMappingURL() + "/" + PRE_POPULATED_MAPPING2_ID)
        .headers(defaultHeaders()))
      .andExpect(status().is2xxSuccessful())
      .andReturn();

    var mapping = fromJson(result, MaterialTypeMappingDTO.class);
    assertNotNull(mapping);

    var expected = findMapping(PRE_POPULATED_MAPPING2_ID);

    assertEquals(expected, mapping);
  }

  @Test
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql"
  })
  void return404WhenMappingIsNotFoundById() throws Exception {
    mockMvc.perform(get(baseMappingURL() + "/" + UUID.randomUUID())
        .headers(defaultHeaders()))
      .andExpect(status().isNotFound());
  }

  @Test
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql"
  })
  void shouldCreateNewMapping() throws Exception {
    var newMapping = deserializeFromJsonFile("/material-type-mapping/create-material-type-mapping-request.json",
      MaterialTypeMappingDTO.class);

    var result = mockMvc.perform(post(baseMappingURL())
        .content(asJsonString(newMapping))
        .headers(defaultHeaders())
        .contentType(APPLICATION_JSON))
      .andExpect(status().is2xxSuccessful())
      .andReturn();

    var created = fromJson(result, MaterialTypeMappingDTO.class);

    assertThat(created, samePropertyValuesAs(newMapping, "id", "metadata"));
  }

  @Test
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql"
  })
  void return400WhenCreatingNewMappingAndCentralItemTypeIsNull() throws Exception {
    var newMapping = deserializeFromJsonFile("/material-type-mapping/create-material-type-mapping-request.json",
      MaterialTypeMappingDTO.class);
    newMapping.setCentralItemType(null);

    var result = mockMvc.perform(post(baseMappingURL())
        .content(asJsonString(newMapping))
        .headers(defaultHeaders())
        .contentType(APPLICATION_JSON))
      .andExpect(status().isBadRequest())
      .andReturn();

    var body = fromJson(result, ValidationErrorsDTO.class);
    assertNotNull(body);
    assertThat(body.getValidationErrors(),
      contains(createValidationError("centralItemType", "must not be null")));
  }

  @Test
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql"
  })
  void return400WhenCreatingNewMappingAndMaterialTypeIdIsNull() throws Exception {
    var newMapping = deserializeFromJsonFile("/material-type-mapping/create-material-type-mapping-request.json",
      MaterialTypeMappingDTO.class);
    newMapping.setMaterialTypeId(null);

    var result = mockMvc.perform(post(baseMappingURL())
        .content(asJsonString(newMapping))
        .headers(defaultHeaders())
        .contentType(APPLICATION_JSON))
      .andExpect(status().isBadRequest())
      .andReturn();

    var body = fromJson(result, ValidationErrorsDTO.class);
    assertNotNull(body);
    assertThat(body.getValidationErrors(),
      contains(createValidationError("materialTypeId", "must not be null")));
  }

  @Test
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/mtype-mapping/pre-populate-material-type-mapping.sql"
  })
  void return409WhenCreatingNewMappingAndMaterialTypeIdAlreadyMapped() throws Exception {
    var newMapping = deserializeFromJsonFile("/material-type-mapping/create-material-type-mapping-request.json",
      MaterialTypeMappingDTO.class);
    newMapping.setMaterialTypeId(fromString(PRE_POPULATED_MATERIAL_TYPE2_ID));

    mockMvc.perform(post(baseMappingURL())
        .content(asJsonString(newMapping))
        .headers(defaultHeaders())
        .contentType(APPLICATION_JSON))
      .andExpect(status().isConflict());
  }

  @Test
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/mtype-mapping/pre-populate-material-type-mapping.sql"
  })
  void shouldUpdateExistingMapping() throws Exception {
    var mapping = deserializeFromJsonFile("/material-type-mapping/update-material-type-mapping-request.json",
      MaterialTypeMappingDTO.class);

    mockMvc.perform(put(baseMappingURL() + "/{mappingId}", PRE_POPULATED_MAPPING2_ID)
        .content(asJsonString(mapping))
        .headers(defaultHeaders())
        .contentType(APPLICATION_JSON))
      .andExpect(status().is2xxSuccessful());
  }

  @Test
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql"
  })
  void return404IfMappingNotFoundWhenUpdating() throws Exception {
    var mapping = deserializeFromJsonFile("/material-type-mapping/update-material-type-mapping-request.json",
      MaterialTypeMappingDTO.class);

    mockMvc.perform(put(baseMappingURL() + "/{mappingId}", UUID.randomUUID())
        .content(asJsonString(mapping))
        .headers(defaultHeaders())
        .contentType(APPLICATION_JSON))
      .andExpect(status().isNotFound());
  }

  @Test
  @Sql(scripts = {
      "classpath:db/central-server/pre-populate-central-server.sql",
      "classpath:db/mtype-mapping/pre-populate-material-type-mapping.sql"
  })
  void shouldUpdateExistingMappings() throws Exception {
    var existing = mapper.toDTOCollection(repository.findAll());
    Integer itemType = 10;
    existing.getMaterialTypeMappings().forEach(mp -> mp.setCentralItemType(itemType));

    mockMvc.perform(put(baseMappingURL())
        .content(asJsonString(existing))
        .headers(defaultHeaders())
        .contentType(APPLICATION_JSON))
      .andExpect(status().isNoContent());

    var updated = mapper.toDTOs(repository.findAll());
    var expected = existing.getMaterialTypeMappings();

    assertEquals(expected.size(), updated.size());
    assertTrue(updated.stream()
        .filter(mp -> !mp.getCentralItemType().equals(itemType))
        .findFirst()
        .isEmpty());
  }

  @Test
  @Sql(scripts = {
      "classpath:db/central-server/pre-populate-central-server.sql",
      "classpath:db/mtype-mapping/pre-populate-material-type-mapping.sql"
  })
  void shouldCreateUpdateAndDeleteMappingsAtTheSameTime() throws Exception {
    var mappings = mapper.toDTOCollection(repository.findAll());
    List<MaterialTypeMappingDTO> em = mappings.getMaterialTypeMappings();

    em.removeIf(idEqualsTo(fromString(PRE_POPULATED_MAPPING1_ID)));         // to delete
    Integer itemType = 10;
    findInList(em, fromString(PRE_POPULATED_MAPPING2_ID))   // to update
        .ifPresent(mapping -> mapping.setCentralItemType(itemType));

    var newMappings = deserializeFromJsonFile("/material-type-mapping/create-material-type-mappings-request.json",
        MaterialTypeMappingsDTO.class);
    em.addAll(newMappings.getMaterialTypeMappings());       // to insert

    mockMvc.perform(put(baseMappingURL())
        .content(asJsonString(mappings))
        .headers(defaultHeaders())
        .contentType(APPLICATION_JSON))
      .andExpect(status().isNoContent());

    var stored = mapper.toDTOs(repository.findAll());

    assertEquals(em.size(), stored.size());
    // verify deleted
    assertTrue(findInList(stored, fromString(PRE_POPULATED_MAPPING1_ID)).isEmpty());
    // verify updated
    assertEquals(itemType,
        findInList(stored, fromString(PRE_POPULATED_MAPPING2_ID))
            .map(MaterialTypeMappingDTO::getCentralItemType).get());
    // verify inserted
    assertThat(stored, hasItems(
        samePropertyValuesAs(newMappings.getMaterialTypeMappings().get(0), "id", "metadata"),
        samePropertyValuesAs(newMappings.getMaterialTypeMappings().get(1), "id", "metadata")
    ));
  }

  @Test
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql",
    "classpath:db/mtype-mapping/pre-populate-material-type-mapping.sql"
  })
  void shouldDeleteExistingMapping() throws Exception {
    mockMvc.perform(delete(baseMappingURL() + "/{mappingId}", PRE_POPULATED_MAPPING2_ID)
        .headers(defaultHeaders()))
      .andExpect(status().isNoContent());

    var deleted = repository.findById(fromString(PRE_POPULATED_MAPPING2_ID));
    assertTrue(deleted.isEmpty());
  }

  @Test
  @Sql(scripts = {
    "classpath:db/central-server/pre-populate-central-server.sql",
  })
  void return404IfMappingNotFoundWhenDeleting() throws Exception {
    mockMvc.perform(delete(baseMappingURL() + "/{mappingId}", UUID.randomUUID())
        .headers(defaultHeaders()))
      .andExpect(status().isNotFound());
  }

  private static Predicate<MaterialTypeMappingDTO> idEqualsTo(UUID id) {
    return mapping -> Objects.equals(mapping.getId(), id);
  }

  private static String baseMappingURL() {
    return baseMappingURL(PRE_POPULATED_CENTRAL_SERVER_ID);
  }

  private static String baseMappingURL(String serverId) {
    return "/inn-reach/central-servers/" + serverId + "/material-type-mappings";
  }

  private MaterialTypeMappingDTO[] entitiesToDTOs(List<MaterialTypeMapping> dbMappings) {
    MaterialTypeMappingDTO[] result = new MaterialTypeMappingDTO[dbMappings.size()];

    int i = 0;
    for (MaterialTypeMapping dbMapping : dbMappings) {
      result[i++] = mapper.toDTO(dbMapping);
    }

    return result;
  }

  private MaterialTypeMappingDTO findMapping(String id) {
    var expectedEntity = repository.findById(fromString(id)).get();

    return mapper.toDTO(expectedEntity);
  }

  private static Optional<MaterialTypeMappingDTO> findInList(List<MaterialTypeMappingDTO> mappings, UUID id) {
    return mappings.stream().filter(idEqualsTo(id)).findFirst();
  }

}
